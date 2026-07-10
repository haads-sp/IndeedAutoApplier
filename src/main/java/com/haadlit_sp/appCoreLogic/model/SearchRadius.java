package com.haadlit_sp.appCoreLogic.model;


/** Search radii mirroring Indeed's own distance options. */
public enum SearchRadius {

    EXACT("Exact location", 0),
    KM_10("Within 10 km", 10),
    KM_25("Within 25 km", 25),
    KM_50("Within 50 km", 50),
    KM_100("Within 100 km", 100);

    private final String label;
    private final int km;

    SearchRadius(String label, int km) {
        this.label = label;
        this.km = km;
    }

    public String label() { return label; }

    public int km() { return km; }

    @Override
    public String toString() { return label; } // shown directly in the radius dropdown
}
