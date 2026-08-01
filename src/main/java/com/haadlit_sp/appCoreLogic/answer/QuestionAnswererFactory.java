package com.haadlit_sp.appCoreLogic.answer;

import com.haadlit_sp.appCoreLogic.llm.LlmQuestionAnswerer;
import com.haadlit_sp.appCoreLogic.llm.LlmRuntime;
import com.haadlit_sp.appCoreLogic.model.AnswerMode;
import com.haadlit_sp.appCoreLogic.model.ContactDetails;

import java.util.function.Supplier;


/**
 * Selects the answerer for the launch-chosen {@link AnswerMode}, without the walkthrough ever
 * knowing which one it got. Rule-based is the free standard; AI-enhanced layers the local model
 * behind the same rules.
 */
public final class QuestionAnswererFactory {

    private QuestionAnswererFactory() {}

    /** The standard rule-based answerer. */
    public static QuestionAnswerer create() {
        return new RuleBasedAnswerer();
    }

    /** The answerer for this mode; AI-enhanced needs the runtime and the user's contact details. */
    public static QuestionAnswerer create(AnswerMode mode, LlmRuntime runtime,
                                          Supplier<ContactDetails> contact) {
        if (mode == AnswerMode.AI_ENHANCED && runtime != null) {
            return new LlmQuestionAnswerer(new RuleBasedAnswerer(), runtime, contact);
        }
        return new RuleBasedAnswerer();
    }
}
