package com.lustig7.overdrive.network;

import com.lustig7.overdrive.Overdrive;
import com.lustig7.overdrive.menu.OverdriveMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: the player picked a multiplier step in the Overdrive GUI. */
public record SetMultiplierPayload(int containerId, int multiplier) implements CustomPacketPayload {
    public static final Type<SetMultiplierPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Overdrive.MODID, "set_multiplier"));

    public static final StreamCodec<ByteBuf, SetMultiplierPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetMultiplierPayload::containerId,
            ByteBufCodecs.VAR_INT, SetMultiplierPayload::multiplier,
            SetMultiplierPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Runs on the server main thread (PayloadRegistrar default). */
    public static void handle(SetMultiplierPayload payload, IPayloadContext ctx) {
        if (ctx.player().containerMenu instanceof OverdriveMenu menu
                && menu.containerId == payload.containerId()
                && menu.stillValid(ctx.player())) {
            menu.setMultiplier(payload.multiplier());
        }
    }
}
