package com.lustig7.overdrive.item;

import java.util.Locale;

/** Effects of the Overdrive's upgrade cards. Each card in the two upgrade slots applies its effect once. */
public enum UpgradeType {
    /** Cooling capacity × upgrades.heatSinkCooling. */
    HEAT_SINK(0xFF4CC9F0),
    /** Energy cost × upgrades.efficiencyCost. */
    EFFICIENCY(0xFFA78BFA),
    /** Multiplier limit × 2, heat factor × upgrades.overchargeHeat. */
    OVERCHARGE(0xFFF59E0B);

    public final int color;

    UpgradeType(int color) {
        this.color = color;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
