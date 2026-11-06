package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Battery bank; CHARGE (0-4) lights the charge bars on its front. */
public class BatteryBankBlock extends MachineBlock {
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 4);

    public BatteryBankBlock(Properties properties) {
        super(properties, MachinesModule.BATTERY_BANK_BE, BatteryBankBlockEntity::new, null, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CHARGE);
    }
}
