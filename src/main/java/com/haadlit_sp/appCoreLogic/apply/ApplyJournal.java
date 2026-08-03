package com.haadlit_sp.appCoreLogic.apply;


/**
 * Fine-grained record of what the walkthrough did inside one application: every module entered,
 * every question and the answer chosen for it, every click, wait and interstitial.
 *
 * <p>An application that stops has exactly one useful question — "what was it doing?" — and an
 * outcome line alone cannot answer it. This is what makes a run reconstructable afterwards.
 */
@FunctionalInterface
public interface ApplyJournal {

    /** Does nothing; for callers that don't want a record (probes, tests). */
    ApplyJournal NONE = (phase, detail) -> { };

    /**
     * @param phase short machine-ish label — {@code module}, {@code question}, {@code fill},
     *              {@code click}, {@code wait}, {@code interstitial}, {@code submit}, {@code outcome}
     * @param detail human-readable specifics
     */
    void step(String phase, String detail);
}
