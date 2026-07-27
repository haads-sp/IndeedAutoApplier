package com.haadlit_sp.appCoreLogic.model;

import java.util.List;


/**
 * An answer to a screener question: one or more values (one for text/number/single-choice, several
 * for multi-choice). An absent answer is modelled as {@code Optional.empty()} by the answerer, which
 * is the signal to pause and ask the user — never a wrong guess.
 */
public record Answer(List<String> values, Source source) {

    /** Where the answer came from — for logs and for showing the user why it was chosen. */
    public enum Source { RULE, BANK, USER }

    public Answer {
        values = values == null ? List.of() : List.copyOf(values);
    }

    public static Answer of(String value, Source source) {
        return new Answer(List.of(value), source);
    }

    public static Answer of(List<String> values, Source source) {
        return new Answer(values, source);
    }

    /** First (or only) value, or "" when empty. */
    public String primary() {
        return values.isEmpty() ? "" : values.get(0);
    }
}
