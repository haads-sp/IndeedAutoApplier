package com.haadlit_sp.appCoreLogic.apply;


/**
 * The outcome of walking one posting's application. Only the SUBMITTED states actually sent it;
 * the pause states leave the browser on the form for the human.
 */
public record ApplyResult(Status status, String detail) {

    public enum Status {
        /**
         * Submitted AND Indeed's confirmation was seen afterwards — the only real proof the
         * application reached the employer.
         */
        SUBMITTED_VERIFIED,
        /** Submit was clicked and the flow ended, but no confirmation was positively identified. */
        SUBMITTED,
        /** Filled and sitting on the review/submit step, waiting for the user to submit. */
        REVIEW_READY,
        /** A required question could not be answered — the user needs to step in. */
        NEEDS_INPUT,
        /** A Cloudflare check appeared; the user must clear it. */
        CHALLENGED,
        /** Not an Indeed Easy-Apply posting (external redirect), so nothing to do. */
        SKIPPED,
        /** Something went wrong; the posting is left as-is. */
        FAILED
    }

    static ApplyResult of(Status status, String detail) {
        return new ApplyResult(status, detail);
    }
}
