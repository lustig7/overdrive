package com.lustig7.overdrive.advancement;

import com.lustig7.overdrive.Overdrive;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ModTriggers {
    /** Players within this many blocks of an Overdrive count as "running" it. */
    public static final double RANGE = 16.0;

    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, Overdrive.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, OverclockTrigger> OVERCLOCK =
            TRIGGERS.register("overclock", OverclockTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, MeltdownTrigger> MELTDOWN =
            TRIGGERS.register("meltdown", MeltdownTrigger::new);

    private ModTriggers() {}

    public static void register(IEventBus bus) {
        TRIGGERS.register(bus);
    }

    public static List<ServerPlayer> playersNear(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(RANGE), p -> !p.isSpectator());
    }
}
