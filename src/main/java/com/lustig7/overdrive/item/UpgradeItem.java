package com.lustig7.overdrive.item;

import com.lustig7.overdrive.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Goes into one of the Overdrive's two upgrade slots. */
public class UpgradeItem extends Item {
    private final UpgradeType type;

    public UpgradeItem(UpgradeType type, Properties properties) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    public UpgradeType getType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = "item.overdrive.upgrade." + type.id() + ".effect";
        Object[] args = switch (type) {
            case HEAT_SINK -> new Object[]{fmt(Config.get(Config.UPGRADE_HEAT_SINK))};
            case EFFICIENCY -> new Object[]{fmt(Config.get(Config.UPGRADE_EFFICIENCY))};
            case OVERCHARGE -> new Object[]{fmt(Config.get(Config.UPGRADE_OVERCHARGE_HEAT))};
        };
        tooltip.add(Component.translatable(key, args).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.overdrive.upgrade.stacks").withStyle(ChatFormatting.GRAY));
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
