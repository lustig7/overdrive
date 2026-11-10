package com.lustig7.overdrive.compat.jei;

import com.lustig7.overdrive.Overdrive;
import com.lustig7.overdrive.registry.ModRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** Info pages. Only loaded by JEI, so the JEI API is never touched without JEI installed. */
@JeiPlugin
public class OverdriveJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Overdrive.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        info(registration, ModRegistry.OVERDRIVE_ITEM.get(), "overdrive");
        for (ItemLike chip : new ItemLike[]{ModRegistry.CHIP_T1.get(), ModRegistry.CHIP_T2.get(), ModRegistry.CHIP_T3.get(), ModRegistry.CHIP_T4.get()}) {
            info(registration, chip, "chip");
        }
        info(registration, ModRegistry.HEAT_SINK_UPGRADE.get(), "heat_sink_upgrade");
        info(registration, ModRegistry.EFFICIENCY_UPGRADE.get(), "efficiency_upgrade");
        info(registration, ModRegistry.OVERCHARGE_UPGRADE.get(), "overcharge_upgrade");
        info(registration, ModRegistry.THERMAL_PASTE.get(), "thermal_paste");
        info(registration, ModRegistry.CRYO_BUCKET.get(), "cryo_coolant");
    }

    private static void info(IRecipeRegistration registration, ItemLike item, String key) {
        registration.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.overdrive.info." + key));
    }
}
