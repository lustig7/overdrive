package com.lustig7.overdrive.recipe;

import com.lustig7.overdrive.Config;
import com.lustig7.overdrive.item.OverclockChipItem;
import com.lustig7.overdrive.item.ThermalPasteItem;
import com.lustig7.overdrive.logic.OverdriveMath;
import com.lustig7.overdrive.registry.ModRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** One damaged Overclock Chip + 1-4 Thermal Paste (shapeless): restores durability per paste. */
public class ChipRepairRecipe extends CustomRecipe {
    public ChipRepairRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return assemble(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = assemble(input);
        return result == null ? ItemStack.EMPTY : result;
    }

    private static ItemStack assemble(CraftingInput input) {
        ItemStack chip = ItemStack.EMPTY;
        int pastes = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof OverclockChipItem) {
                if (!chip.isEmpty()) return null;
                chip = s;
            } else if (s.getItem() instanceof ThermalPasteItem) {
                pastes++;
            } else {
                return null;
            }
        }
        if (chip.isEmpty() || pastes == 0 || pastes > 4 || !chip.isDamaged()) return null;
        ItemStack out = chip.copyWithCount(1);
        out.setDamageValue(OverdriveMath.repairedDamage(chip.getDamageValue(), chip.getMaxDamage(), pastes,
                Config.get(Config.PASTE_CHIP_REPAIR)));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.CHIP_REPAIR.get();
    }
}
