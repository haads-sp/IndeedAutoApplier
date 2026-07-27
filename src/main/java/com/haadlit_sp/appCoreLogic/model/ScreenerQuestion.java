package com.haadlit_sp.appCoreLogic.model;

import java.util.List;


/**
 * One screener question from an application form.
 *
 * <p>{@code id} is the DOM field identifier (used to fill it); {@code text} is the label (used to
 * understand and to key the Q&amp;A bank); {@code options} are the choices for choice-type questions.
 */
public record ScreenerQuestion(String id, String text, QuestionType type,
                               List<String> options, boolean required) {

    public enum QuestionType { YES_NO, SINGLE_CHOICE, MULTI_CHOICE, NUMBER, TEXT, UNKNOWN }

    public ScreenerQuestion {
        text = text == null ? "" : text.strip();
        options = options == null ? List.of() : List.copyOf(options);
    }
}
