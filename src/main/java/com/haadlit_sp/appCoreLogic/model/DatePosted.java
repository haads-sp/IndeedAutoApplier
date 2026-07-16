package com.haadlit_sp.appCoreLogic.model;


/**
 * How recently a posting went up — mirrors Indeed's own "Date posted" filter.
 *
 * <p>Indeed only offers 1 / 3 / 7 / 14 days (its {@code fromage} URL parameter); there is no
 * "past month" option, so offering one would promise a filter we could not actually apply.
 */
public enum DatePosted {

    ANY_TIME("Any time", null),
    LAST_24_HOURS("Last 24 hours", 1),
    LAST_3_DAYS("Last 3 days", 3),
    LAST_7_DAYS("Last 7 days", 7),
    LAST_14_DAYS("Last 14 days", 14);

    private final String label;
    private final Integer days;

    DatePosted(String label, Integer days) {
        this.label = label;
        this.days = days;
    }

    public String label() { return label; }

    /** Days back, or null for no date filter at all. */
    public Integer days() { return days; }

    @Override
    public String toString() { return label; } // shown directly in the dropdown
}
