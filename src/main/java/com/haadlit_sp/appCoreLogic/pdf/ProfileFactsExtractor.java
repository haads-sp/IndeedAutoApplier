package com.haadlit_sp.appCoreLogic.pdf;

import com.haadlit_sp.appCoreLogic.model.ProfileFacts;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Turns raw resume text into a {@link ProfileFacts} fact base using plain rules — no API key
 * required. Resumes vary wildly, so every rule is best-effort and yields null/empty rather than
 * guessing wrong; unanswerable screener questions are meant to fall through to asking the user.
 */
public class ProfileFactsExtractor {

    private static final int MAX_ITEMS = 40;
    private static final int MAX_ITEM_LENGTH = 40;

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    private static final Pattern PHONE = Pattern.compile("\\+?\\d[\\d\\-().\\s]{7,}\\d");
    private static final Pattern YEARS = Pattern.compile(
            "(\\d{1,2})\\s*\\+?\\s*(?:years?|yrs?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_LINE = Pattern.compile("^[A-Z][a-zA-Z'.-]+(?: [A-Z][a-zA-Z'.-]+){1,3}$");
    private static final Pattern ITEM_SPLIT = Pattern.compile("[,;•·|/\\t]+");

    /** Section headings used both to find a section and to know where it ends. */
    private static final List<String> HEADINGS = List.of(
            "summary", "objective", "profile", "about", "experience", "work experience",
            "professional experience", "employment", "education", "skills", "technical skills",
            "core competencies", "certifications", "certificates", "licenses", "projects",
            "awards", "achievements", "interests", "references", "contact", "languages",
            "volunteer", "publications", "work authorization", "authorization", "eligibility",
            "training", "affiliations", "additional information", "hobbies");

    /** Education levels, highest first — the first match wins. */
    private static final List<String[]> EDUCATION = List.of(
            new String[]{"Doctorate", "ph.d", "phd", "doctorate", "doctoral"},
            new String[]{"Master's", "master", "m.sc", "msc", "m.s.", "mba", "m.eng"},
            new String[]{"Bachelor's", "bachelor", "b.sc", "bsc", "b.s.", "b.a.", "b.eng", "undergraduate"},
            new String[]{"Associate", "associate degree", "a.a.", "a.s."},
            new String[]{"Diploma", "diploma"},
            new String[]{"High school", "high school", "secondary school", "g.e.d"});

    /** Work-authorization hints, most specific first. */
    private static final List<String[]> WORK_AUTH = List.of(
            new String[]{"Citizen", "citizen"},
            new String[]{"Permanent resident", "permanent resident"},
            new String[]{"Work permit", "work permit", "open work permit"},
            new String[]{"Authorized to work", "authorized to work", "eligible to work", "legally entitled to work"},
            new String[]{"Visa", "visa", "sponsorship"});

    public ProfileFacts extract(String text) {
        if (text == null || text.isBlank()) {
            return ProfileFacts.empty();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return new ProfileFacts(
                findName(text),
                firstMatch(EMAIL, text),
                firstMatch(PHONE, text),
                sectionItems(text, "skills", "technical skills", "core competencies"),
                findYears(text),
                findKeyed(lower, EDUCATION),
                sectionItems(text, "certifications", "certificates", "licenses"),
                findKeyed(lower, WORK_AUTH),
                text);
    }

    private static String firstMatch(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group().trim() : null;
    }

    /** Highest number of years mentioned anywhere ("5+ years", "3 yrs"). */
    private static Integer findYears(String text) {
        Matcher m = YEARS.matcher(text);
        int best = -1;
        while (m.find()) {
            best = Math.max(best, Integer.parseInt(m.group(1)));
        }
        return best < 0 ? null : best;
    }

    /** First label whose keywords appear in the text. */
    private static String findKeyed(String lowerText, List<String[]> table) {
        for (String[] row : table) {
            for (int i = 1; i < row.length; i++) {
                if (lowerText.contains(row[i])) {
                    return row[0];
                }
            }
        }
        return null;
    }

    /** A resume usually opens with the candidate's name on its own line. */
    private static String findName(String text) {
        for (String line : text.split("\\R", 12)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || EMAIL.matcher(trimmed).find() || PHONE.matcher(trimmed).find()) {
                continue;
            }
            if (NAME_LINE.matcher(trimmed).matches() && !isHeading(trimmed)) {
                return trimmed;
            }
        }
        return null;
    }

    /** Items listed under the first matching heading, up to the next heading. */
    private static List<String> sectionItems(String text, String... headings) {
        String[] lines = text.split("\\R");
        int start = -1;
        for (int i = 0; i < lines.length && start < 0; i++) {
            if (matchesHeading(lines[i], headings)) {
                start = i;
            }
        }
        if (start < 0) {
            return List.of();
        }
        Set<String> items = new LinkedHashSet<>();
        for (int i = start + 1; i < lines.length && items.size() < MAX_ITEMS; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (isHeading(line)) {
                break;
            }
            items.addAll(splitItems(line));
        }
        return new ArrayList<>(items).subList(0, Math.min(items.size(), MAX_ITEMS));
    }

    private static List<String> splitItems(String line) {
        List<String> out = new ArrayList<>();
        for (String piece : ITEM_SPLIT.split(line)) {
            String item = piece.replaceAll("^[-*\\s]+", "").trim();
            if (item.length() >= 2 && item.length() <= MAX_ITEM_LENGTH) {
                out.add(item);
            }
        }
        return out;
    }

    private static boolean matchesHeading(String line, String... headings) {
        String norm = normalize(line);
        for (String h : headings) {
            // A heading line is the word itself, not a sentence that merely mentions it.
            if (norm.equals(h) || (norm.startsWith(h) && norm.length() <= h.length() + 3)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHeading(String line) {
        String norm = normalize(line);
        return HEADINGS.contains(norm);
    }

    private static String normalize(String line) {
        return line.toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", " ").replaceAll("\\s+", " ").trim();
    }
}
