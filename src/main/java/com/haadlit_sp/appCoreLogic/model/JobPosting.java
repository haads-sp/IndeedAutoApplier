package com.haadlit_sp.appCoreLogic.model;


/**
 * One posting surfaced by a search.
 *
 * <p>{@code id} is Indeed's own job key (the {@code jk} value) — a stable identifier that is the
 * same across runs, so it is what we dedup on. {@code easyApply} marks postings that can be applied
 * to on Indeed itself; external-redirect postings are skipped. {@code snippet} is the card's short
 * description excerpt — all we know about the job without opening it (used for the AI fit score).
 */
public record JobPosting(String id, String title, String company, String location,
                         String url, boolean easyApply, String snippet) {

    public JobPosting {
        title = title == null ? "" : title.strip();
        company = company == null ? "" : company.strip();
        location = location == null ? "" : location.strip();
        snippet = snippet == null ? "" : snippet.strip();
    }
}
