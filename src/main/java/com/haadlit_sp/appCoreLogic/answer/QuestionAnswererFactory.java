package com.haadlit_sp.appCoreLogic.answer;


/**
 * Selects the answerer. Rule-based is the free default that needs no API key; a local-LLM or Claude
 * variant is added as one more case here (chosen from settings) without touching the walkthrough.
 */
public final class QuestionAnswererFactory {

    private QuestionAnswererFactory() {}

    public static QuestionAnswerer create() {
        return new RuleBasedAnswerer();
    }
}
