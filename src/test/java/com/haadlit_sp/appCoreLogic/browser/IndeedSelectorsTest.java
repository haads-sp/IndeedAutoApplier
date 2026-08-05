package com.haadlit_sp.appCoreLogic.browser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndeedSelectorsTest {

    @Test
    void countryPicksItsOwnIndeedDomain() {
        assertEquals("https://ae.indeed.com", IndeedSelectors.hostFor("United Arab Emirates"));
        assertEquals("https://ca.indeed.com", IndeedSelectors.hostFor("Canada"));
        assertEquals("https://pk.indeed.com", IndeedSelectors.hostFor("Pakistan"));
    }

    @Test
    void theTwoIrregularDomainsAreRight() {
        // Not us.indeed.com, and not gb.indeed.com.
        assertEquals("https://www.indeed.com", IndeedSelectors.hostFor("United States"));
        assertEquals("https://uk.indeed.com", IndeedSelectors.hostFor("United Kingdom"));
    }

    @Test
    void countryMatchingIgnoresCaseAndSurroundingSpace() {
        assertEquals("https://ae.indeed.com", IndeedSelectors.hostFor("  united arab emirates "));
        assertTrue(IndeedSelectors.knowsCountry("SPAIN"));
    }

    @Test
    void unknownOrMissingCountryFallsBackToTheDefaultDomain() {
        assertEquals(IndeedSelectors.SEARCH_HOST, IndeedSelectors.hostFor(null));
        assertEquals(IndeedSelectors.SEARCH_HOST, IndeedSelectors.hostFor(""));
        assertEquals(IndeedSelectors.SEARCH_HOST, IndeedSelectors.hostFor("Atlantis"));
        assertFalse(IndeedSelectors.knowsCountry("Atlantis"));
    }

    @Test
    void searchUrlRunsOnTheCountrysDomain() {
        String url = IndeedSelectors.searchUrl("receptionist", "Dubai, Dubai, United Arab Emirates",
                "United Arab Emirates", 25, 3);
        assertTrue(url.startsWith("https://ae.indeed.com/jobs?q=receptionist"), url);
        assertTrue(url.contains("&l=Dubai"), url);
        assertTrue(url.contains("&radius=25"), url);
        assertTrue(url.contains("&fromage=3"), url);
    }

    @Test
    void radiusConvertsToMilesWhereIndeedExpectsThem() {
        // The user asked for 50 km; on a miles domain the raw 50 would be ~80 km.
        String uk = IndeedSelectors.searchUrl("nurse", "London", "United Kingdom", 50, null);
        assertTrue(uk.contains("&radius=31"), uk);
        String us = IndeedSelectors.searchUrl("nurse", "Austin", "United States", 25, null);
        assertTrue(us.contains("&radius=16"), us);
        // Metric domains keep the number as-is.
        String ca = IndeedSelectors.searchUrl("nurse", "Toronto", "Canada", 25, null);
        assertTrue(ca.contains("&radius=25"), ca);
    }

    @Test
    void postingUrlStaysOnTheSearchesRegion() {
        assertEquals("https://ae.indeed.com/viewjob?jk=abc123",
                IndeedSelectors.jobUrl("abc123", "United Arab Emirates"));
        assertEquals(IndeedSelectors.SEARCH_HOST + "/viewjob?jk=abc123",
                IndeedSelectors.jobUrl("abc123"));
    }

    @Test
    void paginationOffsetIsAppendedOnlyAfterTheFirstPage() {
        assertFalse(IndeedSelectors.searchUrl("q", "Dubai", "United Arab Emirates", 0, null, 0)
                .contains("&start="));
        assertTrue(IndeedSelectors.searchUrl("q", "Dubai", "United Arab Emirates", 0, null, 20)
                .contains("&start=20"));
    }
}
