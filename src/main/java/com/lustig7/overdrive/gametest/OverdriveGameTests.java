package com.lustig7.overdrive.gametest;

import com.lustig7.overdrive.Overdrive;
import com.lustig7.overdrive.block.OverdriveBlock;
import com.lustig7.overdrive.block.OverdriveBlockEntity;
import com.lustig7.overdrive.block.TickBudget;
import com.lustig7.overdrive.logic.CoolingSource;
import com.lustig7.overdrive.logic.HeatStatus;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import com.lustig7.overdrive.logic.OverdriveMath;
import com.lustig7.overdrive.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import com.lustig7.overdrive.logic.RedstoneMode;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Run with: ./gradlew runGameTestServer */
@GameTestHolder(Overdrive.MODID)
@PrefixGameTestTemplate(false)
public class OverdriveGameTests {
    private static final String EMPTY = "empty";
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    private static final BlockPos DRIVE = new BlockPos(2, 1, 3); // south of the machine, FACING north

    // ------------------------------------------------------------------ pure formulas

    @GameTest(template = EMPTY)
    public static void energyFormula(GameTestHelper h) {
        check(h, OverdriveMath.energyCost(1, 100) == 0, "1x must be free");
        check(h, OverdriveMath.energyCost(2, 100) == 400, "2x = 400 FE/t");
        check(h, OverdriveMath.energyCost(16, 100) == 25_600, "16x = 25,600 FE/t");
        check(h, OverdriveMath.energyCost(64, 100) == 409_600, "64x = 409,600 FE/t");
        // auto step-down instead of shutting off
        check(h, OverdriveMath.powerLimited(64, 409_600, 100) == 64, "full power keeps 64x");
        check(h, OverdriveMath.powerLimited(64, 409_599, 100) == 32, "1 FE short steps to 32x");
        check(h, OverdriveMath.powerLimited(64, 500, 100) == 2, "500 FE runs 2x");
        check(h, OverdriveMath.powerLimited(64, 0, 100) == 1, "empty buffer falls back to 1x");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void heatThresholds(GameTestHelper h) {
        // equilibrium: min(100, m^2 / cooling * 40)
        check(h, OverdriveMath.equilibriumHeat(1, 4, 40) == 0.0, "1x produces no heat");
        check(h, OverdriveMath.equilibriumHeat(2, 4, 40) == 40.0, "2x uncooled = 40 %");
        check(h, OverdriveMath.equilibriumHeat(4, 16, 40) == 40.0, "4x passive = 40 %");
        check(h, OverdriveMath.equilibriumHeat(16, 2048, 40) == 5.0, "16x cryo = 5 %");
        check(h, OverdriveMath.equilibriumHeat(64, 4, 40) == 100.0, "capped at 100 %");
        // thresholds
        check(h, OverdriveMath.statusForHeat(0, 1, 70, 90) == HeatStatus.OFF, "idle = OFF");
        check(h, OverdriveMath.statusForHeat(69.9, 8, 70, 90) == HeatStatus.STABLE, "69.9 % = STABLE");
        check(h, OverdriveMath.statusForHeat(70, 8, 70, 90) == HeatStatus.THROTTLING, "70 % = THROTTLING");
        check(h, OverdriveMath.statusForHeat(89.9, 8, 70, 90) == HeatStatus.THROTTLING, "89.9 % = THROTTLING");
        check(h, OverdriveMath.statusForHeat(90, 8, 70, 90) == HeatStatus.DAMAGE, "90 % = DAMAGE");
        check(h, OverdriveMath.statusForHeat(99.9, 8, 70, 90) == HeatStatus.DAMAGE, "99.9 % = DAMAGE");
        check(h, OverdriveMath.statusForHeat(100, 8, 70, 90) == HeatStatus.CRITICAL, "100 % = CRITICAL");
        check(h, OverdriveMath.throttle(16) == 8 && OverdriveMath.throttle(1) == 1, "throttle halves, never below 1");
        // heat stepping
        check(h, OverdriveMath.stepHeat(10, 40, 0.25, 0.5) == 10.25, "heat rises by riseRate");
        check(h, OverdriveMath.stepHeat(40, 10, 0.25, 0.5) == 39.5, "heat falls by decayRate");
        check(h, OverdriveMath.stepHeat(39.9, 40, 0.25, 0.5) == 40, "heat never overshoots");
        // passive cooling scales 4 -> 16
        check(h, OverdriveMath.passiveCooling(0, 4, 16) == 4, "no coolers");
        check(h, OverdriveMath.passiveCooling(2, 4, 16) == 10, "two coolers");
        check(h, OverdriveMath.passiveCooling(6, 4, 16) == 16, "capped at four");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tickBudget(GameTestHelper h) {
        TickBudget.reset();
        var dim = Level.OVERWORLD;
        BlockPos p = new BlockPos(1_000_000, 0, 1_000_000);
        check(h, TickBudget.claim(dim, p, 60, 100) == 60, "first claim granted");
        check(h, TickBudget.claim(dim, p.east(), 60, 100) == 40, "same chunk gets the rest");
        check(h, TickBudget.claim(dim, p, 10, 100) == 0, "budget exhausted");
        check(h, TickBudget.claim(dim, p.east(16), 10, 100) == 10, "other chunk unaffected");
        check(h, TickBudget.claim(dim, p, 10, 0) == 10, "0 = unlimited");
        TickBudget.reset();
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void blacklist(GameTestHelper h) {
        check(h, OverdriveBlockEntity.isBlacklisted(ModRegistry.OVERDRIVE_BE.get()), "Overdrive is blacklisted");
        check(h, OverdriveBlockEntity.isBlacklisted(BlockEntityType.MOB_SPAWNER), "spawner is blacklisted");
        check(h, OverdriveBlockEntity.isBlacklisted(BlockEntityType.TRIAL_SPAWNER), "trial spawner is blacklisted");
        check(h, !OverdriveBlockEntity.isBlacklisted(BlockEntityType.FURNACE), "furnace is allowed");
        h.succeed();
    }

    // ------------------------------------------------------------------ in-world behaviour

    private static OverdriveBlockEntity placeDrive(GameTestHelper h, int multiplier, int energy) {
        h.setBlock(DRIVE, ModRegistry.OVERDRIVE.get().defaultBlockState().setValue(OverdriveBlock.FACING, Direction.NORTH));
        OverdriveBlockEntity be = h.getBlockEntity(DRIVE);
        be.getChipHandler().setStackInSlot(0, new ItemStack(ModRegistry.CHIP_T4.get()));
        be.getEnergyStorage().setEnergy(energy);
        be.setSelected(multiplier);
        return be;
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void acceleratesFurnace(GameTestHelper h) {
        h.setBlock(MACHINE, Blocks.FURNACE);
        FurnaceBlockEntity furnace = h.getBlockEntity(MACHINE);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 16));
        furnace.setItem(1, new ItemStack(Items.COAL, 4));
        OverdriveBlockEntity be = placeDrive(h, 8, 10_000_000);
        // Vanilla needs 200 ticks per item; at 8x two items take ~50 ticks.
        h.runAfterDelay(60, () -> {
            int out = furnace.getItem(2).getCount();
            check(h, be.getEffective() == 8, "should run at 8x, was " + be.getEffective());
            check(h, be.getLastExtraTicks() == 7, "7 extra ticks per tick, was " + be.getLastExtraTicks());
            check(h, out >= 2, "expected >= 2 smelted items after 60 ticks at 8x, got " + out);
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void stepsDownOnLowEnergy(GameTestHelper h) {
        h.setBlock(MACHINE, Blocks.FURNACE);
        // 8x costs 6,400 FE/t; 1,000 FE affords only 2x (400 FE/t)
        OverdriveBlockEntity be = placeDrive(h, 8, 1_000);
        h.runAfterDelay(1, () -> {
            check(h, be.getEffective() == 2, "expected step-down to 2x, was " + be.getEffective());
            check(h, be.getLastCost() == 400, "expected 400 FE/t, was " + be.getLastCost());
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void throttlesWhenHot(GameTestHelper h) {
        h.setBlock(MACHINE, Blocks.FURNACE);
        OverdriveBlockEntity be = placeDrive(h, 8, 10_000_000);
        be.setHeat(75.0);
        h.runAfterDelay(1, () -> {
            check(h, be.getStatus() == HeatStatus.THROTTLING, "expected THROTTLING, was " + be.getStatus());
            check(h, be.getEffective() == 4, "throttle should halve 8x to 4x, was " + be.getEffective());
            check(h, h.getBlockState(DRIVE).getValue(OverdriveBlock.HEAT_STATE) == 2, "block should show the hot texture");
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void neverAcceleratesAnotherOverdrive(GameTestHelper h) {
        h.setBlock(MACHINE, ModRegistry.OVERDRIVE.get().defaultBlockState());
        OverdriveBlockEntity be = placeDrive(h, 8, 10_000_000);
        h.runAfterDelay(2, () -> {
            check(h, be.getTargetStatus() == OverdriveBlockEntity.TARGET_BLOCKED, "target must be blocked");
            check(h, be.getEffective() == 1 && be.getLastCost() == 0, "blocked target must not run or cost energy");
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void chipLimitsMultiplier(GameTestHelper h) {
        h.setBlock(MACHINE, Blocks.FURNACE);
        OverdriveBlockEntity be = placeDrive(h, 64, 10_000_000);
        be.getChipHandler().setStackInSlot(0, new ItemStack(ModRegistry.CHIP_T1.get()));
        h.runAfterDelay(1, () -> {
            check(h, be.getEffective() == 8, "T1 chip caps at 8x, was " + be.getEffective());
            be.getChipHandler().setStackInSlot(0, ItemStack.EMPTY);
            h.runAfterDelay(1, () -> {
                check(h, be.getEffective() == 2, "no chip caps at 2x, was " + be.getEffective());
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ phase 4: fluid cooling

    @GameTest(template = EMPTY)
    public static void coolantFormulas(GameTestHelper h) {
        check(h, OverdriveMath.coolantPerTick(1, 0.02) == 0.0, "1x uses no coolant");
        check(h, Math.abs(OverdriveMath.coolantPerTick(16, 0.02) - 5.12) < 1e-9, "16x water = 5.12 mB/t");
        check(h, Math.abs(OverdriveMath.coolantPerTick(64, 0.0005) - 2.048) < 1e-9, "64x cryo = 2.048 mB/t");
        check(h, OverdriveMath.coolingCapacity(16, 128) == 128, "water beats passive");
        check(h, OverdriveMath.coolingCapacity(16, 0) == 16, "empty tank falls back to passive");
        // spec balance: cryo keeps 32x cool, 64x still lands in the throttle band
        check(h, OverdriveMath.equilibriumHeat(32, 2048, 40) == 20.0, "32x cryo = 20 %");
        check(h, OverdriveMath.equilibriumHeat(64, 2048, 40) == 80.0, "64x cryo = 80 %");
        check(h, OverdriveMath.equilibriumHeat(8, 128, 40) == 20.0, "8x water = 20 %");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tankAcceptsOnlyCoolants(GameTestHelper h) {
        h.setBlock(DRIVE, ModRegistry.OVERDRIVE.get().defaultBlockState());
        OverdriveBlockEntity be = h.getBlockEntity(DRIVE);
        var tank = be.getTank();
        check(h, tank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "lava rejected");
        check(h, tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "water accepted");
        check(h, tank.fill(new FluidStack(ModRegistry.CRYO_SOURCE.get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 0,
                "cannot mix cryo into water");
        tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        check(h, tank.fill(new FluidStack(ModRegistry.CRYO_SOURCE.get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000,
                "cryo accepted once empty");
        var cap = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(DRIVE), Direction.UP);
        check(h, cap != null && cap.getFluidInTank(0).getAmount() == 1000, "fluid capability exposes the tank");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void waterCoolsAndIsConsumed(GameTestHelper h) {
        h.setBlock(MACHINE, Blocks.FURNACE);
        OverdriveBlockEntity be = placeDrive(h, 16, 10_000_000);
        be.getTank().fill(new FluidStack(Fluids.WATER, 16_000), IFluidHandler.FluidAction.EXECUTE);
        h.runAfterDelay(20, () -> {
            check(h, be.getCoolingSource() == CoolingSource.WATER, "expected WATER, was " + be.getCoolingSource());
            check(h, be.getCoolingCapacity() == 128.0, "expected capacity 128, was " + be.getCoolingCapacity());
            int used = 16_000 - be.getTank().getFluidAmount();
            // 16x * 5.12 mB/t for ~20 ticks
            check(h, used >= 90 && used <= 115, "expected ~100 mB used, was " + used);
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void cryoCoolsAndFallsBackWhenEmpty(GameTestHelper h) {
        h.setBlock(MACHINE, Blocks.FURNACE);
        h.setBlock(DRIVE.east(), Blocks.BLUE_ICE);
        h.setBlock(DRIVE.west(), Blocks.BLUE_ICE);
        OverdriveBlockEntity be = placeDrive(h, 32, 10_000_000);
        be.getTank().fill(new FluidStack(ModRegistry.CRYO_SOURCE.get(), 1), IFluidHandler.FluidAction.EXECUTE);
        h.runAfterDelay(1, () -> {
            check(h, be.getCoolingSource() == CoolingSource.CRYO, "expected CRYO, was " + be.getCoolingSource());
            check(h, be.getCoolingCapacity() == 2048.0, "expected 2048, was " + be.getCoolingCapacity());
            // 32x cryo uses 0.512 mB/t, so the single mB is gone after two ticks
            h.runAfterDelay(5, () -> {
                check(h, be.getTank().isEmpty(), "tank should be drained");
                check(h, be.getCoolingSource() == CoolingSource.PASSIVE, "expected PASSIVE fallback, was " + be.getCoolingSource());
                check(h, be.getCoolingCapacity() == 10.0, "two ice blocks = 10, was " + be.getCoolingCapacity());
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ advancements & recipes

    private static final String[] ADVANCEMENTS = {"root", "first_overclock", "chip", "chip_t4", "redline",
            "beyond_the_limit", "water_cooled", "cryo_cooled", "meltdown", "cool_down", "upgrade", "fully_upgraded"};
    /** Recipes with Mekanism > AE2 > vanilla variants. */
    private static final String[] VARIANT_RECIPES = {"overdrive", "overclock_chip_t1", "overclock_chip_t2", "overclock_chip_t3",
            "heat_sink_upgrade", "efficiency_upgrade", "overcharge_upgrade"};

    @GameTest(template = EMPTY)
    public static void advancementsAndRecipesLoad(GameTestHelper h) {
        var server = h.getLevel().getServer();
        for (String name : ADVANCEMENTS) {
            check(h, server.getAdvancements().get(rl(name)) != null, "advancement overdrive:" + name + " did not load");
        }
        boolean atm = ModList.get().isLoaded("allthemodium");
        var recipes = server.getRecipeManager();
        check(h, recipes.byKey(rl("overclock_chip_t4")).isPresent() != atm, "vanilla T4 recipe must load only without Allthemodium");
        check(h, recipes.byKey(rl("overclock_chip_t4_allthemodium")).isPresent() == atm, "Allthemodium T4 recipe must load only with Allthemodium");
        boolean mek = ModList.get().isLoaded("mekanism"), ae2 = ModList.get().isLoaded("ae2");
        for (String r : VARIANT_RECIPES) {
            int loaded = 0;
            if (recipes.byKey(rl(r)).isPresent()) { loaded++; check(h, !mek && !ae2, r + ": vanilla variant loaded next to a mod"); }
            if (recipes.byKey(rl(r + "_mekanism")).isPresent()) { loaded++; check(h, mek, r + "_mekanism loaded without Mekanism"); }
            if (recipes.byKey(rl(r + "_ae2")).isPresent()) { loaded++; check(h, ae2 && !mek, r + "_ae2 loaded wrongly"); }
            check(h, loaded == 1, r + ": expected exactly one variant, got " + loaded);
        }
        for (String r : new String[]{"thermal_paste", "chip_repair", "cryo_coolant_bucket"}) {
            check(h, recipes.byKey(rl(r)).isPresent(), r + " recipe did not load");
        }
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void overclockGrantsAdvancement(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.moveTo(h.absoluteVec(DRIVE.getCenter()).add(0, 1, 0));
        h.setBlock(MACHINE, Blocks.FURNACE);
        placeDrive(h, 16, 10_000_000);
        var server = h.getLevel().getServer();
        var first = server.getAdvancements().get(rl("first_overclock"));
        var redline = server.getAdvancements().get(rl("redline"));
        var limit = server.getAdvancements().get(rl("beyond_the_limit"));
        h.succeedWhen(() -> {
            check(h, player.getAdvancements().getOrStartProgress(first).isDone(), "first_overclock not granted");
            check(h, player.getAdvancements().getOrStartProgress(redline).isDone(), "redline not granted");
            check(h, !player.getAdvancements().getOrStartProgress(limit).isDone(), "beyond_the_limit must need 64x");
        });
    }

    // ------------------------------------------------------------------ upgrades, redstone, paste

    @GameTest(template = EMPTY)
    public static void upgradeMath(GameTestHelper h) {
        check(h, OverdriveMath.upgradeFactor(1.5, 0) == 1.0, "no cards = x1");
        check(h, OverdriveMath.upgradeFactor(1.5, 2) == 2.25, "two heat sinks = x2.25");
        check(h, OverdriveMath.upgradedCost(1, 100, 0.75) == 0, "1x stays free");
        check(h, OverdriveMath.upgradedCost(16, 100, 0.75) == 19_200, "16x efficient = 19,200 FE/t");
        check(h, OverdriveMath.powerLimited(64, 307_200, 100, 0.75) == 64, "efficiency lets a smaller buffer run 64x");
        check(h, OverdriveMath.overchargedLimit(32, 1, 64) == 64, "T3 + overcharge = 64x");
        check(h, OverdriveMath.overchargedLimit(64, 2, 64) == 64, "global max still applies");
        check(h, OverdriveMath.overchargedLimit(2, 2, 64) == 8, "no chip + 2 overcharge = 8x");
        check(h, OverdriveMath.comparatorLevel(0) == 0 && OverdriveMath.comparatorLevel(100) == 15, "comparator range");
        check(h, OverdriveMath.comparatorLevel(50) == 8, "50 % heat -> 8");
        check(h, OverdriveMath.repairedDamage(1000, 2000, 1, 0.25) == 500, "one paste restores 25 %");
        check(h, OverdriveMath.repairedDamage(300, 2000, 4, 0.25) == 0, "repair never goes below 0 damage");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void upgradesApply(GameTestHelper h) {
        h.setBlock(MACHINE, Blocks.FURNACE);
        OverdriveBlockEntity be = placeDrive(h, 2, 10_000_000);
        be.getChipHandler().setStackInSlot(0, new ItemStack(ModRegistry.CHIP_T3.get()));
        check(h, be.getMultiplierLimit() == 32, "T3 limit is 32x");
        be.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModRegistry.OVERCHARGE_UPGRADE.get()));
        check(h, be.getMultiplierLimit() == 64, "T3 + Overcharge should reach 64x, was " + be.getMultiplierLimit());
        check(h, !be.getUpgradeHandler().isItemValid(1, new ItemStack(Items.DIAMOND)), "upgrade slots only take upgrades");
        be.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModRegistry.HEAT_SINK_UPGRADE.get()));
        be.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModRegistry.EFFICIENCY_UPGRADE.get()));
        h.runAfterDelay(2, () -> {
            check(h, be.getCoolingCapacity() == 6.0, "heat sink: 4 x 1.5 = 6, was " + be.getCoolingCapacity());
            check(h, be.getLastCost() == 300, "efficiency: 2x costs 400 x 0.75 = 300, was " + be.getLastCost());
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void redstoneGatesOverdrive(GameTestHelper h) {
        h.setBlock(MACHINE, Blocks.FURNACE);
        OverdriveBlockEntity be = placeDrive(h, 4, 10_000_000);
        be.setRedstoneMode(RedstoneMode.ACTIVE_HIGH);
        h.runAfterDelay(6, () -> {
            check(h, be.isRedstoneBlocked() && be.getEffective() == 1, "ACTIVE_HIGH without signal must idle at 1x");
            h.setBlock(DRIVE.east(), Blocks.REDSTONE_BLOCK);
            h.runAfterDelay(6, () -> {
                check(h, !be.isRedstoneBlocked() && be.getEffective() == 4, "signal should enable 4x, was " + be.getEffective());
                be.setRedstoneMode(RedstoneMode.ACTIVE_LOW);
                h.runAfterDelay(6, () -> {
                    check(h, be.isRedstoneBlocked() && be.getEffective() == 1, "ACTIVE_LOW with signal must idle");
                    h.succeed();
                });
            });
        });
    }

    @GameTest(template = EMPTY)
    public static void comparatorAndThermalPaste(GameTestHelper h) {
        OverdriveBlockEntity be = placeDrive(h, 1, 0);
        check(h, !be.applyThermalPaste(), "paste on a cold Overdrive does nothing");
        be.setHeat(80.0);
        var state = h.getBlockState(DRIVE);
        check(h, state.getAnalogOutputSignal(h.getLevel(), h.absolutePos(DRIVE)) == 12, "80 % heat -> comparator 12");
        check(h, be.applyThermalPaste(), "paste should apply");
        check(h, Math.abs(be.getHeat() - 50.0) < 1e-9, "paste removes 30 %, heat was " + be.getHeat());
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void chipRepairRecipe(GameTestHelper h) {
        ItemStack chip = new ItemStack(ModRegistry.CHIP_T1.get());
        chip.setDamageValue(15_000);
        ItemStack paste = new ItemStack(ModRegistry.THERMAL_PASTE.get());
        CraftingInput input = CraftingInput.of(2, 2, List.of(chip, paste, paste, ItemStack.EMPTY));
        var level = h.getLevel();
        var match = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        check(h, match.isPresent() && match.get().id().equals(rl("chip_repair")), "chip + paste should match chip_repair");
        ItemStack out = match.get().value().assemble(input, level.registryAccess());
        check(h, out.getDamageValue() == 5_000, "two pastes restore 10,000 of 20,000, damage was " + out.getDamageValue());
        CraftingInput undamaged = CraftingInput.of(2, 1, List.of(new ItemStack(ModRegistry.CHIP_T1.get()), paste));
        check(h, level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, undamaged, level).isEmpty(), "undamaged chip must not match");
        h.succeed();
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Overdrive.MODID, path);
    }

    private static void check(GameTestHelper h, boolean condition, String message) {
        if (!condition) h.fail(message);
    }
}
