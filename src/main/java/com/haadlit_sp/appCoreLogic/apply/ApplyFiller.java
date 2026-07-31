package com.haadlit_sp.appCoreLogic.apply;

import com.haadlit_sp.appCoreLogic.browser.BrowserDriver;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;


/**
 * Fills one screener question with a value.
 *
 * <p>Text and number fields are handled (verified on the live form). Choice-type questions
 * (radio / select / checkbox) are not filled yet — filling one wrong on a real application is worse
 * than pausing — so {@link #fill} returns false for them and the walkthrough asks the user instead.
 * Choice filling is added once a real screener module's markup has been verified.
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
                default -> false; // choice types: pause and ask until verified live
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
