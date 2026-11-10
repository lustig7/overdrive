package com.lustig7.overdrive.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Goes into the Overdrive's chip slot. Its tier caps the multiplier; it wears out while running. */
public class OverclockChipItem extends Item {
    private final int tier;
    private final int maxMultiplier;

    public OverclockChipItem(int tier, int maxMultiplier, Properties properties) {
        super(properties);
        this.tier = tier;
        this.maxMultiplier = maxMultiplier;
    }

    public int getTier() {
        return tier;
    }

    public int getMaxMultiplier() {
        return maxMultiplier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.overdrive.overclock_chip.max", maxMultiplier).withStyle(ChatFormatting.AQUA));
        int max = stack.getMaxDamage();
        if (max > 0) {
            int left = max - stack.getDamageValue();
            tooltip.add(Component.translatable("item.overdrive.overclock_chip.durability", left, max).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
