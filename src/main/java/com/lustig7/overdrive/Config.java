package com.lustig7.overdrive;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config (per world, synced to clients). All balance values live here so packs
 * (e.g. via KubeJS or the config file) can retune Overdrive without code changes.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public enum MeltdownAction { EXPLODE, DESTROY, SHUTDOWN }

    // ---- multiplier
    public static final ModConfigSpec.IntValue MAX_MULTIPLIER = BUILDER
            .comment("Highest multiplier any Overdrive may run at (rounded down to a power of two).")
            .defineInRange("multiplier.max", 64, 1, 64);
    public static final ModConfigSpec.IntValue NO_CHIP_MAX_MULTIPLIER = BUILDER
            .comment("Highest multiplier without an Overclock Chip in the chip slot.")
            .defineInRange("multiplier.withoutChip", 2, 1, 64);

    // ---- energy
    public static final ModConfigSpec.IntValue BASE_ENERGY_COST = BUILDER
            .comment("Energy cost per tick = base * multiplier^2 (1x costs nothing).")
            .defineInRange("energy.baseCost", 100, 0, 1_000_000);
    public static final ModConfigSpec.IntValue ENERGY_BUFFER = BUILDER
            .comment("FE buffer of one Overdrive.")
            .defineInRange("energy.buffer", 10_000_000, 1_000, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue MAX_ENERGY_INPUT = BUILDER
            .comment("Maximum FE accepted per tick (per insert call).")
            .defineInRange("energy.maxInput", 1_000_000, 1, Integer.MAX_VALUE);

    // ---- heat
    public static final ModConfigSpec.DoubleValue HEAT_FACTOR = BUILDER
            .comment("Equilibrium heat % = min(100, multiplier^2 / cooling * factor).")
            .defineInRange("heat.factor", 40.0, 0.0, 10_000.0);
    public static final ModConfigSpec.DoubleValue HEAT_RISE_RATE = BUILDER
            .comment("Heat % gained per tick while below equilibrium.")
            .defineInRange("heat.riseRate", 0.25, 0.001, 100.0);
    public static final ModConfigSpec.DoubleValue HEAT_DECAY_RATE = BUILDER
            .comment("Heat % lost per tick while above equilibrium.")
            .defineInRange("heat.decayRate", 0.5, 0.001, 100.0);
    public static final ModConfigSpec.DoubleValue COOLING_NONE = BUILDER
            .comment("Cooling capacity with no cooling at all.")
            .defineInRange("cooling.none", 4.0, 0.01, 1_000_000.0);
    public static final ModConfigSpec.DoubleValue COOLING_PASSIVE = BUILDER
            .comment("Cooling capacity with 4+ adjacent blocks from the #overdrive:passive_coolants tag (scales linearly from 'none').")
            .defineInRange("cooling.passive", 16.0, 0.01, 1_000_000.0);
    public static final ModConfigSpec.DoubleValue COOLING_WATER = BUILDER
            .comment("Cooling capacity while the tank holds a #overdrive:water_coolants fluid.")
            .defineInRange("cooling.water", 128.0, 0.01, 1_000_000.0);
    public static final ModConfigSpec.DoubleValue COOLING_CRYO = BUILDER
            .comment("Cooling capacity while the tank holds a #overdrive:cryo_coolants fluid.")
            .defineInRange("cooling.cryo", 2048.0, 0.01, 1_000_000.0);
    public static final ModConfigSpec.IntValue TANK_CAPACITY = BUILDER
            .comment("Coolant tank size in mB.")
            .defineInRange("coolant.tankCapacity", 16_000, 1_000, 1_000_000);
    public static final ModConfigSpec.DoubleValue WATER_USAGE = BUILDER
            .comment("Water used per tick = multiplier^2 * this (mB). 16x = 5.1 mB/t.")
            .defineInRange("coolant.waterPerM2", 0.02, 0.0, 1_000.0);
    public static final ModConfigSpec.DoubleValue CRYO_USAGE = BUILDER
            .comment("Cryo fluid used per tick = multiplier^2 * this (mB). 64x = 2 mB/t.")
            .defineInRange("coolant.cryoPerM2", 0.0005, 0.0, 1_000.0);
    public static final ModConfigSpec.DoubleValue THROTTLE_THRESHOLD = BUILDER
            .comment("Heat % at which the effective multiplier is halved.")
            .defineInRange("heat.throttleAt", 70.0, 0.0, 100.0);
    public static final ModConfigSpec.DoubleValue THROTTLE_HYSTERESIS = BUILDER
            .comment("Throttling stays active until heat drops this many % below the throttle threshold.")
            .defineInRange("heat.throttleHysteresis", 5.0, 0.0, 100.0);
    public static final ModConfigSpec.DoubleValue DAMAGE_THRESHOLD = BUILDER
            .comment("Heat % at which the chip wears 4x faster and a warning sounds.")
            .defineInRange("heat.damageAt", 90.0, 0.0, 100.0);
    public static final ModConfigSpec.EnumValue<MeltdownAction> MELTDOWN_ACTION = BUILDER
            .comment("What happens at 100 % heat. EXPLODE / DESTROY (block drops, chip lost) / SHUTDOWN (forced 1x until cool).")
            .defineEnum("heat.meltdownAction", MeltdownAction.EXPLODE);
    public static final ModConfigSpec.DoubleValue EXPLOSION_POWER = BUILDER
            .comment("Explosion power for EXPLODE (TNT = 4).")
            .defineInRange("heat.explosionPower", 3.0, 0.0, 20.0);
    public static final ModConfigSpec.BooleanValue EXPLOSION_BREAKS_BLOCKS = BUILDER
            .comment("Whether meltdown explosions destroy surrounding blocks.")
            .define("heat.explosionBreaksBlocks", true);

    // ---- wear
    public static final ModConfigSpec.DoubleValue CHIP_WEAR_MULTIPLIER = BUILDER
            .comment("Scales chip durability loss. 0 disables wear.")
            .defineInRange("chip.wearMultiplier", 1.0, 0.0, 1000.0);

    // ---- upgrades
    public static final ModConfigSpec.DoubleValue UPGRADE_HEAT_SINK = BUILDER
            .comment("Cooling capacity factor per Heat Sink Upgrade (stacks multiplicatively).")
            .defineInRange("upgrades.heatSinkCooling", 1.5, 1.0, 100.0);
    public static final ModConfigSpec.DoubleValue UPGRADE_EFFICIENCY = BUILDER
            .comment("Energy cost factor per Efficiency Upgrade (stacks multiplicatively).")
            .defineInRange("upgrades.efficiencyCost", 0.75, 0.01, 1.0);
    public static final ModConfigSpec.DoubleValue UPGRADE_OVERCHARGE_HEAT = BUILDER
            .comment("Heat factor per Overcharge Upgrade. Each Overcharge also doubles the chip's multiplier limit.")
            .defineInRange("upgrades.overchargeHeat", 1.5, 1.0, 100.0);

    // ---- thermal paste
    public static final ModConfigSpec.DoubleValue PASTE_HEAT_REDUCTION = BUILDER
            .comment("Heat % removed when Thermal Paste is used on an Overdrive.")
            .defineInRange("thermalPaste.heatReduction", 30.0, 0.0, 100.0);
    public static final ModConfigSpec.DoubleValue PASTE_CHIP_REPAIR = BUILDER
            .comment("Fraction of a chip's max durability restored per Thermal Paste in the chip repair recipe.")
            .defineInRange("thermalPaste.chipRepair", 0.25, 0.0, 1.0);

    // ---- performance
    public static final ModConfigSpec.IntValue MAX_EXTRA_TICKS_PER_CHUNK = BUILDER
            .comment("Max extra ticks all Overdrives may add to machines in one chunk per server tick. 0 = unlimited.")
            .defineInRange("performance.maxExtraTicksPerChunk", 2048, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}

    /** Reads a config value, falling back to the default before the server config is loaded (e.g. on the title screen). */
    public static int get(ModConfigSpec.IntValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    public static double get(ModConfigSpec.DoubleValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    public static boolean get(ModConfigSpec.BooleanValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    public static <E extends Enum<E>> E get(ModConfigSpec.EnumValue<E> v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }
}
