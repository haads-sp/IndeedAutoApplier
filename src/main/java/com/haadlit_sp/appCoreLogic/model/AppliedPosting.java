package com.haadlit_sp.appCoreLogic.model;

import java.time.Instant;


/**
 * A persisted record of one posting we processed — the dedup key and the audit trail in one.
 *
 * <p>Deliberately holds only what dedup and the history view need: the job id, when, the outcome,
 * and the title/company to show. No resume text, no cookies, nothing sensitive ever reaches disk.
 */
public record AppliedPosting(String jobId, Instant appliedAt, Outcome outcome,
                             String title, String company) {

    public enum Outcome { SUBMITTED, REVIEW_READY, NEEDS_INPUT, SKIPPED, FAILED }

    public HistoryEntry toHistoryEntry() {
        return new HistoryEntry(title, company, appliedAt, outcome.name().toLowerCase());
    }
}
