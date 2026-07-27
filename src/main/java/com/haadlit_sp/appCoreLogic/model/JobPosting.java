package com.haadlit_sp.appCoreLogic.model;


/**
 * One posting surfaced by a search.
 *
 * <p>{@code id} is Indeed's own job key (the {@code jk} value) — a stable identifier that is the
 * same across runs, so it is what we dedup on. {@code easyApply} marks postings that can be applied
 * to on Indeed itself; external-redirect postings are skipped.
 */
public record JobPosting(String id, String title, String company, String location,
                         String url, boolean easyApply) {

    public JobPosting {
        title = title == null ? "" : title.strip();
        company = company == null ? "" : company.strip();
        location = location == null ? "" : location.strip();
    }
}
