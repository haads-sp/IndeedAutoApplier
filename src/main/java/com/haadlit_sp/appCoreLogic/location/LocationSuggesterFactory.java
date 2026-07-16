package com.haadlit_sp.appCoreLogic.location;


/** Selects where location suggestions come from. Add a case here to swap in a live geocoding API. */
public final class LocationSuggesterFactory {

    private LocationSuggesterFactory() {}

    public static LocationSuggester create() {
        return new BundledCityLocationSuggester();
    }
}
