package com.haadlit_sp.appCoreLogic.model;


/**
 * What and where the user wants to search for.
 *
 * <p>{@code country} decides which Indeed domain the search runs on — Indeed is region-specific,
 * so a Dubai location searched on the Canadian domain finds nothing. Blank means "unknown", which
 * falls back to the default domain.
 */
public record SearchCriteria(String jobQuery, String city, String country, SearchRadius radius,
                             DatePosted datePosted) {

    public SearchCriteria {
        country = country == null ? "" : country.strip();
    }

    public static SearchCriteria blank() {
        return new SearchCriteria("", "", "", SearchRadius.KM_25, DatePosted.ANY_TIME);
    }

    public SearchCriteria withJobQuery(String query) {
        return new SearchCriteria(query, city, country, radius, datePosted);
    }

    public SearchCriteria withLocation(String city, String country, SearchRadius radius) {
        return new SearchCriteria(jobQuery, city, country, radius, datePosted);
    }

    public SearchCriteria withDatePosted(DatePosted datePosted) {
        return new SearchCriteria(jobQuery, city, country, radius, datePosted);
    }

    public boolean isReady() {
        return jobQuery != null && !jobQuery.isBlank()
                && city != null && !city.isBlank();
    }
}
