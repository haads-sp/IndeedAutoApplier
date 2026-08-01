package com.haadlit_sp.appCoreLogic.llm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Strict validation of the model's reply before anything touches a form: the answer must be usable
 * exactly as the filler needs it (an exact option label, plain digits, short clean text). Defense
 * in depth — the schema/grammar should already guarantee most of this, but nothing unvalidated
 * gets typed into a job application. Empty result = unusable, caller falls back.
 */
final class LlmResponseValidator {

    private LlmResponseValidator() {}

    /** The validated fill values, or empty when the reply is unusable or the model was unsure. */
    static Optional<List<String>> validate(ScreenerQuestion question, String rawJson) {
        JsonObject reply;
        try {
            reply = JsonParser.parseString(rawJson).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            return Optional.empty();
        }
        JsonElement unsure = reply.get("unsure");
        boolean confidentlyAnswered = unsure != null && unsure.isJsonPrimitive()
                && unsure.getAsJsonPrimitive().isBoolean() && !unsure.getAsBoolean();
        if (!confidentlyAnswered) {
            return Optional.empty();
        }
        JsonElement answer = reply.get("answer");
        if (answer == null) {
            return Optional.empty();
        }
        return switch (question.type()) {
            case YES_NO, SINGLE_CHOICE -> single(question, answer);
            case MULTI_CHOICE -> multi(question, answer);
            case NUMBER -> number(answer);
            case TEXT -> text(answer);
            case UNKNOWN -> Optional.empty();
        };
    }

    private static Optional<List<String>> single(ScreenerQuestion question, JsonElement answer) {
        if (!isString(answer)) {
            return Optional.empty();
        }
        return matchOption(ScreenerPrompt.choiceValues(question), answer.getAsString())
                .map(List::of);
    }

    private static Optional<List<String>> multi(ScreenerQuestion question, JsonElement answer) {
        if (!answer.isJsonArray() || answer.getAsJsonArray().isEmpty()
                || question.options().isEmpty()) {
            return Optional.empty();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : answer.getAsJsonArray()) {
            if (!isString(element)) {
                return Optional.empty();
            }
            Optional<String> match = matchOption(question.options(), element.getAsString());
            if (match.isEmpty()) {
                return Optional.empty();   // one off-list value poisons the whole answer
            }
            if (!values.contains(match.get())) {
                values.add(match.get());
            }
        }
        return Optional.of(values);
    }

    private static Optional<List<String>> number(JsonElement answer) {
        String value;
        if (answer.isJsonPrimitive() && answer.getAsJsonPrimitive().isNumber()) {
            value = String.valueOf(answer.getAsLong());
        } else if (isString(answer)) {
            value = answer.getAsString().strip();
        } else {
            return Optional.empty();
        }
        return value.matches("\\d{1,7}") ? Optional.of(List.of(value)) : Optional.empty();
    }

    private static Optional<List<String>> text(JsonElement answer) {
        if (!isString(answer)) {
            return Optional.empty();
        }
        String value = answer.getAsString().replaceAll("[\t\r\n]", " ").strip();
        if (value.isBlank() || value.length() > ScreenerPrompt.TEXT_ANSWER_LIMIT) {
            return Optional.empty();
        }
        return Optional.of(List.of(value));
    }

    /** The option label the filler should click — matched exactly (after strip), returned verbatim. */
    private static Optional<String> matchOption(List<String> options, String value) {
        String wanted = value == null ? "" : value.strip();
        for (String option : options) {
            if (option.strip().equals(wanted)) {
                return Optional.of(option);
            }
        }
        return Optional.empty();
    }

    private static boolean isString(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }
}
