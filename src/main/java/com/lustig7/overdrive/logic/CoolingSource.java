package com.lustig7.overdrive.logic;

/** Where an Overdrive's cooling currently comes from. */
public enum CoolingSource {
    NONE(0xFF4A5059),
    PASSIVE(0xFF9AA3AD),
    WATER(0xFF3B82F6),
    CRYO(0xFFA5F3FC);

    /** ARGB colour for the GUI coolant bar. */
    public final int color;

    CoolingSource(int color) {
        this.color = color;
    }

    public boolean isFluid() {
        return this == WATER || this == CRYO;
    }

    public String translationKey() {
        return "gui.overdrive.cooling." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static CoolingSource byId(int id) {
        CoolingSource[] v = values();
        return id >= 0 && id < v.length ? v[id] : NONE;
    }
}
