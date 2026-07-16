package com.haadlit_sp.appCoreLogic.location;

import com.haadlit_sp.appCoreLogic.model.CityLocation;

import java.util.List;


/** Suggests places as the user types. Swap the implementation to change where places come from. */
public interface LocationSuggester {

    /**
     * @param typed what the user has typed so far
     * @param limit maximum suggestions to return
     * @return best matches, most relevant first; empty when nothing matches
     */
    List<CityLocation> suggest(String typed, int limit);
}
