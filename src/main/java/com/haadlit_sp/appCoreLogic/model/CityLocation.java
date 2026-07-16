package com.haadlit_sp.appCoreLogic.model;


/** One place the user can search in. Region + country are what tell two same-named cities apart. */
public record CityLocation(String name, String region, String country) {

    /** "London, Ontario, Canada" — the suffix is how you pick the right one of several Londons. */
    public String displayName() {
        StringBuilder sb = new StringBuilder(name);
        // City-states repeat themselves ("Tokyo, Tokyo, Japan"); say it once.
        if (region != null && !region.isBlank() && !region.equalsIgnoreCase(name)) {
            sb.append(", ").append(region);
        }
        if (country != null && !country.isBlank()) {
            sb.append(", ").append(country);
        }
        return sb.toString();
    }
}
