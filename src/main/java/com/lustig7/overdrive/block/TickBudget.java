package com.lustig7.overdrive.block;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Caps the extra ticks all Overdrives may add per chunk per server tick, so a wall of 64x
 * blocks cannot stall the server. Reset at the start of every server tick.
 */
public final class TickBudget {
    private static final Map<ResourceKey<Level>, Long2IntOpenHashMap> USED = new HashMap<>();

    private TickBudget() {}

    public static void reset() {
        for (Long2IntOpenHashMap map : USED.values()) map.clear();
    }

    public static void clearAll() {
        USED.clear();
    }

    /**
     * Claims up to {@code wanted} extra ticks for the chunk containing {@code target}.
     * @param max per-chunk limit; 0 or less means unlimited
     * @return how many ticks were granted
     */
    public static int claim(ResourceKey<Level> dimension, BlockPos target, int wanted, int max) {
        if (wanted <= 0) return 0;
        if (max <= 0) return wanted;
        Long2IntOpenHashMap map = USED.computeIfAbsent(dimension, k -> new Long2IntOpenHashMap());
        long key = ChunkPos.asLong(target.getX() >> 4, target.getZ() >> 4);
        int used = map.get(key);
        int granted = Math.max(0, Math.min(wanted, max - used));
        if (granted > 0) map.put(key, used + granted);
        return granted;
    }
}
