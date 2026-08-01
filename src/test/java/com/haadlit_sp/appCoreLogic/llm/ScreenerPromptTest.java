package com.haadlit_sp.appCoreLogic.llm;

import com.google.gson.JsonObject;
import com.haadlit_sp.appCoreLogic.model.ContactDetails;
import com.haadlit_sp.appCoreLogic.model.ProfileFacts;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenerPromptTest {

    @Test
    void choiceSchemaListsOptionsVerbatim() {
        var q = new ScreenerQuestion("q1", "AZ experience?", QuestionType.SINGLE_CHOICE,
                List.of("None", "Under 1 year", "5+ years"), true);
        JsonObject schema = ScreenerPrompt.schemaFor(q);
        var enumValues = schema.getAsJsonObject("properties").getAsJsonObject("answer")
                .getAsJsonArray("enum");
        assertEquals(3, enumValues.size());
        assertEquals("Under 1 year", enumValues.get(1).getAsString());
        assertEquals("false", schema.get("additionalProperties").getAsString());
    }

    @Test
    void yesNoWithoutOptionsFallsBackToLiteralPair() {
        var q = new ScreenerQuestion("q1", "Can you work evenings?", QuestionType.YES_NO,
                List.of(), true);
        var enumValues = ScreenerPrompt.schemaFor(q).getAsJsonObject("properties")
                .getAsJsonObject("answer").getAsJsonArray("enum");
        assertEquals(2, enumValues.size());
        assertEquals("Yes", enumValues.get(0).getAsString());
    }

    @Test
    void numberSchemaIsBoundedInteger() {
        var q = new ScreenerQuestion("q1", "Years of experience?", QuestionType.NUMBER,
                List.of(), true);
        JsonObject answer = ScreenerPrompt.schemaFor(q).getAsJsonObject("properties")
                .getAsJsonObject("answer");
        assertEquals("integer", answer.get("type").getAsString());
        assertEquals(0, answer.get("minimum").getAsInt());
    }

    @Test
    void userPromptTruncatesResumeAndListsOptions() {
        var facts = new ProfileFacts("Pat Doe", "pat@x.com", "4165550100", List.of("driving"),
                3, "Diploma", List.of(), "Citizen", "R".repeat(9_000));
        var q = new ScreenerQuestion("q1", "How much AZ experience?", QuestionType.SINGLE_CHOICE,
                List.of("None", "1-2 years"), true);
        String user = ScreenerPrompt.user(q, facts, ContactDetails.empty());
        assertTrue(user.contains("How much AZ experience?"));
        assertTrue(user.contains("- 1-2 years"));
        assertTrue(user.contains("copied EXACTLY"));
        assertFalse(user.contains("R".repeat(ScreenerPrompt.RESUME_CHAR_LIMIT + 1)));
        assertTrue(user.contains("R".repeat(ScreenerPrompt.RESUME_CHAR_LIMIT)));
    }

    @Test
    void nullFactsAndContactAreSafe() {
        var q = new ScreenerQuestion("q1", "Can you commute?", QuestionType.YES_NO,
                List.of("Yes", "No"), false);
        String user = ScreenerPrompt.user(q, null, null);
        assertTrue(user.contains("Can you commute?"));
    }

    @Test
    void systemPromptForbidsFabricationAndAllowsUnsure() {
        String system = ScreenerPrompt.system();
        assertTrue(system.contains("NEVER"));
        assertTrue(system.contains("unsure"));
        assertTrue(system.contains("zero/none/\"No\" default"));
    }
}
