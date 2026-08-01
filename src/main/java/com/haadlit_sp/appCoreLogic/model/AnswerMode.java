package com.haadlit_sp.appCoreLogic.model;


/**
 * How screener questions get answered. Standard is the built-in rules plus the learned bank;
 * AI-enhanced adds a small language model running locally for questions the rules can't derive.
 * Chosen at launch; everything downstream of the answerer factory is mode-agnostic.
 */
public enum AnswerMode {

    /** Rule-based answering only — exactly the behavior before the AI layer existed. */
    STANDARD("Standard",
            "Answer screener questions from your resume with built-in rules. Nothing to download."),

    /** Rules first, then a local model for the questions rules can't answer. */
    AI_ENHANCED("AI-enhanced",
            "A small AI running on this PC (fully offline, no account) also answers questions the "
                    + "rules can't, using your resume. First use downloads about 1.2 GB, once. "
                    + "In auto-submit modes its answers count as answered.");

    private final String label;
    private final String description;

    AnswerMode(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() { return label; }

    public String description() { return description; }

    @Override
    public String toString() {
        return label;
    }
}
