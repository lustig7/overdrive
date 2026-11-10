package com.lustig7.overdrive.network;

import com.lustig7.overdrive.Overdrive;
import com.lustig7.overdrive.menu.OverdriveMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: the player clicked the redstone mode button in the Overdrive GUI. */
public record CycleRedstoneModePayload(int containerId) implements CustomPacketPayload {
    public static final Type<CycleRedstoneModePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Overdrive.MODID, "cycle_redstone_mode"));

    public static final StreamCodec<ByteBuf, CycleRedstoneModePayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(CycleRedstoneModePayload::new, CycleRedstoneModePayload::containerId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CycleRedstoneModePayload payload, IPayloadContext ctx) {
        if (ctx.player().containerMenu instanceof OverdriveMenu menu
                && menu.containerId == payload.containerId()
                && menu.stillValid(ctx.player())) {
            menu.cycleRedstoneMode();
        }
    }
}
