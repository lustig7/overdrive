package com.lustig7.overdrive.logic;

import java.util.Locale;

/** How a redstone signal gates the Overdrive. While gated off it runs at 1× (no cost, no heat). */
public enum RedstoneMode {
    IGNORE,
    ACTIVE_HIGH,
    ACTIVE_LOW;

    public boolean allows(boolean powered) {
        return switch (this) {
            case IGNORE -> true;
            case ACTIVE_HIGH -> powered;
            case ACTIVE_LOW -> !powered;
        };
    }

    public RedstoneMode next() {
        RedstoneMode[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public String translationKey() {
        return "gui.overdrive.redstone." + name().toLowerCase(Locale.ROOT);
    }

    public static RedstoneMode byId(int id) {
        RedstoneMode[] v = values();
        return id >= 0 && id < v.length ? v[id] : IGNORE;
    }
}
