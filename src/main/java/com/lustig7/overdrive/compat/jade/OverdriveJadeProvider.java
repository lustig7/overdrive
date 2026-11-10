package com.lustig7.overdrive.compat.jade;

import com.lustig7.overdrive.Overdrive;
import com.lustig7.overdrive.block.OverdriveBlockEntity;
import com.lustig7.overdrive.logic.CoolingSource;
import com.lustig7.overdrive.logic.HeatStatus;
import com.lustig7.overdrive.logic.RedstoneMode;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/** Multiplier, heat, energy use, cooling and redstone state. Energy and fluid bars come from Jade's own providers. */
public enum OverdriveJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Overdrive.MODID, "overdrive");

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof OverdriveBlockEntity be)) return;
        data.putInt("Selected", be.getSelected());
        data.putInt("Effective", be.getEffective());
        data.putDouble("Heat", be.getHeat());
        data.putInt("Status", be.getStatus().ordinal());
        data.putLong("Cost", be.getLastCost());
        data.putInt("Cooling", be.getCoolingSource().ordinal());
        data.putInt("Passive", be.getPassiveBlocks());
        data.putInt("CoolingCap", (int) Math.round(be.getCoolingCapacity()));
        data.putInt("Redstone", be.getRedstoneMode().ordinal());
        data.putBoolean("Blocked", be.isRedstoneBlocked());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag d = accessor.getServerData();
        if (!d.contains("Selected")) return;
        int selected = d.getInt("Selected"), effective = d.getInt("Effective");
        HeatStatus status = HeatStatus.byId(d.getInt("Status"));
        Component mult = Component.literal(effective + "×").withStyle(effective < selected ? ChatFormatting.GOLD : ChatFormatting.AQUA);
        if (effective != selected) mult = mult.copy().append(Component.literal(" / " + selected + "×").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jade.overdrive.multiplier", mult));
        tooltip.add(Component.translatable("jade.overdrive.heat",
                Component.literal(String.format(Locale.ROOT, "%.1f %%", d.getDouble("Heat"))).withColor(status.color & 0xFFFFFF),
                Component.translatable(status.translationKey())));
        if (d.getLong("Cost") > 0) {
            tooltip.add(Component.translatable("jade.overdrive.usage", String.format("%,d", d.getLong("Cost"))));
        }
        CoolingSource cooling = CoolingSource.byId(d.getInt("Cooling"));
        tooltip.add(Component.translatable("jade.overdrive.cooling",
                Component.translatable(cooling.translationKey(), d.getInt("Passive")), String.format("%,d", d.getInt("CoolingCap"))));
        RedstoneMode mode = RedstoneMode.byId(d.getInt("Redstone"));
        if (mode != RedstoneMode.IGNORE) {
            tooltip.add(Component.translatable(mode.translationKey())
                    .withStyle(d.getBoolean("Blocked") ? ChatFormatting.RED : ChatFormatting.GRAY));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
