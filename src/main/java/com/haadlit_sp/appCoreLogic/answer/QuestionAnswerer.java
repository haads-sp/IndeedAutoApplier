package com.haadlit_sp.appCoreLogic.answer;

import com.haadlit_sp.appCoreLogic.model.Answer;
import com.haadlit_sp.appCoreLogic.model.ProfileFacts;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion;
import com.haadlit_sp.appCoreLogic.store.QaBankStore;

import java.util.Optional;


/**
 * Answers a screener question. Variants (rule-based / local LLM / Claude) implement this; the
 * factory picks one. An empty result means "not confident" — the caller pauses and asks the user,
 * then teaches the answer to the bank. Better to ask than to answer a real application wrong.
 */
public interface QuestionAnswerer {

    Optional<Answer> answer(ScreenerQuestion question, ProfileFacts facts, QaBankStore bank);
}
