package com.lustig7.overdrive.item;

import com.lustig7.overdrive.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Used on an Overdrive: cools it down instantly (handled in OverdriveBlock#useItemOn).
 * Crafted together with a damaged chip: repairs it (ChipRepairRecipe).
 */
public class ThermalPasteItem extends Item {
    public ThermalPasteItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.overdrive.thermal_paste.use", (int) Math.round(Config.get(Config.PASTE_HEAT_REDUCTION)))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.overdrive.thermal_paste.repair", (int) Math.round(Config.get(Config.PASTE_CHIP_REPAIR) * 100))
                .withStyle(ChatFormatting.GRAY));
    }
}
