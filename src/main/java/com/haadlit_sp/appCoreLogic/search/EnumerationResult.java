package com.haadlit_sp.appCoreLogic.search;

import com.haadlit_sp.appCoreLogic.model.JobPosting;

import java.util.List;


/**
 * The outcome of one search. Distinguishing a bot check from genuinely-empty results matters:
 * a challenge means "pause and ask the human", empty means "nothing matched — move on".
 */
public record EnumerationResult(Outcome outcome, List<JobPosting> postings) {

    public enum Outcome { OK, CHALLENGED, NO_RESULTS }

    public EnumerationResult {
        postings = postings == null ? List.of() : List.copyOf(postings);
    }

    static EnumerationResult ok(List<JobPosting> postings) {
        return new EnumerationResult(Outcome.OK, postings);
    }

    static EnumerationResult challenged() {
        return new EnumerationResult(Outcome.CHALLENGED, List.of());
    }

    static EnumerationResult noResults() {
        return new EnumerationResult(Outcome.NO_RESULTS, List.of());
    }
}
