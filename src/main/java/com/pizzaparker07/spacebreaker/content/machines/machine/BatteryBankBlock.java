package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Battery bank; CHARGE (0-4) lights the charge bars on its front. Mounted on a ceiling (placed against the underside
 * of a block) its front, the output, faces down: a bank in a habitat roof charges from the solar array above and
 * powers the room below.
 */
public class BatteryBankBlock extends MachineBlock {
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 4);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty OUTPUT_DOWN =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("output_down");

    public BatteryBankBlock(Properties properties) {
        super(properties, MachinesModule.BATTERY_BANK_BE, BatteryBankBlockEntity::new, null, 0);
        registerDefaultState(defaultBlockState().setValue(OUTPUT_DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CHARGE, OUTPUT_DOWN);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        return state == null ? null : state.setValue(OUTPUT_DOWN, ctx.getClickedFace() == net.minecraft.core.Direction.DOWN);
    }

    /** The side energy leaves through. */
    public static net.minecraft.core.Direction front(BlockState state) {
        return state.hasProperty(OUTPUT_DOWN) && state.getValue(OUTPUT_DOWN) ? net.minecraft.core.Direction.DOWN : state.getValue(FACING);
    }
}
