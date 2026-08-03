package com.haadlit_sp.appCoreLogic.answer;

import com.haadlit_sp.appCoreLogic.model.Answer;
import com.haadlit_sp.appCoreLogic.model.ProfileFacts;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion.QuestionType;
import com.haadlit_sp.appCoreLogic.store.QaBankStore;

import java.util.List;
import java.util.Locale;
import java.util.Optional;


/**
 * Default, free answerer: pattern-matches common screener questions against the {@link ProfileFacts}
 * from the resume plus the learned {@link QaBankStore}. Deliberately CONSERVATIVE — it answers only
 * when a fact clearly covers the question, and otherwise returns empty so the user is asked. On a
 * real job application, "ask" is always safer than "guess".
 */
public class RuleBasedAnswerer implements QuestionAnswerer {

    /** Education label (as ProfileFacts stores it) → keywords likely to appear in an option. */
    private static final List<String[]> EDUCATION_KEYWORDS = List.of(
            new String[]{"Doctorate", "doctor", "phd", "ph.d"},
            new String[]{"Master's", "master"},
            new String[]{"Bachelor's", "bachelor"},
            new String[]{"Associate", "associate"},
            new String[]{"Diploma", "diploma"},
            new String[]{"High school", "high school", "secondary", "ged"});

    @Override
    public Optional<Answer> answer(ScreenerQuestion question, ProfileFacts facts, QaBankStore bank) {
        // A previously learned answer always wins — that is the whole point of the bank.
        Optional<Answer> learned = bank.lookup(question);
        if (learned.isPresent()) {
            return learned;
        }

        String text = question.text().toLowerCase(Locale.ROOT);

        if (asksYearsOfExperience(text) && facts.yearsOfExperience() != null) {
            Optional<Answer> years = answerYears(question, facts.yearsOfExperience());
            if (years.isPresent()) {
                return years;
            }
        }
        if (asksWorkAuthorization(text) && isAuthorized(facts)) {
            return yes(question);
        }
        if (asksSponsorship(text) && isAuthorized(facts)) {
            // Authorized to work here → I do not require sponsorship.
            return no(question);
        }
        if (asksEducation(text)) {
            Optional<String> option = matchEducationOption(question, facts);
            if (option.isPresent()) {
                return Optional.of(Answer.of(option.get(), Answer.Source.RULE));
            }
        }
        if (asksCommute(text)) {
            // They gave us a city and radius, so willingness to commute within it is implied.
            return yes(question);
        }
        if (asksHowYouHeard(text)) {
            Optional<Answer> heard = answerHowYouHeard(question);
            if (heard.isPresent()) {
                return heard;
            }
        }
        if (asksAvailability(text) && question.type() == QuestionType.TEXT) {
            // Open-ended availability: state flexibility rather than inventing a timetable the
            // profile does not contain. A yes/no or choice version is left to the AI/user.
            return Optional.of(Answer.of(
                    "Flexible — available for the hours this role requires, including a "
                            + "mix of weekdays and weekends.", Answer.Source.RULE));
        }
        if (asksAboutMoney(text) && question.type() == QuestionType.TEXT) {
            // The standard human non-answer for free-text pay questions. A NUMBER pay field still
            // pauses — inventing a figure is worse than asking.
            return Optional.of(Answer.of("Negotiable", Answer.Source.RULE));
        }
        // Not confident — let the caller ask the user and teach the bank.
        return Optional.empty();
    }

    /** Words between "years of" and "experience" that still mean the overall total. */
    private static final java.util.Set<String> GENERIC_EXPERIENCE = java.util.Set.of(
            "work", "working", "relevant", "related", "total", "overall", "professional", "paid");
    private static final java.util.regex.Pattern YEARS_OF_WHAT =
            java.util.regex.Pattern.compile("years?(?:\\s+of)?(?:\\s+(.+?))?\\s+experience");

    /**
     * Only the GENERIC total-experience question ("years of experience", "years of work
     * experience"). A question naming a specific skill or domain ("years of AZ driving
     * experience") is NOT answerable with the resume's overall total — claiming it would
     * fabricate experience — so it falls through to the AI or the user.
     */
    private static boolean asksYearsOfExperience(String text) {
        if (!text.contains("experience")) {
            return false;
        }
        java.util.regex.Matcher m = YEARS_OF_WHAT.matcher(text);
        if (!m.find()) {
            return false;
        }
        if (text.matches(".*experience\\s+(in|with|as)\\b.*")) {
            return false;   // "years of experience in <specific thing>"
        }
        String what = m.group(1);
        if (what == null || what.isBlank()) {
            return true;
        }
        for (String word : what.strip().split("\\s+")) {
            if (!GENERIC_EXPERIENCE.contains(word)) {
                return false;
            }
        }
        return true;
    }

    private static boolean asksWorkAuthorization(String text) {
        return (text.contains("authorized") || text.contains("authorised") || text.contains("eligible")
                || text.contains("legally entitled")) && text.contains("work");
    }

    private static boolean asksEducation(String text) {
        return text.contains("education") || text.contains("degree") || text.contains("qualification");
    }

    private static boolean asksCommute(String text) {
        return text.contains("commute") || text.contains("reliably commute");
    }

    private static boolean asksSponsorship(String text) {
        return text.contains("sponsorship") || (text.contains("sponsor") && text.contains("work"));
    }

    private static boolean asksHowYouHeard(String text) {
        return (text.contains("how did you hear") || text.contains("how were you referred")
                || text.contains("where did you hear") || text.contains("how you heard"))
                || (text.contains("hear about") && text.contains("position"));
    }

    /** Availability / schedule / shift-commitment questions. */
    private static boolean asksAvailability(String text) {
        return text.contains("availability") || text.contains("days and hours")
                || text.contains("hours of work") || text.contains("work schedule")
                || (text.contains("available") && (text.contains("hours") || text.contains("days")
                        || text.contains("shift")));
    }

    /**
     * We heard about it on Indeed — that is simply true, and it is the answer the user would give.
     * Prefers a matching option ("Indeed", a job-board option, then "Other"); free text gets
     * "Indeed".
     */
    private static Optional<Answer> answerHowYouHeard(ScreenerQuestion question) {
        if (question.options().isEmpty()) {
            return question.type() == QuestionType.TEXT
                    ? Optional.of(Answer.of("Indeed", Answer.Source.RULE)) : Optional.empty();
        }
        for (String wanted : List.of("indeed", "job board", "online job board", "job site",
                "online", "other")) {
            for (String option : question.options()) {
                if (option.toLowerCase(Locale.ROOT).contains(wanted)) {
                    return Optional.of(Answer.of(option, Answer.Source.RULE));
                }
            }
        }
        return Optional.empty();
    }

    /** Pay/salary/compensation questions (shared wording with the AI answerer's guard). */
    public static boolean asksAboutMoney(String text) {
        return text.contains("salary") || text.contains("wage") || text.contains("compensation")
                || text.matches(".*\\bpay\\b.*") || text.matches(".*\\brate\\b.*");
    }

    /** Map the resume's years of experience onto the question's range option (or the raw number). */
    private static Optional<Answer> answerYears(ScreenerQuestion question, int years) {
        if (question.options().isEmpty()) {
            return Optional.of(Answer.of(String.valueOf(years), Answer.Source.RULE));
        }
        for (String option : question.options()) {
            if (yearsFitsOption(option, years)) {
                return Optional.of(Answer.of(option, Answer.Source.RULE));
            }
        }
        return Optional.empty();
    }

    /** Whether {@code years} falls in an option like "None", "Under 1 year", "1-2 years", "5+ years". */
    private static boolean yearsFitsOption(String option, int years) {
        String o = option.toLowerCase(Locale.ROOT);
        if (o.contains("none") || o.equals("0")) {
            return years == 0;
        }
        List<Integer> nums = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(o);
        while (m.find()) {
            nums.add(Integer.parseInt(m.group()));
        }
        if (nums.isEmpty()) {
            return false;
        }
        boolean atLeast = o.contains("+") || o.contains("more") || o.contains("over") || o.contains("at least");
        boolean under = o.contains("under") || o.contains("less than") || o.contains("fewer") || o.contains("up to");
        if (atLeast) {
            return years >= nums.get(0);
        }
        if (under) {
            return years < nums.get(0);
        }
        if (nums.size() >= 2) {
            return years >= nums.get(0) && years <= nums.get(1);
        }
        return years == nums.get(0);
    }

    /** "No" as the question expects it: a matching option if listed, else the literal word. */
    private static Optional<Answer> no(ScreenerQuestion question) {
        for (String option : question.options()) {
            if (option.trim().equalsIgnoreCase("no")) {
                return Optional.of(Answer.of(option, Answer.Source.RULE));
            }
        }
        return Optional.of(Answer.of("No", Answer.Source.RULE));
    }

    private static boolean isAuthorized(ProfileFacts facts) {
        String auth = facts.workAuthorization();
        if (auth == null) {
            return false; // unknown → ask, don't assume
        }
        String a = auth.toLowerCase(Locale.ROOT);
        return a.contains("citizen") || a.contains("permanent resident")
                || a.contains("work permit") || a.contains("authorized");
    }

    /** Map the resume's education level to one of the question's own options. */
    private static Optional<String> matchEducationOption(ScreenerQuestion question, ProfileFacts facts) {
        if (facts.educationLevel() == null || question.options().isEmpty()) {
            return Optional.empty();
        }
        String[] keywords = keywordsFor(facts.educationLevel());
        for (String option : question.options()) {
            String lower = option.toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (lower.contains(keyword)) {
                    return Optional.of(option);
                }
            }
        }
        return Optional.empty();
    }

    private static String[] keywordsFor(String educationLevel) {
        for (String[] row : EDUCATION_KEYWORDS) {
            if (row[0].equalsIgnoreCase(educationLevel)) {
                return java.util.Arrays.copyOfRange(row, 1, row.length);
            }
        }
        return new String[]{educationLevel.toLowerCase(Locale.ROOT)};
    }

    /** "Yes" as the question expects it: a matching option if it lists them, else the literal word. */
    private static Optional<Answer> yes(ScreenerQuestion question) {
        if (question.type() == QuestionType.SINGLE_CHOICE || question.type() == QuestionType.MULTI_CHOICE) {
            for (String option : question.options()) {
                if (option.toLowerCase(Locale.ROOT).contains("yes")) {
                    return Optional.of(Answer.of(option, Answer.Source.RULE));
                }
            }
        }
        return Optional.of(Answer.of("Yes", Answer.Source.RULE));
    }
}
