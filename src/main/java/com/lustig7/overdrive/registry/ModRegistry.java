package com.lustig7.overdrive.registry;

import com.lustig7.overdrive.Overdrive;
import com.lustig7.overdrive.block.OverdriveBlock;
import com.lustig7.overdrive.block.OverdriveBlockEntity;
import com.lustig7.overdrive.item.OverclockChipItem;
import com.lustig7.overdrive.item.ThermalPasteItem;
import com.lustig7.overdrive.item.UpgradeItem;
import com.lustig7.overdrive.item.UpgradeType;
import com.lustig7.overdrive.recipe.ChipRepairRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import com.lustig7.overdrive.menu.OverdriveMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Overdrive.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Overdrive.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Overdrive.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Overdrive.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Overdrive.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Overdrive.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Overdrive.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Overdrive.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Overdrive.MODID);

    public static final DeferredBlock<OverdriveBlock> OVERDRIVE = BLOCKS.register("overdrive", () -> new OverdriveBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(s -> switch (s.getValue(OverdriveBlock.HEAT_STATE)) {
                        case 1 -> 6;
                        case 2 -> 9;
                        case 3 -> 12;
                        default -> 0;
                    })));
    public static final DeferredItem<BlockItem> OVERDRIVE_ITEM = ITEMS.registerSimpleBlockItem(OVERDRIVE);

    public static final DeferredItem<OverclockChipItem> CHIP_T1 = chip(1, 8, 20_000);
    public static final DeferredItem<OverclockChipItem> CHIP_T2 = chip(2, 16, 40_000);
    public static final DeferredItem<OverclockChipItem> CHIP_T3 = chip(3, 32, 80_000);
    public static final DeferredItem<OverclockChipItem> CHIP_T4 = chip(4, 64, 160_000);

    public static final DeferredItem<UpgradeItem> HEAT_SINK_UPGRADE = upgrade(UpgradeType.HEAT_SINK);
    public static final DeferredItem<UpgradeItem> EFFICIENCY_UPGRADE = upgrade(UpgradeType.EFFICIENCY);
    public static final DeferredItem<UpgradeItem> OVERCHARGE_UPGRADE = upgrade(UpgradeType.OVERCHARGE);
    public static final DeferredItem<ThermalPasteItem> THERMAL_PASTE = ITEMS.register("thermal_paste",
            () -> new ThermalPasteItem(new Item.Properties()));

    // ---- sounds (sounds.json points them at vanilla sound files; resource packs can replace them)
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_HUM = sound("block.overdrive.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_ALARM = sound("block.overdrive.alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_HISS = sound("block.overdrive.hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_CHIP_BREAK = sound("block.overdrive.chip_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_SHUTDOWN = sound("block.overdrive.shutdown");

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<ChipRepairRecipe>> CHIP_REPAIR =
            RECIPE_SERIALIZERS.register("chip_repair", () -> new SimpleCraftingRecipeSerializer<>(ChipRepairRecipe::new));

    // ---- Cryo Coolant: the high-end coolant (cooling 2048 by default)
    public static final DeferredHolder<FluidType, FluidType> CRYO_TYPE = FLUID_TYPES.register("cryo_coolant", () -> new FluidType(
            FluidType.Properties.create()
                    .descriptionId("block.overdrive.cryo_coolant")
                    .temperature(70)
                    .density(1200)
                    .viscosity(1500)
                    .canExtinguish(true)
                    .supportsBoating(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CRYO_SOURCE =
            FLUIDS.register("cryo_coolant", () -> new BaseFlowingFluid.Source(cryoProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> CRYO_FLOWING =
            FLUIDS.register("flowing_cryo_coolant", () -> new BaseFlowingFluid.Flowing(cryoProperties()));
    public static final DeferredBlock<LiquidBlock> CRYO_BLOCK = BLOCKS.register("cryo_coolant",
            () -> new LiquidBlock(CRYO_SOURCE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.ICE)));
    public static final DeferredItem<BucketItem> CRYO_BUCKET = ITEMS.register("cryo_coolant_bucket",
            () -> new BucketItem(CRYO_SOURCE.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OverdriveBlockEntity>> OVERDRIVE_BE =
            BLOCK_ENTITIES.register("overdrive", () -> BlockEntityType.Builder.of(OverdriveBlockEntity::new, OVERDRIVE.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<OverdriveMenu>> OVERDRIVE_MENU =
            MENUS.register("overdrive", () -> IMenuTypeExtension.create(OverdriveMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.overdrive"))
            .icon(() -> OVERDRIVE_ITEM.get().getDefaultInstance())
            .displayItems((params, out) -> {
                out.accept(OVERDRIVE_ITEM.get());
                out.accept(CHIP_T1.get());
                out.accept(CHIP_T2.get());
                out.accept(CHIP_T3.get());
                out.accept(CHIP_T4.get());
                out.accept(HEAT_SINK_UPGRADE.get());
                out.accept(EFFICIENCY_UPGRADE.get());
                out.accept(OVERCHARGE_UPGRADE.get());
                out.accept(THERMAL_PASTE.get());
                out.accept(CRYO_BUCKET.get());
            })
            .build());

    private ModRegistry() {}

    private static BaseFlowingFluid.Properties cryoProperties() {
        return new BaseFlowingFluid.Properties(CRYO_TYPE, CRYO_SOURCE, CRYO_FLOWING)
                .bucket(CRYO_BUCKET)
                .block(CRYO_BLOCK)
                .slopeFindDistance(3)
                .levelDecreasePerBlock(2)
                .tickRate(10);
    }

    private static DeferredItem<OverclockChipItem> chip(int tier, int maxMultiplier, int durability) {
        return ITEMS.register("overclock_chip_t" + tier,
                () -> new OverclockChipItem(tier, maxMultiplier, new Item.Properties().durability(durability)));
    }

    private static DeferredItem<UpgradeItem> upgrade(UpgradeType type) {
        return ITEMS.register(type.id() + "_upgrade", () -> new UpgradeItem(type, new Item.Properties()));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Overdrive.MODID, name)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
    }
}
