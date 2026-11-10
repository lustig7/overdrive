package com.lustig7.overdrive.block;

import com.lustig7.overdrive.Config;
import com.lustig7.overdrive.Overdrive;
import com.lustig7.overdrive.item.OverclockChipItem;
import com.lustig7.overdrive.item.UpgradeItem;
import com.lustig7.overdrive.item.UpgradeType;
import com.lustig7.overdrive.logic.RedstoneMode;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import com.lustig7.overdrive.logic.HeatStatus;
import com.lustig7.overdrive.logic.OverdriveMath;
import com.lustig7.overdrive.menu.OverdriveMenu;
import com.lustig7.overdrive.registry.ModRegistry;
import com.lustig7.overdrive.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.lustig7.overdrive.advancement.ModTriggers;
import com.lustig7.overdrive.logic.CoolingSource;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OverdriveBlockEntity extends BlockEntity implements MenuProvider {
    // ContainerData slots. Values travel as shorts, so ints are split into lo/hi halves.
    public static final int DATA_SELECTED = 0;
    public static final int DATA_EFFECTIVE = 1;
    public static final int DATA_HEAT_X10 = 2;
    public static final int DATA_ENERGY_LO = 3, DATA_ENERGY_HI = 4;
    public static final int DATA_CAPACITY_LO = 5, DATA_CAPACITY_HI = 6;
    public static final int DATA_COST_LO = 7, DATA_COST_HI = 8;
    public static final int DATA_LIMIT = 9;
    public static final int DATA_PASSIVE_BLOCKS = 10;
    public static final int DATA_STATUS = 11;
    public static final int DATA_TARGET = 12;
    public static final int DATA_TANK_LO = 13, DATA_TANK_HI = 14;
    public static final int DATA_TANK_CAP_LO = 15, DATA_TANK_CAP_HI = 16;
    public static final int DATA_TANK_FLUID = 17; // CoolingSource of the stored fluid (NONE/WATER/CRYO)
    public static final int DATA_COOLING_SOURCE = 18; // CoolingSource in effect
    public static final int DATA_COOLING_LO = 19, DATA_COOLING_HI = 20; // capacity, rounded
    public static final int DATA_REDSTONE_MODE = 21;
    public static final int DATA_REDSTONE_BLOCKED = 22; // 1 while the redstone mode holds the Overdrive at 1x
    public static final int DATA_COUNT = 23;

    public static final int TARGET_NONE = 0, TARGET_OK = 1, TARGET_BLOCKED = 2, TARGET_NOT_TICKING = 3;

    public static final int UPGRADE_SLOTS = 2;

    /** Types whose ticker threw during an extra tick; skipped until restart. */
    private static final Set<BlockEntityType<?>> FAULTED = ConcurrentHashMap.newKeySet();

    private final OverdriveEnergyStorage energy = new OverdriveEnergyStorage(this::setChanged);
    private final ItemStackHandler chipHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof OverclockChipItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private final ItemStackHandler upgradeHandler = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof UpgradeItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private final FluidTank tank = new FluidTank(Config.get(Config.TANK_CAPACITY), s -> coolantType(s) != CoolingSource.NONE) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    // persisted
    private int selected = 1;
    private double coolantDebt;
    private double heat;
    private boolean throttled;
    private boolean shutdownLatched;
    private double wearAccumulator;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;

    // runtime
    private int effective = 1;
    private long lastCost;
    private int lastExtraTicks;
    private int passiveBlocks;
    private int coolingTimer;
    private HeatStatus status = HeatStatus.OFF;
    private int targetStatus = TARGET_NONE;
    private CoolingSource coolingSource = CoolingSource.NONE;
    private double coolingCapacity;
    private boolean powered;
    private boolean redstoneBlocked;
    private int redstoneTimer;
    private int comparatorLevel = -1;
    // last values sent to clients (sound + particles); on the client these hold the received values
    private int syncedEffective = 1;
    private CoolingSource syncedCooling = CoolingSource.NONE;

    @Nullable private BlockEntity cachedTarget;
    @Nullable private BlockState cachedTargetState;
    @Nullable private BlockEntityTicker<BlockEntity> cachedTicker;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_SELECTED -> selected;
                case DATA_EFFECTIVE -> effective;
                case DATA_HEAT_X10 -> (int) Math.round(heat * 10.0);
                case DATA_ENERGY_LO -> lo(energy.getEnergyStored());
                case DATA_ENERGY_HI -> hi(energy.getEnergyStored());
                case DATA_CAPACITY_LO -> lo(energy.getMaxEnergyStored());
                case DATA_CAPACITY_HI -> hi(energy.getMaxEnergyStored());
                case DATA_COST_LO -> lo((int) Math.min(Integer.MAX_VALUE, lastCost));
                case DATA_COST_HI -> hi((int) Math.min(Integer.MAX_VALUE, lastCost));
                case DATA_LIMIT -> getMultiplierLimit();
                case DATA_PASSIVE_BLOCKS -> passiveBlocks;
                case DATA_STATUS -> status.ordinal();
                case DATA_TARGET -> targetStatus;
                case DATA_TANK_LO -> lo(tank.getFluidAmount());
                case DATA_TANK_HI -> hi(tank.getFluidAmount());
                case DATA_TANK_CAP_LO -> lo(tank.getCapacity());
                case DATA_TANK_CAP_HI -> hi(tank.getCapacity());
                case DATA_TANK_FLUID -> coolantType(tank.getFluid()).ordinal();
                case DATA_COOLING_SOURCE -> coolingSource.ordinal();
                case DATA_COOLING_LO -> lo((int) Math.round(coolingCapacity));
                case DATA_COOLING_HI -> hi((int) Math.round(coolingCapacity));
                case DATA_REDSTONE_MODE -> redstoneMode.ordinal();
                case DATA_REDSTONE_BLOCKED -> redstoneBlocked ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // read-only on the server; the client copy is a SimpleContainerData
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public OverdriveBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.OVERDRIVE_BE.get(), pos, state);
    }

    public static int lo(int v) {
        return v & 0xFFFF;
    }

    public static int hi(int v) {
        return (v >>> 16) & 0xFFFF;
    }

    public static int join(int lo, int hi) {
        return ((hi & 0xFFFF) << 16) | (lo & 0xFFFF);
    }

    // ------------------------------------------------------------------ ticking

    /** Client: keeps the looping hum alive while the server reports a multiplier above 1x. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, OverdriveBlockEntity be) {
        if (be.syncedEffective > 1 && clientHook != null) clientHook.accept(be);
    }

    /** Set by the client entry point (keeps client classes out of this common class). */
    @Nullable public static java.util.function.Consumer<OverdriveBlockEntity> clientHook;

    public static void serverTick(Level level, BlockPos pos, BlockState state, OverdriveBlockEntity be) {
        be.tickServer((ServerLevel) level, pos, state);
    }

    private void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(OverdriveBlock.FACING);
        BlockPos targetPos = pos.relative(facing);

        if (--coolingTimer <= 0) {
            coolingTimer = 20;
            passiveBlocks = countPassiveCoolers(level, pos, facing);
        }
        double cooling = updateCooling();
        double throttleAt = Config.get(Config.THROTTLE_THRESHOLD);
        double damageAt = Config.get(Config.DAMAGE_THRESHOLD);
        int baseCost = Config.get(Config.BASE_ENERGY_COST);
        double efficiency = OverdriveMath.upgradeFactor(Config.get(Config.UPGRADE_EFFICIENCY), countUpgrades(UpgradeType.EFFICIENCY));
        double heatFactor = Config.get(Config.HEAT_FACTOR)
                * OverdriveMath.upgradeFactor(Config.get(Config.UPGRADE_OVERCHARGE_HEAT), countUpgrades(UpgradeType.OVERCHARGE));

        if (--redstoneTimer <= 0) {
            redstoneTimer = 4;
            powered = level.hasNeighborSignal(pos);
        }
        redstoneBlocked = !redstoneMode.allows(powered);

        BlockEntityTicker<BlockEntity> ticker = level.shouldTickBlocksAt(targetPos) ? resolveTarget(level, targetPos) : null;
        if (ticker == null && !level.shouldTickBlocksAt(targetPos)) targetStatus = TARGET_NONE;

        if (shutdownLatched && heat < throttleAt) shutdownLatched = false;
        int requested = (ticker == null || shutdownLatched || redstoneBlocked) ? 1 : Math.min(OverdriveMath.floorStep(selected), getMultiplierLimit());

        // Throttle with hysteresis so the block doesn't flicker at the threshold.
        throttled = throttled ? heat >= throttleAt - Config.get(Config.THROTTLE_HYSTERESIS) : heat >= throttleAt;
        int m = throttled ? OverdriveMath.throttle(requested) : requested;
        m = OverdriveMath.powerLimited(m, energy.getEnergyStored(), baseCost, efficiency);
        effective = m;

        heat = OverdriveMath.stepHeat(heat,
                OverdriveMath.equilibriumHeat(m, cooling, heatFactor),
                Config.get(Config.HEAT_RISE_RATE), Config.get(Config.HEAT_DECAY_RATE));

        lastCost = OverdriveMath.upgradedCost(m, baseCost, efficiency);
        energy.consume(lastCost);
        consumeCoolant(m);

        int granted = TickBudget.claim(level.dimension(), targetPos, m - 1, Config.get(Config.MAX_EXTRA_TICKS_PER_CHUNK));
        lastExtraTicks = runExtraTicks(level, targetPos, ticker, granted);

        wearChip(level, pos, m, damageAt);

        if (m > 1 && level.getGameTime() % 20 == 0) {
            for (var player : ModTriggers.playersNear(level, pos)) {
                ModTriggers.OVERCLOCK.get().trigger(player, m, coolingSource);
            }
        }

        HeatStatus s = OverdriveMath.statusForHeat(heat, m, throttleAt, damageAt);
        if (throttled && s.ordinal() < HeatStatus.THROTTLING.ordinal()) s = HeatStatus.THROTTLING;
        status = s;

        if (status == HeatStatus.DAMAGE && level.getGameTime() % 40 == 0) {
            level.playSound(null, pos, ModRegistry.SOUND_ALARM.get(), SoundSource.BLOCKS, 0.8F, 1.6F);
        }
        if (heat >= 100.0 && !shutdownLatched) {
            meltdown(level, pos);
            if (isRemoved()) return;
        }

        int visual = status.visualState;
        if (state.getValue(OverdriveBlock.HEAT_STATE) != visual) {
            level.setBlock(pos, state.setValue(OverdriveBlock.HEAT_STATE, visual), Block.UPDATE_CLIENTS);
        }
        if (m > 1 || heat > 0.0) setChanged();
        updateComparator(level, pos);
        syncToClients(level, pos, state);
    }

    private void updateComparator(Level level, BlockPos pos) {
        int c = OverdriveMath.comparatorLevel(heat);
        if (c != comparatorLevel) {
            comparatorLevel = c;
            level.updateNeighbourForOutputSignal(pos, getBlockState().getBlock());
        }
    }

    /** Clients only need the multiplier and coolant (for the hum and particles); sent only on change. */
    private void syncToClients(Level level, BlockPos pos, BlockState state) {
        if (effective != syncedEffective || coolingSource != syncedCooling) {
            syncedEffective = effective;
            syncedCooling = coolingSource;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public int getComparatorLevel() {
        return OverdriveMath.comparatorLevel(heat);
    }

    @Nullable
    private BlockEntityTicker<BlockEntity> resolveTarget(Level level, BlockPos targetPos) {
        BlockEntity target = level.getBlockEntity(targetPos);
        if (target == null || target.isRemoved()) {
            clearTargetCache();
            targetStatus = TARGET_NONE;
            return null;
        }
        BlockState ts = level.getBlockState(targetPos);
        if (target == cachedTarget && ts == cachedTargetState) {
            return cachedTicker;
        }
        cachedTarget = target;
        cachedTargetState = ts;
        cachedTicker = null;
        if (!canAccelerate(target)) {
            targetStatus = TARGET_BLOCKED;
            return null;
        }
        @SuppressWarnings("unchecked")
        BlockEntityTicker<BlockEntity> t = (BlockEntityTicker<BlockEntity>) ts.getTicker(level, target.getType());
        cachedTicker = t;
        targetStatus = t == null ? TARGET_NOT_TICKING : TARGET_OK;
        return t;
    }

    private void clearTargetCache() {
        cachedTarget = null;
        cachedTargetState = null;
        cachedTicker = null;
    }

    /** Never accelerate another Overdrive (no recursion), blacklisted types, or types that crashed before. */
    public static boolean canAccelerate(BlockEntity target) {
        if (target instanceof OverdriveBlockEntity) return false;
        BlockEntityType<?> type = target.getType();
        return !isBlacklisted(type) && !FAULTED.contains(type);
    }

    public static boolean isBlacklisted(BlockEntityType<?> type) {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.wrapAsHolder(type).is(ModTags.BLACKLISTED);
    }

    private int runExtraTicks(Level level, BlockPos targetPos, @Nullable BlockEntityTicker<BlockEntity> ticker, int count) {
        BlockEntity target = cachedTarget;
        if (ticker == null || target == null || count <= 0) return 0;
        int done = 0;
        try {
            for (; done < count; done++) {
                if (target.isRemoved()) break;
                BlockState current = target.getBlockState();
                if (!target.getType().isValid(current)) break;
                ticker.tick(level, targetPos, current, target);
            }
        } catch (RuntimeException e) {
            FAULTED.add(target.getType());
            Overdrive.LOGGER.error("Overdrive at {} disabled acceleration of {} after it threw during an extra tick",
                    worldPosition, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(target.getType()), e);
            clearTargetCache();
        }
        return done;
    }

    private int countPassiveCoolers(Level level, BlockPos pos, Direction facing) {
        int n = 0;
        for (Direction d : Direction.values()) {
            if (d != facing && level.getBlockState(pos.relative(d)).is(ModTags.PASSIVE_COOLANTS)) n++;
        }
        return n;
    }

    /** Coolant tier of a fluid: CRYO, WATER or NONE (not accepted by the tank). */
    public static CoolingSource coolantType(FluidStack stack) {
        if (stack.isEmpty()) return CoolingSource.NONE;
        if (stack.is(ModTags.CRYO_COOLANTS)) return CoolingSource.CRYO;
        if (stack.is(ModTags.WATER_COOLANTS)) return CoolingSource.WATER;
        return CoolingSource.NONE;
    }

    /** Picks the best available cooling: tank fluid if present, otherwise adjacent passive blocks. */
    private double updateCooling() {
        double none = Config.get(Config.COOLING_NONE);
        double passive = OverdriveMath.passiveCooling(passiveBlocks, none, Config.get(Config.COOLING_PASSIVE));
        CoolingSource fluid = coolantType(tank.getFluid());
        double fluidCap = switch (fluid) {
            case WATER -> Config.get(Config.COOLING_WATER);
            case CRYO -> Config.get(Config.COOLING_CRYO);
            default -> 0.0;
        };
        double sink = OverdriveMath.upgradeFactor(Config.get(Config.UPGRADE_HEAT_SINK), countUpgrades(UpgradeType.HEAT_SINK));
        coolingCapacity = OverdriveMath.coolingCapacity(passive, fluidCap) * sink;
        if (fluid.isFluid() && fluidCap >= passive) coolingSource = fluid;
        else coolingSource = passiveBlocks > 0 ? CoolingSource.PASSIVE : CoolingSource.NONE;
        return coolingCapacity;
    }

    private void consumeCoolant(int m) {
        if (!coolingSource.isFluid()) {
            coolantDebt = 0.0;
            return;
        }
        double rate = coolingSource == CoolingSource.CRYO ? Config.get(Config.CRYO_USAGE) : Config.get(Config.WATER_USAGE);
        coolantDebt += OverdriveMath.coolantPerTick(m, rate);
        if (coolantDebt >= 1.0) {
            int mb = (int) coolantDebt;
            coolantDebt -= mb;
            tank.drain(mb, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void wearChip(Level level, BlockPos pos, int m, double damageAt) {
        ItemStack chip = chipHandler.getStackInSlot(0);
        if (m <= 1 || chip.isEmpty() || !chip.isDamageableItem()) return;
        wearAccumulator += OverdriveMath.chipWearPerSecond(m, heat, damageAt, Config.get(Config.CHIP_WEAR_MULTIPLIER)) / 20.0;
        if (wearAccumulator < 1.0) return;
        int dmg = (int) wearAccumulator;
        wearAccumulator -= dmg;
        int newDamage = chip.getDamageValue() + dmg;
        if (newDamage >= chip.getMaxDamage()) {
            chipHandler.setStackInSlot(0, ItemStack.EMPTY);
            level.playSound(null, pos, ModRegistry.SOUND_CHIP_BREAK.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
        } else {
            chip.setDamageValue(newDamage);
            setChanged();
        }
    }

    private void meltdown(ServerLevel level, BlockPos pos) {
        for (var player : ModTriggers.playersNear(level, pos)) {
            ModTriggers.MELTDOWN.get().trigger(player);
        }
        switch (Config.get(Config.MELTDOWN_ACTION)) {
            case SHUTDOWN -> {
                shutdownLatched = true;
                level.playSound(null, pos, ModRegistry.SOUND_SHUTDOWN.get(), SoundSource.BLOCKS, 1.0F, 0.6F);
            }
            case DESTROY -> {
                chipHandler.setStackInSlot(0, ItemStack.EMPTY);
                level.destroyBlock(pos, true);
            }
            case EXPLODE -> {
                chipHandler.setStackInSlot(0, ItemStack.EMPTY);
                for (int i = 0; i < UPGRADE_SLOTS; i++) upgradeHandler.setStackInSlot(i, ItemStack.EMPTY);
                level.removeBlock(pos, false);
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        (float) Config.get(Config.EXPLOSION_POWER),
                        Config.get(Config.EXPLOSION_BREAKS_BLOCKS) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
            }
        }
    }

    // ------------------------------------------------------------------ state

    /** Highest multiplier allowed by the chip (or lack of one) and the server config. */
    public int getMultiplierLimit() {
        ItemStack chip = chipHandler.getStackInSlot(0);
        int limit = chip.getItem() instanceof OverclockChipItem c ? c.getMaxMultiplier() : Config.get(Config.NO_CHIP_MAX_MULTIPLIER);
        return OverdriveMath.overchargedLimit(limit, countUpgrades(UpgradeType.OVERCHARGE), Config.get(Config.MAX_MULTIPLIER));
    }

    public int countUpgrades(UpgradeType type) {
        int n = 0;
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            if (upgradeHandler.getStackInSlot(i).getItem() instanceof UpgradeItem u && u.getType() == type) n++;
        }
        return n;
    }

    /** Thermal Paste: instant heat drop. Returns false if there was nothing to cool. */
    public boolean applyThermalPaste() {
        if (heat <= 0.0) return false;
        setHeat(heat - Config.get(Config.PASTE_HEAT_REDUCTION));
        setChanged();
        return true;
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode mode) {
        redstoneMode = mode;
        redstoneTimer = 0; // re-read the signal next tick
        setChanged();
    }

    public boolean isRedstoneBlocked() {
        return redstoneBlocked;
    }

    public ItemStackHandler getUpgradeHandler() {
        return upgradeHandler;
    }

    /** Client side: multiplier and coolant last received from the server. */
    public int getClientEffective() {
        return syncedEffective;
    }

    public CoolingSource getClientCooling() {
        return syncedCooling;
    }

    public void setSelected(int multiplier) {
        int m = OverdriveMath.floorStep(multiplier);
        if (m != selected) {
            selected = m;
            setChanged();
        }
    }

    public int getSelected() {
        return selected;
    }

    public int getEffective() {
        return effective;
    }

    public double getHeat() {
        return heat;
    }

    public void setHeat(double heat) {
        this.heat = Math.max(0.0, Math.min(100.0, heat));
    }

    public HeatStatus getStatus() {
        return status;
    }

    public long getLastCost() {
        return lastCost;
    }

    public int getLastExtraTicks() {
        return lastExtraTicks;
    }

    public int getTargetStatus() {
        return targetStatus;
    }

    public OverdriveEnergyStorage getEnergyStorage() {
        return energy;
    }

    public ItemStackHandler getChipHandler() {
        return chipHandler;
    }

    public FluidTank getTank() {
        return tank;
    }

    public CoolingSource getCoolingSource() {
        return coolingSource;
    }

    public int getPassiveBlocks() {
        return passiveBlocks;
    }

    public double getCoolingCapacity() {
        return coolingCapacity;
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.overdrive.overdrive");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new OverdriveMenu(containerId, inventory, this, data);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Selected", selected);
        tag.putDouble("Heat", heat);
        tag.putBoolean("Throttled", throttled);
        tag.putBoolean("Shutdown", shutdownLatched);
        tag.putDouble("Wear", wearAccumulator);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.put("Chip", chipHandler.serializeNBT(registries));
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putDouble("CoolantDebt", coolantDebt);
        tag.put("Upgrades", upgradeHandler.serializeNBT(registries));
        tag.putInt("RedstoneMode", redstoneMode.ordinal());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SyncEffective", effective);
        tag.putInt("SyncCooling", coolingSource.ordinal());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        syncedEffective = Math.max(1, tag.getInt("SyncEffective"));
        syncedCooling = CoolingSource.byId(tag.getInt("SyncCooling"));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        selected = OverdriveMath.floorStep(tag.contains("Selected") ? tag.getInt("Selected") : 1);
        heat = tag.getDouble("Heat");
        throttled = tag.getBoolean("Throttled");
        shutdownLatched = tag.getBoolean("Shutdown");
        wearAccumulator = tag.getDouble("Wear");
        energy.setEnergy(tag.getInt("Energy"));
        if (tag.contains("Chip")) chipHandler.deserializeNBT(registries, tag.getCompound("Chip"));
        if (tag.contains("Tank")) tank.readFromNBT(registries, tag.getCompound("Tank"));
        coolantDebt = tag.getDouble("CoolantDebt");
        if (tag.contains("Upgrades")) upgradeHandler.deserializeNBT(registries, tag.getCompound("Upgrades"));
        redstoneMode = RedstoneMode.byId(tag.getInt("RedstoneMode"));
    }
}
