package com.lustig7.overdrive.logic;

/** Operating state shown in the GUI pill and on the block (heat_state 0-3). */
public enum HeatStatus {
    OFF(0, 0xFF9AA3AD),
    STABLE(1, 0xFF4CC9F0),
    THROTTLING(2, 0xFFF59E0B),
    DAMAGE(3, 0xFFFF5A4E),
    CRITICAL(3, 0xFFFF5A4E);

    /** Value of the blockstate's heat_state property (texture). */
    public final int visualState;
    /** ARGB colour used for the GUI pill and heat bar. */
    public final int color;

    HeatStatus(int visualState, int color) {
        this.visualState = visualState;
        this.color = color;
    }

    public String translationKey() {
        return "gui.overdrive.status." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static HeatStatus byId(int id) {
        HeatStatus[] v = values();
        return id >= 0 && id < v.length ? v[id] : OFF;
    }
}
