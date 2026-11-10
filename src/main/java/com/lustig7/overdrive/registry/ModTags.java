package com.lustig7.overdrive.registry;

import com.lustig7.overdrive.Overdrive;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

public final class ModTags {
    /** Block entity types that must never be accelerated (controllers, spawners, other accelerators). */
    public static final TagKey<BlockEntityType<?>> BLACKLISTED =
            TagKey.create(Registries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Overdrive.MODID, "blacklisted"));

    /** Blocks that count as passive cooling when adjacent to an Overdrive. */
    public static final TagKey<Block> PASSIVE_COOLANTS =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Overdrive.MODID, "passive_coolants"));

    /** Fluids accepted by the coolant tank, per tier. */
    public static final TagKey<Fluid> WATER_COOLANTS =
            TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(Overdrive.MODID, "water_coolants"));
    public static final TagKey<Fluid> CRYO_COOLANTS =
            TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(Overdrive.MODID, "cryo_coolants"));

    private ModTags() {}
}
