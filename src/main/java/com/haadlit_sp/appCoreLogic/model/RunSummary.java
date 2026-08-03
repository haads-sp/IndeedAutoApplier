package com.haadlit_sp.appCoreLogic.model;


/**
 * What one apply run actually did, in the terms the user cares about. Every posting the run
 * touched lands in exactly one of the outcome buckets, so {@code skipped + abandoned + needsInput
 * + finished} accounts for {@code attempted}.
 *
 * @param found       postings the search returned
 * @param skipped     not Easy Apply, so nothing to do
 * @param attempted   postings the run opened
 * @param abandoned   an error ended the application part-way
 * @param needsInput  stopped for the human (an unanswerable question, an uncleared check)
 * @param finished    walked all the way to the end of the form
 * @param submitted   Submit was clicked
 * @param verified    a confirmation screen was seen afterwards — proof it reached the employer
 */
public record RunSummary(int found, int skipped, int attempted, int abandoned, int needsInput,
                         int finished, int submitted, int verified) {

    public static RunSummary empty() {
        return new RunSummary(0, 0, 0, 0, 0, 0, 0, 0);
    }

    public boolean isEmpty() {
        return attempted == 0 && found == 0;
    }
}
