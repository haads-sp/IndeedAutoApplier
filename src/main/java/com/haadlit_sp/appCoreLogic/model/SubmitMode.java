package com.haadlit_sp.appCoreLogic.model;


/**
 * How far the app goes on each application. The user picks this per run; it defaults to the safe
 * option because submitting is irreversible and goes to a real employer.
 */
public enum SubmitMode {

    /** Fill everything, then stop at the final Submit for the user to review and click. */
    REVIEW("Review before submit",
            "Fill the form and answer questions, then pause for you to review and click Submit."),

    /** Auto-submit only when it's Easy Apply and every question was answered confidently. */
    AUTO_EASY_KNOWN("Auto — Easy Apply, all answers known",
            "Submit on its own only when the posting is Easy Apply and nothing had to be asked; "
                    + "anything with an unknown answer pauses for you."),

    /** Auto-submit every application, no review. */
    AUTO("Fully automatic",
            "Submit every application automatically, without review.");

    private final String label;
    private final String description;

    SubmitMode(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() { return label; }

    public String description() { return description; }

    /**
     * Whether to click Submit without asking, given this posting's traits.
     *
     * @param easyApply    the posting is Easy Apply (applied on Indeed itself)
     * @param allAnswered  every screener question was answered confidently (nothing left to ask)
     */
    public boolean autoSubmits(boolean easyApply, boolean allAnswered) {
        return switch (this) {
            case REVIEW -> false;
            case AUTO_EASY_KNOWN -> easyApply && allAnswered;
            case AUTO -> true;
        };
    }

    @Override
    public String toString() {
        return label;
    }
}
