package com.haadlit_sp.appCoreLogic.apply;

import com.haadlit_sp.appCoreLogic.browser.BrowserDriver;
import com.haadlit_sp.appCoreLogic.browser.IndeedSelectors;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion.QuestionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Reads the current Indeed Apply module into {@link ScreenerQuestion}s by running the generic
 * field scraper and mapping each field's kind to a question type. Read-only.
 */
public class ApplyFormReader {

    /** Every fillable question on the current module, in DOM order. */
    public List<ScreenerQuestion> read(BrowserDriver driver) {
        Object raw = driver.evaluate(IndeedSelectors.SCRAPE_MODULE_FIELDS_JS);
        if (!(raw instanceof List<?> rows)) {
            return List.of();
        }
        List<ScreenerQuestion> questions = new ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> field)) {
                continue;
            }
            String kind = str(field.get("kind"));
            List<String> options = toList(field.get("options"));
            // Prefer the stable name attribute; smartapply's ids are React-generated and change.
            String id = firstNonBlank(str(field.get("name")), str(field.get("id")));
            questions.add(new ScreenerQuestion(id, str(field.get("label")),
                    typeOf(kind, options), options, bool(field.get("required"))));
        }
        return questions;
    }

    private static QuestionType typeOf(String kind, List<String> options) {
        return switch (kind) {
            case "number" -> QuestionType.NUMBER;
            case "select", "radio" -> isYesNo(options) ? QuestionType.YES_NO : QuestionType.SINGLE_CHOICE;
            case "checkbox" -> QuestionType.MULTI_CHOICE;
            case "text", "tel", "email", "textarea", "url", "search" -> QuestionType.TEXT;
            default -> QuestionType.UNKNOWN;
        };
    }

    private static boolean isYesNo(List<String> options) {
        if (options.size() != 2) {
            return false;
        }
        return options.stream().allMatch(o -> {
            String v = o.toLowerCase(Locale.ROOT).strip();
            return v.equals("yes") || v.equals("no");
        });
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value);
    }

    private static List<String> toList(Object value) {
        if (!(value instanceof List<?> raw)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object o : raw) {
            if (o != null) {
                out.add(o.toString());
            }
        }
        return out;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }
}
