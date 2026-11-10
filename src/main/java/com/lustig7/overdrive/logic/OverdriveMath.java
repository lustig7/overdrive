package com.lustig7.overdrive.logic;

/**
 * Pure balance formulas. No Minecraft state, so GameTests (and future unit tests) can
 * check them directly.
 */
public final class OverdriveMath {
    public static final int[] STEPS = {1, 2, 4, 8, 16, 32, 64};
    public static final int MAX_STEP = 64;

    private OverdriveMath() {}

    /** FE per tick to run at multiplier m: base * m^2. 1x is "off" and free. */
    public static long energyCost(int m, int base) {
        return m <= 1 ? 0L : (long) base * m * m;
    }

    /** Steps the multiplier down (halving) until the buffer can pay for one tick. Never below 1. */
    public static int powerLimited(int m, long availableEnergy, int base) {
        while (m > 1 && energyCost(m, base) > availableEnergy) m >>= 1;
        return Math.max(1, m);
    }

    /** Equilibrium heat %: min(100, m^2 / cooling * factor). 1x produces no heat. */
    public static double equilibriumHeat(int m, double cooling, double factor) {
        if (m <= 1) return 0.0;
        return Math.min(100.0, (double) m * m / Math.max(cooling, 1e-6) * factor);
    }

    /** Moves heat toward the target by at most riseRate (up) or decayRate (down) per tick. */
    public static double stepHeat(double heat, double target, double riseRate, double decayRate) {
        if (heat < target) return Math.min(target, heat + riseRate);
        if (heat > target) return Math.max(target, heat - decayRate);
        return heat;
    }

    /** Heat status from heat alone (no hysteresis). */
    public static HeatStatus statusForHeat(double heat, int effective, double throttleAt, double damageAt) {
        if (heat >= 100.0) return HeatStatus.CRITICAL;
        if (heat >= damageAt) return HeatStatus.DAMAGE;
        if (heat >= throttleAt) return HeatStatus.THROTTLING;
        return effective > 1 ? HeatStatus.STABLE : HeatStatus.OFF;
    }

    /** Throttling halves the multiplier. */
    public static int throttle(int m) {
        return Math.max(1, m >> 1);
    }

    /** Passive cooling: linear from 'none' (0 blocks) to 'passive' (4+ blocks). */
    public static double passiveCooling(int blocks, double none, double passive) {
        int n = Math.max(0, Math.min(4, blocks));
        return none + (passive - none) * n / 4.0;
    }

    /** Fluid cooling replaces passive cooling only when it is better (a weak config can't make things worse). */
    public static double coolingCapacity(double passive, double fluid) {
        return Math.max(passive, fluid);
    }

    /** Coolant used per tick in mB: m^2 * rate. 1x uses nothing. */
    public static double coolantPerTick(int m, double ratePerM2) {
        return m <= 1 ? 0.0 : (double) m * m * ratePerM2;
    }

    /** Chip durability lost per second: log2(m) * (1 + 2*heat/100), x4 in the damage zone. */
    public static double chipWearPerSecond(int m, double heat, double damageAt, double multiplier) {
        if (m <= 1) return 0.0;
        double wear = log2(m) * (1.0 + 2.0 * heat / 100.0);
        if (heat >= damageAt) wear *= 4.0;
        return wear * multiplier;
    }

    /** Stacking upgrade effect: factor^count (count 0 = 1.0). */
    public static double upgradeFactor(double factor, int count) {
        return count <= 0 ? 1.0 : Math.pow(factor, count);
    }

    /** Energy cost with Efficiency upgrades applied; 1x stays free. */
    public static long upgradedCost(int m, int base, double efficiencyFactor) {
        long cost = energyCost(m, base);
        return cost == 0 ? 0 : Math.max(1L, Math.round(cost * efficiencyFactor));
    }

    /** Steps down until the buffer can pay the (upgraded) cost. */
    public static int powerLimited(int m, long availableEnergy, int base, double efficiencyFactor) {
        while (m > 1 && upgradedCost(m, base, efficiencyFactor) > availableEnergy) m >>= 1;
        return Math.max(1, m);
    }

    /** Overcharge doubles the chip's limit per card; the global maximum still applies. */
    public static int overchargedLimit(int chipLimit, int overcharges, int globalMax) {
        long limit = (long) chipLimit << Math.max(0, Math.min(6, overcharges));
        return floorStep((int) Math.min(limit, globalMax));
    }

    /** Comparator signal 0..15 from heat %. */
    public static int comparatorLevel(double heat) {
        return (int) Math.round(Math.max(0.0, Math.min(100.0, heat)) / 100.0 * 15.0);
    }

    /** Chip damage after applying 'pastes' Thermal Paste, each restoring fraction of max durability. */
    public static int repairedDamage(int damage, int maxDamage, int pastes, double fraction) {
        return Math.max(0, damage - (int) Math.round(maxDamage * fraction * pastes));
    }

    public static boolean isStep(int m) {
        return m >= 1 && m <= MAX_STEP && Integer.bitCount(m) == 1;
    }

    /** Rounds down to the nearest valid step (power of two, 1..64). */
    public static int floorStep(int m) {
        if (m < 1) return 1;
        return Math.min(MAX_STEP, Integer.highestOneBit(m));
    }

    public static int log2(int m) {
        return 31 - Integer.numberOfLeadingZeros(Math.max(1, m));
    }
}
