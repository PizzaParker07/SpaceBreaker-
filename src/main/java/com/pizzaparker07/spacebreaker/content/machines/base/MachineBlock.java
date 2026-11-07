package com.pizzaparker07.spacebreaker.content.machines.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A machine block: faces the player when placed, glows while working (LIT), opens its screen on use and
 * fills/drains with buckets and canisters.
 */
public class MachineBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type;
    private final BiFunction<BlockPos, BlockState, ? extends MachineBlockEntity> factory;
    private final VoxelShape shape;
    private final int litLight;

    public MachineBlock(Properties properties, Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type,
                        BiFunction<BlockPos, BlockState, ? extends MachineBlockEntity> factory, @Nullable VoxelShape shape, int litLight) {
        super(properties.lightLevel(s -> s.hasProperty(LIT) && s.getValue(LIT) ? litLight : 0));
        this.type = type;
        this.factory = factory;
        this.shape = shape == null ? Shapes.block() : shape;
        this.litLight = litLight;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        throw new UnsupportedOperationException("machines are not data-driven blocks");
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            be.setOwner(player.getUUID());
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return rotateShape(shape, state.getValue(FACING));
    }

    /** Rotates a shape drawn facing north to the given facing. */
    public static VoxelShape rotateShape(VoxelShape shape, Direction facing) {
        if (facing == Direction.NORTH) {
            return shape;
        }
        VoxelShape[] out = {Shapes.empty()};
        int turns = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        shape.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
            double ax0 = x0, az0 = z0, ax1 = x1, az1 = z1;
            for (int i = 0; i < turns; i++) {
                double nx0 = 1 - az1, nz0 = ax0, nx1 = 1 - az0, nz1 = ax1;
                ax0 = nx0;
                az0 = nz0;
                ax1 = nx1;
                az1 = nz1;
            }
            out[0] = Shapes.or(out[0], Shapes.box(ax0, y0, az0, ax1, y1, az1));
        });
        return out[0];
    }

    /** Set by the client: starts a machine's loop sound at a position (a no-op on servers). */
    public static java.util.function.BiConsumer<Level, BlockPos> CLIENT_LOOP = (l, p) -> { };

    /** The sound this machine makes while it works, or null for a quiet machine. */
    public @Nullable net.minecraft.sounds.SoundEvent loopSound() {
        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this).getPath();
        return switch (id) {
            case "electrolyzer" -> com.pizzaparker07.spacebreaker.content.audio.SbSounds.ELECTROLYZER_LOOP.get();
            case "crusher" -> com.pizzaparker07.spacebreaker.content.audio.SbSounds.DRILL_LOOP.get();
            default -> null;
        };
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        if (level.isClientSide) {
            // only for machines with a working sound: tells the client sound manager every second that it is running
            return loopSound() != null && beType == type.get() ? (l, p, s, be) -> {
                if (s.getValue(LIT) && (l.getGameTime() + p.hashCode()) % 20 == 0) {
                    CLIENT_LOOP.accept(l, p);
                }
            } : null;
        }
        return beType == type.get() ? (l, p, s, be) -> MachineBlockEntity.serverTick(l, p, s, (MachineBlockEntity) be) : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity be && !be.tanks().isEmpty()
                && FluidUtil.getFluidHandler(stack).isPresent()) {
            if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }

    public static InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            sp.openMenu(be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            be.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        if (this == com.pizzaparker07.spacebreaker.content.machines.MachinesModule.COMBUSTION_GENERATOR.block().get()) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.spacebreaker.generator",
                    com.pizzaparker07.spacebreaker.content.machines.machine.CombustionGeneratorBlockEntity.OUTPUT)
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        if (this == com.pizzaparker07.spacebreaker.content.machines.MachinesModule.RTG.block().get()) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.spacebreaker.rtg",
                    com.pizzaparker07.spacebreaker.content.machines.machine.RtgBlockEntity.OUTPUT).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this).getPath();
        if (java.util.Set.of("atmosphere_processor", "sabatier_reactor", "regolith_heater", "ice_melter", "chemical_reactor").contains(id)) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.spacebreaker." + id).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        if (this == com.pizzaparker07.spacebreaker.content.machines.MachinesModule.ELECTROLYZER.block().get()) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.spacebreaker.electrolyzer")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            return Math.round(be.energy().fraction() * 15);
        }
        return 0;
    }
}
