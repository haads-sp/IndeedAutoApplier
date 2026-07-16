package com.haadlit_sp.appCoreLogic.location;

import com.haadlit_sp.appCoreLogic.model.CityLocation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;


/**
 * Suggests cities from a list bundled in the jar — no API key, no network, no rate limit, and it
 * works offline. (Nominatim's usage policy forbids autocomplete and GeoNames' API needs a
 * registered account, so a live service would be a worse default.)
 *
 * <p>The file is ordered by population, so simply preserving its order ranks the big cities first.
 * City data comes from GeoNames (https://www.geonames.org), licensed CC BY 4.0.
 */
public class BundledCityLocationSuggester implements LocationSuggester {

    private static final Logger LOG = System.getLogger(BundledCityLocationSuggester.class.getName());
    private static final String RESOURCE = "/cities.tsv";
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private final List<CityLocation> cities = new ArrayList<>();
    /** Parallel to {@link #cities}: accent-free lowercase names, so "montreal" finds "Montréal". */
    private final List<String> searchKeys = new ArrayList<>();

    public BundledCityLocationSuggester() {
        load();
    }

    private void load() {
        try (InputStream in = BundledCityLocationSuggester.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                LOG.log(Level.WARNING, "City list {0} not on the classpath; suggestions disabled", RESOURCE);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split("\t", -1);
                    if (parts.length < 3) {
                        continue;
                    }
                    cities.add(new CityLocation(parts[0], parts[1], parts[2]));
                    searchKeys.add(normalize(parts[0]));
                }
            }
            LOG.log(Level.INFO, "Loaded {0} cities", cities.size());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not load the city list; suggestions disabled", e);
        }
    }

    @Override
    public List<CityLocation> suggest(String typed, int limit) {
        String query = normalize(typed == null ? "" : typed.trim());
        if (query.isEmpty() || limit <= 0) {
            return List.of();
        }
        // Two passes so "york" offers New York before Yorkton, while a prefix match always wins.
        List<CityLocation> starts = new ArrayList<>();
        List<CityLocation> contains = new ArrayList<>();
        for (int i = 0; i < searchKeys.size(); i++) {
            String key = searchKeys.get(i);
            if (key.startsWith(query)) {
                starts.add(cities.get(i));
                if (starts.size() >= limit) {
                    return starts;
                }
            } else if (contains.size() < limit && key.contains(query)) {
                contains.add(cities.get(i));
            }
        }
        for (CityLocation city : contains) {
            if (starts.size() >= limit) {
                break;
            }
            starts.add(city);
        }
        return starts;
    }

    /** Lowercase and strip accents so typing plain ASCII still matches "Zürich" or "São Paulo". */
    private static String normalize(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT);
    }
}
