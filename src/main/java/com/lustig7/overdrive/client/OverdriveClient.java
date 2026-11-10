package com.lustig7.overdrive.client;

import com.lustig7.overdrive.Overdrive;
import com.lustig7.overdrive.block.OverdriveBlockEntity;
import com.lustig7.overdrive.registry.ModRegistry;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Overdrive.MODID, dist = Dist.CLIENT)
public class OverdriveClient {
    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final int CRYO_TINT = 0xE0A5F3FC;

    public OverdriveClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener((RegisterMenuScreensEvent e) -> e.register(ModRegistry.OVERDRIVE_MENU.get(), OverdriveScreen::new));
        modBus.addListener(OverdriveClient::registerClientExtensions);
        modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(OverdriveClient::setRenderLayers));
        OverdriveBlockEntity.clientHook = OverdriveHumSound::ensurePlaying;
    }

    @SuppressWarnings("deprecation") // still the supported way to put a fluid on the translucent layer in 1.21.1
    private static void setRenderLayers() {
        ItemBlockRenderTypes.setRenderLayer(ModRegistry.CRYO_SOURCE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModRegistry.CRYO_FLOWING.get(), RenderType.translucent());
    }

    /** Cryo Coolant reuses the vanilla water textures, tinted icy cyan. */
    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return WATER_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return WATER_FLOW;
            }

            @Override
            public int getTintColor() {
                return CRYO_TINT;
            }
        }, ModRegistry.CRYO_TYPE.get());
    }
}
