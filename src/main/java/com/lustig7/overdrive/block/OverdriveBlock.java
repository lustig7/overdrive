package com.lustig7.overdrive.block;

import com.lustig7.overdrive.item.ThermalPasteItem;
import com.lustig7.overdrive.logic.CoolingSource;
import com.lustig7.overdrive.registry.ModRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Overdrive block. FACING points at the target machine (the back face / connector plate);
 * the glowing front faces away from it. HEAT_STATE (0-3) drives the texture and light level.
 */
public class OverdriveBlock extends BaseEntityBlock {
    public static final MapCodec<OverdriveBlock> CODEC = simpleCodec(OverdriveBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty HEAT_STATE = IntegerProperty.create("heat_state", 0, 3);

    public OverdriveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HEAT_STATE, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HEAT_STATE);
    }

    /** Placed against a machine, the back faces that machine. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OverdriveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? createTickerHelper(type, ModRegistry.OVERDRIVE_BE.get(), OverdriveBlockEntity::clientTick)
                : createTickerHelper(type, ModRegistry.OVERDRIVE_BE.get(), OverdriveBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof OverdriveBlockEntity be) {
            sp.openMenu(be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Buckets and other fluid containers fill (or empty) the coolant tank. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ThermalPasteItem) {
            if (!(level.getBlockEntity(pos) instanceof OverdriveBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!be.applyThermalPaste()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            stack.consume(1, player);
            level.playSound(null, pos, ModRegistry.SOUND_HISS.get(), SoundSource.BLOCKS, 1.0F, 1.2F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.02);
            }
            return ItemInteractionResult.CONSUME;
        }
        if (FluidUtil.getFluidHandler(stack).isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        return FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())
                ? ItemInteractionResult.CONSUME
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof OverdriveBlockEntity be) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.getChipHandler().getStackInSlot(0));
            be.getChipHandler().setStackInSlot(0, ItemStack.EMPTY);
            for (int i = 0; i < OverdriveBlockEntity.UPGRADE_SLOTS; i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.getUpgradeHandler().getStackInSlot(i));
                be.getUpgradeHandler().setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparator output: heat 0-100 % mapped to 0-15. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof OverdriveBlockEntity be ? be.getComparatorLevel() : 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int heat = state.getValue(HEAT_STATE);
        double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
        double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
        double y = pos.getY() + 1.02;
        if (level.getBlockEntity(pos) instanceof OverdriveBlockEntity be && be.getClientEffective() > 1) {
            // coolant at work: steam from water, frost from cryo
            CoolingSource c = be.getClientCooling();
            if (c == CoolingSource.WATER && random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.CLOUD, x, y, z, 0.0, 0.03, 0.0);
            } else if (c == CoolingSource.CRYO && random.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.0, 0.02, 0.0);
            }
        }
        if (heat < 2) return;
        if (random.nextInt(heat == 3 ? 1 : 3) == 0) {
            level.addParticle(heat == 3 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, x, y, z, 0.0, 0.04, 0.0);
        }
        if (heat == 3 && random.nextInt(2) == 0) {
            Direction d = Direction.getRandom(random);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5 + d.getStepX() * 0.55, pos.getY() + 0.5 + d.getStepY() * 0.55, pos.getZ() + 0.5 + d.getStepZ() * 0.55,
                    (random.nextDouble() - 0.5) * 0.2, 0.05, (random.nextDouble() - 0.5) * 0.2);
        }
    }
}
