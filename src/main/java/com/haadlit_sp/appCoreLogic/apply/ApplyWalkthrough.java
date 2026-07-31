package com.haadlit_sp.appCoreLogic.apply;

import com.haadlit_sp.appCoreLogic.answer.QuestionAnswerer;
import com.haadlit_sp.appCoreLogic.browser.BrowserDriver;
import com.haadlit_sp.appCoreLogic.browser.IndeedSelectors;
import com.haadlit_sp.appCoreLogic.model.Answer;
import com.haadlit_sp.appCoreLogic.model.ContactDetails;
import com.haadlit_sp.appCoreLogic.model.JobPosting;
import com.haadlit_sp.appCoreLogic.model.ProfileFacts;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion;
import com.haadlit_sp.appCoreLogic.model.SubmitMode;
import com.haadlit_sp.appCoreLogic.store.QaBankStore;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


/**
 * Walks one posting through Indeed's Easy-Apply flow: open the posting, click apply, then step each
 * smartapply module — filling contact/location from the user's details and answering screeners from
 * the fact base and Q&amp;A bank. At the review step it submits or pauses per the {@link SubmitMode}.
 *
 * <p>Runs on the browser worker thread. It only ever clicks Submit in the auto modes; otherwise it
 * stops on the review step for the human. A required question it cannot answer pauses the run rather
 * than guessing.
 */
public class ApplyWalkthrough {

    private static final Logger LOG = System.getLogger(ApplyWalkthrough.class.getName());
    private static final int MAX_MODULES = 20;
    private static final int WAIT_ATTEMPTS = 30;
    private static final long WAIT_MS = 500;
    private static final long SETTLE_MS = 1200;
    private static final long POST_FILL_MS = 800;

    private final BrowserDriver driver;
    private final QuestionAnswerer answerer;
    private final QaBankStore qaBank;
    private final ApplyFormReader reader = new ApplyFormReader();
    private final ApplyFiller filler;

    public ApplyWalkthrough(BrowserDriver driver, QuestionAnswerer answerer, QaBankStore qaBank) {
        this.driver = driver;
        this.answerer = answerer;
        this.qaBank = qaBank;
        this.filler = new ApplyFiller(driver);
    }

    public ApplyResult apply(JobPosting posting, ContactDetails contact, ProfileFacts facts,
                             Path resume, SubmitMode mode) throws InterruptedException {
        driver.navigate(posting.url());
        Thread.sleep(SETTLE_MS);
        if (isChallenged()) {
            return ApplyResult.of(ApplyResult.Status.CHALLENGED, "Cloudflare check on the posting page.");
        }
        if (!driver.exists(IndeedSelectors.APPLY_BUTTON)) {
            return ApplyResult.of(ApplyResult.Status.SKIPPED, "No Indeed apply button (external posting).");
        }
        driver.click(IndeedSelectors.APPLY_BUTTON);
        if (!waitForApplyFlow()) {
            return ApplyResult.of(ApplyResult.Status.FAILED, "The application form did not open.");
        }

        boolean everythingKnown = true;
        for (int step = 0; step < MAX_MODULES; step++) {
            if (!waitForModuleReady()) {
                if (!IndeedSelectors.inApplyFlow(driver.currentUrl())) {
                    return ApplyResult.of(ApplyResult.Status.SUBMITTED, "Application completed.");
                }
                return ApplyResult.of(ApplyResult.Status.FAILED, "A step did not finish loading.");
            }
            if (isChallenged()) {
                return ApplyResult.of(ApplyResult.Status.CHALLENGED, "Cloudflare check during the application.");
            }
            if (!IndeedSelectors.inApplyFlow(driver.currentUrl())) {
                return ApplyResult.of(ApplyResult.Status.SUBMITTED, "Application completed.");
            }

            String module = IndeedSelectors.applyModule(driver.currentUrl());
            List<ScreenerQuestion> questions = reader.read(driver);
            LOG.log(Level.INFO, "Module ''{0}'': {1} question(s)", module, questions.size());

            if (IndeedSelectors.isResumeModule(module)) {
                if (!attachResume(resume)) {
                    return ApplyResult.of(ApplyResult.Status.NEEDS_INPUT,
                            "Choose a resume in the browser to continue.");
                }
                Thread.sleep(POST_FILL_MS);
            } else {
                List<ScreenerQuestion> requiredUnfilled = fillModule(questions, contact, facts);
                Thread.sleep(POST_FILL_MS); // let React register the fills before validating / advancing
                everythingKnown &= requiredUnfilled.isEmpty() && allFilled(questions, contact, facts);

                if (hasSubmit()) {
                    if (mode.autoSubmits(posting.easyApply(), everythingKnown) && requiredUnfilled.isEmpty()) {
                        driver.clickFirstVisible(IndeedSelectors.SUBMIT_BUTTON);
                        waitToLeaveFlow();
                        return ApplyResult.of(ApplyResult.Status.SUBMITTED, "Submitted.");
                    }
                    return ApplyResult.of(ApplyResult.Status.REVIEW_READY,
                            "Filled and ready — review and click Submit in the browser.");
                }

                if (!requiredUnfilled.isEmpty()) {
                    return ApplyResult.of(ApplyResult.Status.NEEDS_INPUT,
                            "Needs your answer: \"" + requiredUnfilled.get(0).text() + "\"");
                }
            }

            try {
                driver.clickFirstVisible(IndeedSelectors.CONTINUE_BUTTON);
            } catch (RuntimeException e) {
                return ApplyResult.of(ApplyResult.Status.FAILED, "No Continue button on step: " + module);
            }
            if (!waitForAdvance(module, questions)) {
                return ApplyResult.of(ApplyResult.Status.NEEDS_INPUT,
                        "The application did not advance past \"" + module + "\" — it may need you in the browser.");
            }
        }
        return ApplyResult.of(ApplyResult.Status.FAILED, "Too many steps — stopping to be safe.");
    }

    /**
     * Attach the user's resume on the resume step. Clicks the "Upload a resume" card (which opens a
     * file chooser) and supplies the PDF; falls back to a plain file input. Returns false if there is
     * no resume or nothing accepts it — in which case the user finishes this step in the browser.
     */
    private boolean attachResume(Path resume) throws InterruptedException {
        if (resume == null) {
            return false;
        }
        // Select "Upload a resume" so the Select-file button appears.
        if (driver.exists(IndeedSelectors.RESUME_UPLOAD_CARD)) {
            driver.click(IndeedSelectors.RESUME_UPLOAD_CARD);
            Thread.sleep(POST_FILL_MS);
        }
        // Click "Select file" and hand the PDF to the chooser it opens.
        if (driver.exists(IndeedSelectors.RESUME_SELECT_FILE_BUTTON)) {
            try {
                driver.uploadViaChooser(IndeedSelectors.RESUME_SELECT_FILE_BUTTON, resume);
                return true;
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Select-file chooser failed; trying the hidden input", e);
            }
        }
        // Fallback: set the hidden file input directly.
        try {
            if (driver.exists(IndeedSelectors.RESUME_FILE_INPUT)) {
                driver.uploadFile(IndeedSelectors.RESUME_FILE_INPUT, resume);
                return true;
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Resume file-input upload failed", e);
        }
        return false;
    }

    /** Fill what we can; return the required questions we could not answer. */
    private List<ScreenerQuestion> fillModule(List<ScreenerQuestion> questions,
                                              ContactDetails contact, ProfileFacts facts) {
        List<ScreenerQuestion> requiredUnfilled = new ArrayList<>();
        for (ScreenerQuestion q : questions) {
            String value = resolve(q, contact, facts);
            boolean filled = value != null && filler.fill(q, value);
            if (!filled && q.required()) {
                requiredUnfilled.add(q);
            }
        }
        return requiredUnfilled;
    }

    /** True only if every question on the module had a value we could supply (for the auto gate). */
    private boolean allFilled(List<ScreenerQuestion> questions, ContactDetails contact, ProfileFacts facts) {
        for (ScreenerQuestion q : questions) {
            String value = resolve(q, contact, facts);
            if (value == null || value.isBlank() || q.type() == ScreenerQuestion.QuestionType.SINGLE_CHOICE
                    || q.type() == ScreenerQuestion.QuestionType.MULTI_CHOICE
                    || q.type() == ScreenerQuestion.QuestionType.YES_NO) {
                return false; // choice types aren't auto-filled yet, so they count as "had to ask"
            }
        }
        return true;
    }

    /** A value for this question from the user's details, else the answerer, else null (must ask). */
    private String resolve(ScreenerQuestion q, ContactDetails contact, ProfileFacts facts) {
        String fromContact = contactValue(q, contact);
        if (fromContact != null && !fromContact.isBlank()) {
            return fromContact;
        }
        Optional<Answer> answer = answerer.answer(q, facts, qaBank);
        return answer.map(Answer::primary).filter(v -> !v.isBlank()).orElse(null);
    }

    /** Map the known contact/location fields to the user's details; null if not a contact field. */
    private static String contactValue(ScreenerQuestion q, ContactDetails c) {
        String id = q.id().toLowerCase(Locale.ROOT);
        return switch (id) {
            case "names-first-name" -> c.firstName();
            case "names-last-name" -> c.lastName();
            case "phone" -> c.phone();
            case "location-postal-code" -> c.postalCode();
            case "location-locality" -> c.cityRegion();
            case "location-address" -> c.streetAddress();
            default -> id.contains("email") ? c.email() : null;
        };
    }

    // ---- waits / checks ----

    private boolean isChallenged() {
        return Boolean.TRUE.equals(driver.evaluate(IndeedSelectors.IS_CHALLENGE_JS));
    }

    private boolean hasSubmit() {
        return Boolean.TRUE.equals(driver.evaluate(IndeedSelectors.HAS_SUBMIT_JS));
    }

    private boolean waitForApplyFlow() throws InterruptedException {
        for (int i = 0; i < WAIT_ATTEMPTS; i++) {
            if (IndeedSelectors.inApplyFlow(driver.currentUrl())) {
                return true;
            }
            Thread.sleep(WAIT_MS);
        }
        return false;
    }

    /** Wait until the module's fields/buttons have rendered — smartapply is a heavy React app. */
    private boolean waitForModuleReady() throws InterruptedException {
        for (int i = 0; i < WAIT_ATTEMPTS; i++) {
            if (!IndeedSelectors.inApplyFlow(driver.currentUrl())) {
                return true; // left the flow; the caller decides what that means
            }
            if (Boolean.TRUE.equals(driver.evaluate(IndeedSelectors.MODULE_READY_JS))) {
                Thread.sleep(SETTLE_MS); // let React finish populating field values
                return true;
            }
            Thread.sleep(WAIT_MS);
        }
        return false;
    }

    /**
     * Wait until the form has moved past {@code from}. The smartapply URL can lag the real
     * navigation, so we also treat the previous module's fields disappearing as "advanced".
     */
    private boolean waitForAdvance(String from, List<ScreenerQuestion> previous) throws InterruptedException {
        String anchor = previous.isEmpty() ? "" : previous.get(0).id();
        for (int i = 0; i < WAIT_ATTEMPTS; i++) {
            String url = driver.currentUrl();
            if (!IndeedSelectors.inApplyFlow(url) || !IndeedSelectors.applyModule(url).equals(from)) {
                return true;
            }
            if (!anchor.isBlank() && !driver.exists("[name=\"" + anchor + "\"]")) {
                return true; // the fields we just filled are gone — the module changed
            }
            Thread.sleep(WAIT_MS);
        }
        return false;
    }

    private void waitToLeaveFlow() throws InterruptedException {
        for (int i = 0; i < WAIT_ATTEMPTS; i++) {
            if (!IndeedSelectors.inApplyFlow(driver.currentUrl())) {
                return;
            }
            Thread.sleep(WAIT_MS);
        }
    }
}
