package com.pizzaparker07.spacebreaker.content.machines.net;

import com.mojang.serialization.MapCodec;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Cable or pipe. Connects to conduits of the same type and to any block that offers energy (cables) or fluid
 * (pipes) on the touching face. Sneak-use with an empty hand on a pipe arm toggles extraction from that block.
 */
public class ConduitBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final Map<Direction, EnumProperty<ConduitSide>> SIDES = new EnumMap<>(Direction.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    static {
        for (Direction d : Direction.values()) {
            SIDES.put(d, EnumProperty.create(d.getSerializedName(), ConduitSide.class));
        }
    }

    public final ConduitType type;
    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();

    public ConduitBlock(ConduitType type, Properties properties) {
        super(properties);
        this.type = type;
        BlockState s = stateDefinition.any().setValue(WATERLOGGED, false);
        for (EnumProperty<ConduitSide> p : SIDES.values()) {
            s = s.setValue(p, ConduitSide.NONE);
        }
        registerDefaultState(s);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
        builder.add(WATERLOGGED);
    }

    public boolean hasExtract(BlockState state) {
        for (EnumProperty<ConduitSide> p : SIDES.values()) {
            if (state.getValue(p) == ConduitSide.EXTRACT) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ connections

    private boolean canConnect(LevelAccessor access, BlockPos pos, Direction dir) {
        BlockPos np = pos.relative(dir);
        BlockState ns = access.getBlockState(np);
        if (ns.getBlock() instanceof ConduitBlock other) {
            return other.type == type;
        }
        if (!(access instanceof Level level)) {
            return false;
        }
        if (type.energy) {
            return level.getCapability(Capabilities.EnergyStorage.BLOCK, np, ns, null, dir.getOpposite()) != null;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, np, ns, null, dir.getOpposite()) != null
                || (type.accepts(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1))
                && ConduitNetwork.isWaterWell(level, np));
    }

    private BlockState withSide(BlockState state, LevelAccessor level, BlockPos pos, Direction dir) {
        ConduitSide old = state.getValue(SIDES.get(dir));
        boolean connect = canConnect(level, pos, dir);
        ConduitSide side = !connect ? ConduitSide.NONE
                : (old == ConduitSide.EXTRACT && !(level.getBlockState(pos.relative(dir)).getBlock() instanceof ConduitBlock) ? ConduitSide.EXTRACT : ConduitSide.CONNECTED);
        return state.setValue(SIDES.get(dir), side);
    }

    private BlockState computeAll(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            state = withSide(state, level, pos, d);
        }
        return state;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState().setValue(WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == Fluids.WATER);
        return computeAll(s, ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return withSide(state, level, pos, dir);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        BlockState updated = computeAll(state, level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
        invalidate(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        invalidate(level, pos);
        for (Direction d : Direction.values()) {
            invalidate(level, pos.relative(d));
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        invalidate(level, pos);
        super.onRemove(state, level, pos, newState, moving);
        for (Direction d : Direction.values()) {
            invalidate(level, pos.relative(d));
        }
    }

    private static void invalidate(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ConduitBlockEntity be) {
            be.invalidateNetwork();
        }
    }

    // ------------------------------------------------------------------ extraction toggle

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (type.energy || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Direction dir = armAt(hit.getLocation().subtract(Vec3.atLowerCornerWithOffset(pos, 0, 0, 0)), hit.getDirection());
        ConduitSide side = state.getValue(SIDES.get(dir));
        if (!side.connected() || level.getBlockState(pos.relative(dir)).getBlock() instanceof ConduitBlock) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ConduitSide next = side == ConduitSide.EXTRACT ? ConduitSide.CONNECTED : ConduitSide.EXTRACT;
            level.setBlock(pos, state.setValue(SIDES.get(dir), next), Block.UPDATE_ALL);
            invalidate(level, pos);
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5F, next == ConduitSide.EXTRACT ? 1.4F : 0.9F);
            player.displayClientMessage(Component.translatable(next == ConduitSide.EXTRACT
                    ? "message.spacebreaker.pipe_extract_on" : "message.spacebreaker.pipe_extract_off"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Which arm a hit (block-local coordinates) lands on; the core counts as the clicked face. */
    private Direction armAt(Vec3 local, Direction face) {
        double r = type.radius / 16.0;
        double dx = local.x - 0.5, dy = local.y - 0.5, dz = local.z - 0.5;
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ax <= r + 1e-3 && ay <= r + 1e-3 && az <= r + 1e-3) {
            return face;
        }
        if (ax >= ay && ax >= az) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        if (ay >= az) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable(type.energy ? "tooltip.spacebreaker.cable" : "tooltip.spacebreaker.pipe", type.rate)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    // ------------------------------------------------------------------ shape, fluid, entity

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.computeIfAbsent(state, this::makeShape);
    }

    private VoxelShape makeShape(BlockState state) {
        double lo = (8 - type.radius) / 16.0, hi = (8 + type.radius) / 16.0;
        VoxelShape shape = Shapes.box(lo, lo, lo, hi, hi, hi);
        for (Direction d : Direction.values()) {
            if (!state.getValue(SIDES.get(d)).connected()) {
                continue;
            }
            double x0 = d.getStepX() < 0 ? 0 : (d.getStepX() > 0 ? hi : lo);
            double x1 = d.getStepX() < 0 ? lo : (d.getStepX() > 0 ? 1 : hi);
            double y0 = d.getStepY() < 0 ? 0 : (d.getStepY() > 0 ? hi : lo);
            double y1 = d.getStepY() < 0 ? lo : (d.getStepY() > 0 ? 1 : hi);
            double z0 = d.getStepZ() < 0 ? 0 : (d.getStepZ() > 0 ? hi : lo);
            double z1 = d.getStepZ() < 0 ? lo : (d.getStepZ() > 0 ? 1 : hi);
            shape = Shapes.or(shape, Shapes.box(x0, y0, z0, x1, y1, z1));
        }
        return shape;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConduitBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        return level.isClientSide || type.energy ? null : createTickerHelper(beType, MachinesModule.CONDUIT_BE.get(), ConduitBlockEntity::serverTick);
    }
}
