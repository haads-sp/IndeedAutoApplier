package com.haadlit_sp.appCoreLogic.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.haadlit_sp.appCoreLogic.model.ContactDetails;
import com.haadlit_sp.appCoreLogic.model.ProfileFacts;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion;

import java.util.List;


/**
 * Builds the prompts and per-question JSON schemas for the local model. The profile + resume block
 * is byte-identical on every call so llama-server's prefix cache makes every question after the
 * first fast. The system prompt is the safety contract: never fabricate, conservative defaults,
 * admit unsure.
 */
final class ScreenerPrompt {

    /** Enough resume for context without blowing up CPU prompt-processing time. */
    static final int RESUME_CHAR_LIMIT = 6_000;
    static final int TEXT_ANSWER_LIMIT = 300;

    private ScreenerPrompt() {}

    static String system() {
        return """
                You fill in job-application screener questions on behalf of a candidate, using ONLY \
                the candidate profile and resume provided. NEVER claim a credential, licence, \
                certificate, degree, work authorization, skill, employer, or availability the \
                resume does not state.

                Decide every answer with this procedure, in order:
                1. The resume or profile directly answers the question -> give that answer, \
                "unsure": false.
                2. The question asks about experience, skills, qualifications, licences, or \
                certificates the resume does NOT mention -> the answer is the zero/none/"No" \
                default, "unsure": false. Never mark these unsure; the candidate wants the honest \
                default given confidently.
                3. Anything else the profile does not state (pay expectations, availability, \
                willingness, start dates, addresses, references, personal judgment) -> "unsure": \
                true. Never guess these.

                Examples:
                - "How many years of forklift experience do you have?" — resume never mentions \
                forklifts -> {"answer": 0, "unsure": false}
                - "Do you have a Class B licence?" — resume never mentions one -> \
                {"answer": "No", "unsure": false}
                - "What are your salary expectations?" -> {"answer": "", "unsure": true}
                - "Which shifts can you work?" — resume does not state availability -> \
                {"answer": [], "unsure": true}

                Reply ONLY with JSON matching the schema.""";
    }

    static String user(ScreenerQuestion question, ProfileFacts facts, ContactDetails contact) {
        ProfileFacts f = facts == null ? ProfileFacts.empty() : facts;
        ContactDetails c = contact == null ? ContactDetails.empty() : contact;
        StringBuilder out = new StringBuilder();
        out.append("CANDIDATE PROFILE\n");
        line(out, "Name", f.fullName());
        line(out, "Location", join(c.cityRegion(), c.country()));
        line(out, "Years of experience",
                f.yearsOfExperience() == null ? "" : String.valueOf(f.yearsOfExperience()));
        line(out, "Education", f.educationLevel());
        line(out, "Work authorization", f.workAuthorization());
        line(out, "Certifications", String.join(", ", f.certifications()));
        line(out, "Skills", String.join(", ", f.skills()));

        String resume = f.rawText() == null ? "" : f.rawText();
        if (resume.length() > RESUME_CHAR_LIMIT) {
            resume = resume.substring(0, RESUME_CHAR_LIMIT);
        }
        out.append("\nRESUME TEXT (may be truncated):\n").append(resume).append('\n');

        out.append("\nQUESTION (type=").append(question.type())
                .append(", required=").append(question.required()).append("):\n")
                .append(question.text()).append('\n');
        if (!question.options().isEmpty()) {
            out.append("OPTIONS (answer must be copied EXACTLY from this list):\n");
            for (String option : question.options()) {
                out.append("- ").append(option).append('\n');
            }
        }
        out.append("\nRemember: experience/qualification the resume doesn't mention -> the "
                + "zero/none/\"No\" default with \"unsure\": false. Pay, availability, or personal "
                + "details the profile doesn't state -> \"unsure\": true.\n");
        return out.toString();
    }

    /** The response schema for this question — llama-server enforces it as a grammar. */
    static JsonObject schemaFor(ScreenerQuestion question) {
        JsonObject answer = switch (question.type()) {
            case YES_NO, SINGLE_CHOICE -> enumOf(choiceValues(question));
            case MULTI_CHOICE -> {
                JsonObject array = new JsonObject();
                array.addProperty("type", "array");
                array.add("items", enumOf(question.options()));
                array.addProperty("minItems", 1);
                yield array;
            }
            case NUMBER -> {
                JsonObject number = new JsonObject();
                number.addProperty("type", "integer");
                number.addProperty("minimum", 0);
                number.addProperty("maximum", 99);
                yield number;
            }
            case TEXT, UNKNOWN -> {
                JsonObject text = new JsonObject();
                text.addProperty("type", "string");
                text.addProperty("maxLength", TEXT_ANSWER_LIMIT);
                yield text;
            }
        };
        JsonObject unsure = new JsonObject();
        unsure.addProperty("type", "boolean");
        JsonObject properties = new JsonObject();
        properties.add("answer", answer);
        properties.add("unsure", unsure);
        JsonArray required = new JsonArray();
        required.add("answer");
        required.add("unsure");
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", required);
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    /** Yes/no questions sometimes carry no scraped options; fall back to the literal pair. */
    static List<String> choiceValues(ScreenerQuestion question) {
        return question.options().isEmpty() ? List.of("Yes", "No") : question.options();
    }

    private static JsonObject enumOf(List<String> values) {
        JsonArray items = new JsonArray();
        for (String value : values) {
            items.add(value);
        }
        JsonObject e = new JsonObject();
        e.addProperty("type", "string");
        e.add("enum", items);
        return e;
    }

    private static void line(StringBuilder out, String label, String value) {
        if (value != null && !value.isBlank()) {
            out.append(label).append(": ").append(value).append('\n');
        }
    }

    private static String join(String a, String b) {
        if (a == null || a.isBlank()) {
            return b == null ? "" : b;
        }
        if (b == null || b.isBlank()) {
            return a;
        }
        return a + ", " + b;
    }
}
