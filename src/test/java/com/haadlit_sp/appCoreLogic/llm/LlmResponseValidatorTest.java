package com.haadlit_sp.appCoreLogic.llm;

import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmResponseValidatorTest {

    private static ScreenerQuestion question(QuestionType type, String... options) {
        return new ScreenerQuestion("q1", "How many years of AZ driving experience?",
                type, List.of(options), true);
    }

    // ---- single choice ----

    @Test
    void exactOptionIsAccepted() {
        var q = question(QuestionType.SINGLE_CHOICE, "None", "1-2 years", "5+ years");
        assertEquals(Optional.of(List.of("None")),
                LlmResponseValidator.validate(q, "{\"answer\":\"None\",\"unsure\":false}"));
    }

    @Test
    void offListOptionIsRejected() {
        var q = question(QuestionType.SINGLE_CHOICE, "None", "1-2 years");
        assertTrue(LlmResponseValidator.validate(q,
                "{\"answer\":\"Zero\",\"unsure\":false}").isEmpty());
    }

    @Test
    void whitespaceIsStrippedButOptionReturnedVerbatim() {
        var q = question(QuestionType.SINGLE_CHOICE, "1-2 years ");
        assertEquals(Optional.of(List.of("1-2 years ")),
                LlmResponseValidator.validate(q, "{\"answer\":\" 1-2 years\",\"unsure\":false}"));
    }

    @Test
    void yesNoWithoutOptionsAcceptsLiterals() {
        var q = question(QuestionType.YES_NO);
        assertEquals(Optional.of(List.of("No")),
                LlmResponseValidator.validate(q, "{\"answer\":\"No\",\"unsure\":false}"));
        assertTrue(LlmResponseValidator.validate(q,
                "{\"answer\":\"Maybe\",\"unsure\":false}").isEmpty());
    }

    // ---- unsure / malformed ----

    @Test
    void unsureTrueIsRejected() {
        var q = question(QuestionType.YES_NO, "Yes", "No");
        assertTrue(LlmResponseValidator.validate(q,
                "{\"answer\":\"No\",\"unsure\":true}").isEmpty());
    }

    @Test
    void missingOrNonBooleanUnsureIsRejected() {
        var q = question(QuestionType.YES_NO, "Yes", "No");
        assertTrue(LlmResponseValidator.validate(q, "{\"answer\":\"No\"}").isEmpty());
        assertTrue(LlmResponseValidator.validate(q,
                "{\"answer\":\"No\",\"unsure\":\"false\"}").isEmpty());
    }

    @Test
    void malformedJsonIsRejected() {
        var q = question(QuestionType.YES_NO, "Yes", "No");
        assertTrue(LlmResponseValidator.validate(q, "I think the answer is No").isEmpty());
        assertTrue(LlmResponseValidator.validate(q, "[\"No\"]").isEmpty());
    }

    // ---- multi choice ----

    @Test
    void multiChoiceSubsetIsAcceptedAndDeduped() {
        var q = question(QuestionType.MULTI_CHOICE, "Mornings", "Evenings", "Weekends");
        assertEquals(Optional.of(List.of("Mornings", "Weekends")),
                LlmResponseValidator.validate(q,
                        "{\"answer\":[\"Mornings\",\"Weekends\",\"Mornings\"],\"unsure\":false}"));
    }

    @Test
    void multiChoiceWithAnyOffListValueIsRejected() {
        var q = question(QuestionType.MULTI_CHOICE, "Mornings", "Evenings");
        assertTrue(LlmResponseValidator.validate(q,
                "{\"answer\":[\"Mornings\",\"Nights\"],\"unsure\":false}").isEmpty());
    }

    @Test
    void emptyMultiChoiceIsRejected() {
        var q = question(QuestionType.MULTI_CHOICE, "Mornings", "Evenings");
        assertTrue(LlmResponseValidator.validate(q,
                "{\"answer\":[],\"unsure\":false}").isEmpty());
    }

    // ---- number ----

    @Test
    void numericAnswerIsAccepted() {
        var q = question(QuestionType.NUMBER);
        assertEquals(Optional.of(List.of("0")),
                LlmResponseValidator.validate(q, "{\"answer\":0,\"unsure\":false}"));
        assertEquals(Optional.of(List.of("12")),
                LlmResponseValidator.validate(q, "{\"answer\":\"12\",\"unsure\":false}"));
    }

    @Test
    void nonNumericAndOversizeNumbersAreRejected() {
        var q = question(QuestionType.NUMBER);
        assertTrue(LlmResponseValidator.validate(q,
                "{\"answer\":\"twelve\",\"unsure\":false}").isEmpty());
        assertTrue(LlmResponseValidator.validate(q,
                "{\"answer\":-1,\"unsure\":false}").isEmpty());
        assertTrue(LlmResponseValidator.validate(q,
                "{\"answer\":12345678,\"unsure\":false}").isEmpty());
    }

    // ---- text ----

    @Test
    void textIsCleanedOfTsvHostileCharacters() {
        var q = question(QuestionType.TEXT);
        assertEquals(Optional.of(List.of("Main St and 5th Ave")),
                LlmResponseValidator.validate(q,
                        "{\"answer\":\"Main St\\tand\\n5th Ave\",\"unsure\":false}"));
    }

    @Test
    void blankAndOversizeTextIsRejected() {
        var q = question(QuestionType.TEXT);
        assertTrue(LlmResponseValidator.validate(q,
                "{\"answer\":\"  \",\"unsure\":false}").isEmpty());
        assertTrue(LlmResponseValidator.validate(q,
                "{\"answer\":\"" + "x".repeat(301) + "\",\"unsure\":false}").isEmpty());
    }

    @Test
    void unknownTypeIsRejected() {
        var q = question(QuestionType.UNKNOWN);
        assertTrue(LlmResponseValidator.validate(q,
                "{\"answer\":\"anything\",\"unsure\":false}").isEmpty());
    }
}
