package com.haadlit_sp.appCoreLogic.apply;

import com.haadlit_sp.appCoreLogic.browser.BrowserDriver;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;


/**
 * Fills one screener question with a value.
 *
 * <p>Text and number fields fill by name; choice types (radio groups, verified on the live form)
 * fill via {@code driver.chooseOption}, which clicks the option whose label matches the value —
 * so choice values must be an option's exact visible label. Anything unfillable returns false and
 * the walkthrough asks the user instead.
 */
public class ApplyFiller {

    private static final Logger LOG = System.getLogger(ApplyFiller.class.getName());

    private final BrowserDriver driver;

    public ApplyFiller(BrowserDriver driver) {
        this.driver = driver;
    }

    /** Apply {@code value} to the question. Returns true only if it was filled. */
    public boolean fill(ScreenerQuestion question, String value) {
        if (value == null || value.isBlank() || question.id().isBlank()) {
            return false;
        }
        try {
            return switch (question.type()) {
                case TEXT, NUMBER -> {
                    driver.fill(fieldSelector(question.id()), value);
                    yield true;
                }
                case YES_NO, SINGLE_CHOICE, MULTI_CHOICE -> {
                    // The value is the option's label. A dropdown is selected directly; anything
                    // else is a radio group and the driver clicks the matching radio.
                    String select = "select" + fieldSelector(question.id());
                    if (driver.exists(select)) {
                        driver.selectOption(select, value);
                    } else {
                        driver.chooseOption(question.id(), value);
                    }
                    yield true;
                }
                default -> false;
            };
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Could not fill field " + question.id(), e);
            return false;
        }
    }

    /** Fields are keyed by their stable name attribute (smartapply ids are dynamic React). */
    private static String fieldSelector(String name) {
        return "[name=\"" + name + "\"]";
    }
}
