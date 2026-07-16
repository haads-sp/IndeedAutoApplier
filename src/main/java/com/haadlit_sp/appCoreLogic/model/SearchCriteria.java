package com.haadlit_sp.appCoreLogic.model;


/** What and where the user wants to search for. */
public record SearchCriteria(String jobQuery, String city, SearchRadius radius, DatePosted datePosted) {

    public static SearchCriteria blank() {
        return new SearchCriteria("", "", SearchRadius.KM_25, DatePosted.ANY_TIME);
    }

    public SearchCriteria withJobQuery(String query) {
        return new SearchCriteria(query, city, radius, datePosted);
    }

    public SearchCriteria withLocation(String city, SearchRadius radius) {
        return new SearchCriteria(jobQuery, city, radius, datePosted);
    }

    public SearchCriteria withDatePosted(DatePosted datePosted) {
        return new SearchCriteria(jobQuery, city, radius, datePosted);
    }

    public boolean isReady() {
        return jobQuery != null && !jobQuery.isBlank()
                && city != null && !city.isBlank();
    }
}
