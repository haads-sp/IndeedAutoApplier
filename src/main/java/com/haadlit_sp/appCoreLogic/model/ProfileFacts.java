package com.haadlit_sp.appCoreLogic.model;

import java.util.List;


/**
 * The fact base extracted from the user's PDFs, used to answer screener questions.
 *
 * <p>Every field is best-effort: any of them may be null/empty when the resume does not say.
 * {@code rawText} is kept in memory for keyword lookups by the answerers and must NEVER be
 * written to disk.
 */
public record ProfileFacts(String fullName,
                           String email,
                           String phone,
                           List<String> skills,
                           Integer yearsOfExperience,
                           String educationLevel,
                           List<String> certifications,
                           String workAuthorization,
                           String rawText) {

    public ProfileFacts {
        skills = skills == null ? List.of() : List.copyOf(skills);
        certifications = certifications == null ? List.of() : List.copyOf(certifications);
        rawText = rawText == null ? "" : rawText;
    }

    public static ProfileFacts empty() {
        return new ProfileFacts(null, null, null, List.of(), null, null, List.of(), null, "");
    }

    public boolean isEmpty() {
        return rawText.isBlank();
    }
}
