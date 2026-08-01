package com.haadlit_sp.appCoreLogic.llm;

import com.haadlit_sp.appCoreLogic.answer.QuestionAnswerer;
import com.haadlit_sp.appCoreLogic.answer.RuleBasedAnswerer;
import com.haadlit_sp.appCoreLogic.model.Answer;
import com.haadlit_sp.appCoreLogic.model.ContactDetails;
import com.haadlit_sp.appCoreLogic.model.ProfileFacts;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion;
import com.haadlit_sp.appCoreLogic.store.QaBankStore;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


/**
 * AI-enhanced answerer: bank and rules first (deterministic, free, verified), then the local model
 * for the long tail they can't derive. A validated model answer is written back to the bank, so the
 * second resolve of the same question — and every future posting that asks it — is instant and free.
 * Anything the model can't answer safely stays {@code Optional.empty()}: the existing pause-and-ask.
 *
 * <p>Lives in the llm package so the prompt/validator stay package-private; the rest of the app
 * only ever sees the {@link QuestionAnswerer} interface via the factory.
 */
public class LlmQuestionAnswerer implements QuestionAnswerer {

    private static final Duration TIMEOUT = Duration.ofSeconds(45);

    private final RuleBasedAnswerer fallback;
    private final LlmRuntime runtime;
    private final LlmClient client;
    private final Supplier<ContactDetails> contact;
    /** Questions the model already declined this session — don't pay the latency again. */
    private final Set<String> declined = ConcurrentHashMap.newKeySet();

    public LlmQuestionAnswerer(RuleBasedAnswerer fallback, LlmRuntime runtime,
                               Supplier<ContactDetails> contact) {
        this.fallback = fallback;
        this.runtime = runtime;
        this.client = new LlmClient(runtime);
        this.contact = contact;
    }

    @Override
    public Optional<Answer> answer(ScreenerQuestion question, ProfileFacts facts, QaBankStore bank) {
        // Bank + rules first: RuleBasedAnswerer already checks the bank before its rules.
        Optional<Answer> ruled = fallback.answer(question, facts, bank);
        if (ruled.isPresent()) {
            return ruled;
        }
        if (question.type() == ScreenerQuestion.QuestionType.UNKNOWN || !runtime.isReady()) {
            return Optional.empty();
        }
        String key = QaBankStore.key(question);
        if (declined.contains(key)) {
            return Optional.empty();
        }
        if (asksAboutMoney(question.text())) {
            declined.add(key);   // pay is the candidate's call, never the model's
            return Optional.empty();
        }
        Optional<List<String>> values = client
                .complete(ScreenerPrompt.system(),
                        ScreenerPrompt.user(question, facts, contact.get()),
                        ScreenerPrompt.schemaFor(question), TIMEOUT)
                .flatMap(reply -> LlmResponseValidator.validate(question, reply));
        if (values.isEmpty()) {
            declined.add(key);
            return Optional.empty();
        }
        Answer answer = Answer.of(values.get(), Answer.Source.AI);
        bank.remember(question, answer);
        return Optional.of(answer);
    }

    private static boolean asksAboutMoney(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        return t.contains("salary") || t.contains("wage") || t.contains("compensation")
                || t.matches(".*\\bpay\\b.*");
    }
}
