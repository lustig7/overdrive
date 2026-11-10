package com.lustig7.overdrive.menu;

import com.lustig7.overdrive.block.OverdriveBlockEntity;
import com.lustig7.overdrive.item.OverclockChipItem;
import com.lustig7.overdrive.item.UpgradeItem;
import com.lustig7.overdrive.item.UpgradeType;
import com.lustig7.overdrive.logic.RedstoneMode;
import com.lustig7.overdrive.logic.CoolingSource;
import com.lustig7.overdrive.logic.HeatStatus;
import com.lustig7.overdrive.logic.OverdriveMath;
import com.lustig7.overdrive.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class OverdriveMenu extends AbstractContainerMenu {
    // Slot positions, shared with OverdriveScreen.
    public static final int CHIP_X = 13, CHIP_Y = 121;
    public static final int UPGRADE_X = 13, UPGRADE_Y = 149, UPGRADE_GAP = 20;
    public static final int INV_X = 18, INV_Y = 178, HOTBAR_Y = 236;
    /** Slot indices: 0 chip, 1-2 upgrades, then the player inventory. */
    public static final int FIRST_UPGRADE = 1, PLAYER_START = 1 + OverdriveBlockEntity.UPGRADE_SLOTS, HOTBAR_START = PLAYER_START + 27;

    private final ContainerData data;
    private final BlockPos pos;
    private final ContainerLevelAccess access;
    @Nullable private final OverdriveBlockEntity blockEntity;

    /** Client constructor (opened via network). */
    public OverdriveMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof OverclockChipItem;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        }, new ItemStackHandler(OverdriveBlockEntity.UPGRADE_SLOTS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof UpgradeItem;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        }, new SimpleContainerData(OverdriveBlockEntity.DATA_COUNT), null);
    }

    /** Server constructor. */
    public OverdriveMenu(int id, Inventory inv, OverdriveBlockEntity be, ContainerData data) {
        this(id, inv, be.getBlockPos(), be.getChipHandler(), be.getUpgradeHandler(), data, be);
    }

    private OverdriveMenu(int id, Inventory inv, BlockPos pos, IItemHandler chip, IItemHandler upgrades, ContainerData data,
                          @Nullable OverdriveBlockEntity be) {
        super(ModRegistry.OVERDRIVE_MENU.get(), id);
        checkContainerDataCount(data, OverdriveBlockEntity.DATA_COUNT);
        this.data = data;
        this.pos = pos;
        this.blockEntity = be;
        this.access = be != null && be.getLevel() != null ? ContainerLevelAccess.create(be.getLevel(), pos) : ContainerLevelAccess.NULL;

        addSlot(new SlotItemHandler(chip, 0, CHIP_X, CHIP_Y));
        for (int i = 0; i < OverdriveBlockEntity.UPGRADE_SLOTS; i++) {
            addSlot(new SlotItemHandler(upgrades, i, UPGRADE_X + i * UPGRADE_GAP, UPGRADE_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_X + col * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    /** Server side: called from the SetMultiplierPayload handler. */
    public void setMultiplier(int multiplier) {
        if (blockEntity != null && OverdriveMath.isStep(multiplier)) blockEntity.setSelected(multiplier);
    }

    /** Server side: called from the CycleRedstoneModePayload handler. */
    public void cycleRedstoneMode() {
        if (blockEntity != null) blockEntity.setRedstoneMode(blockEntity.getRedstoneMode().next());
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byId(data.get(OverdriveBlockEntity.DATA_REDSTONE_MODE));
    }

    public boolean redstoneBlocked() {
        return data.get(OverdriveBlockEntity.DATA_REDSTONE_BLOCKED) != 0;
    }

    /** Number of installed upgrade cards of a type (read from the synced slots). */
    public int upgrades(UpgradeType type) {
        int n = 0;
        for (int i = 0; i < OverdriveBlockEntity.UPGRADE_SLOTS; i++) {
            if (getSlot(FIRST_UPGRADE + i).getItem().getItem() instanceof UpgradeItem u && u.getType() == type) n++;
        }
        return n;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int selected() {
        return data.get(OverdriveBlockEntity.DATA_SELECTED);
    }

    public int effective() {
        return data.get(OverdriveBlockEntity.DATA_EFFECTIVE);
    }

    public double heat() {
        return data.get(OverdriveBlockEntity.DATA_HEAT_X10) / 10.0;
    }

    public int energy() {
        return OverdriveBlockEntity.join(data.get(OverdriveBlockEntity.DATA_ENERGY_LO), data.get(OverdriveBlockEntity.DATA_ENERGY_HI));
    }

    public int capacity() {
        return OverdriveBlockEntity.join(data.get(OverdriveBlockEntity.DATA_CAPACITY_LO), data.get(OverdriveBlockEntity.DATA_CAPACITY_HI));
    }

    public int cost() {
        return OverdriveBlockEntity.join(data.get(OverdriveBlockEntity.DATA_COST_LO), data.get(OverdriveBlockEntity.DATA_COST_HI));
    }

    public int limit() {
        return data.get(OverdriveBlockEntity.DATA_LIMIT);
    }

    public int passiveBlocks() {
        return data.get(OverdriveBlockEntity.DATA_PASSIVE_BLOCKS);
    }

    public HeatStatus status() {
        return HeatStatus.byId(data.get(OverdriveBlockEntity.DATA_STATUS));
    }

    public int targetStatus() {
        return data.get(OverdriveBlockEntity.DATA_TARGET);
    }

    public int tankAmount() {
        return OverdriveBlockEntity.join(data.get(OverdriveBlockEntity.DATA_TANK_LO), data.get(OverdriveBlockEntity.DATA_TANK_HI));
    }

    public int tankCapacity() {
        return OverdriveBlockEntity.join(data.get(OverdriveBlockEntity.DATA_TANK_CAP_LO), data.get(OverdriveBlockEntity.DATA_TANK_CAP_HI));
    }

    public CoolingSource tankFluid() {
        return CoolingSource.byId(data.get(OverdriveBlockEntity.DATA_TANK_FLUID));
    }

    public CoolingSource coolingSource() {
        return CoolingSource.byId(data.get(OverdriveBlockEntity.DATA_COOLING_SOURCE));
    }

    public int coolingCapacity() {
        return OverdriveBlockEntity.join(data.get(OverdriveBlockEntity.DATA_COOLING_LO), data.get(OverdriveBlockEntity.DATA_COOLING_HI));
    }

    public ItemStack chip() {
        return getSlot(0).getItem();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int playerEnd = slots.size();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, playerEnd, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof OverclockChipItem && !slots.get(0).hasItem()) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof UpgradeItem && moveItemStackTo(stack, FIRST_UPGRADE, PLAYER_START, false)) {
            // moved into a free upgrade slot
        } else if (index < HOTBAR_START) {
            if (!moveItemStackTo(stack, HOTBAR_START, playerEnd, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModRegistry.OVERDRIVE.get());
    }
}
