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
            return Optional.of(Answer.of(String.valueOf(facts.yearsOfExperience()), Answer.Source.RULE));
        }
        if (asksWorkAuthorization(text) && isAuthorized(facts)) {
            return yes(question);
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
        // Not confident — let the caller ask the user and teach the bank.
        return Optional.empty();
    }

    private static boolean asksYearsOfExperience(String text) {
        return (text.contains("years") || text.contains("year")) && text.contains("experience");
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
