package com.lustig7.overdrive;

import com.lustig7.overdrive.advancement.ModTriggers;
import com.lustig7.overdrive.block.TickBudget;
import com.lustig7.overdrive.compat.FtbQuestsCompat;
import com.lustig7.overdrive.network.CycleRedstoneModePayload;
import com.lustig7.overdrive.network.SetMultiplierPayload;
import com.lustig7.overdrive.registry.ModRegistry;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(Overdrive.MODID)
public class Overdrive {
    public static final String MODID = "overdrive";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Overdrive(IEventBus modBus, ModContainer container) {
        ModRegistry.register(modBus);
        ModTriggers.register(modBus);
        modBus.addListener(Overdrive::registerCapabilities);
        modBus.addListener(Overdrive::registerPayloads);

        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre e) -> TickBudget.reset());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> TickBudget.clearAll());

        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        modBus.addListener((FMLCommonSetupEvent e) -> FtbQuestsCompat.installChapter());
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModRegistry.OVERDRIVE_BE.get(),
                (be, side) -> be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModRegistry.OVERDRIVE_BE.get(),
                (be, side) -> be.getChipHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModRegistry.OVERDRIVE_BE.get(),
                (be, side) -> be.getTank());
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(SetMultiplierPayload.TYPE, SetMultiplierPayload.STREAM_CODEC, SetMultiplierPayload::handle);
        registrar.playToServer(CycleRedstoneModePayload.TYPE, CycleRedstoneModePayload.STREAM_CODEC, CycleRedstoneModePayload::handle);
    }
}
