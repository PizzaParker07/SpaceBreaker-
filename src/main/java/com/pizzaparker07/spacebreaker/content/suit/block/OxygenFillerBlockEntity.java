package com.pizzaparker07.spacebreaker.content.suit.block;

import com.pizzaparker07.spacebreaker.content.suit.ManeuveringUnitItem;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Compresses breathable air into oxygen tanks (and nitrogen into Maneuvering Units). Needs FE and
 * breathable air around it: on Earth anywhere, on the Moon only inside a pressurised base.
 */
public class OxygenFillerBlockEntity extends GasMachineBlockEntity {
    /** Breathing ticks pumped per game tick: a small tank fills in 15 seconds. */
    public static final int FILL_RATE = 20;
    public static final int FE_PER_TICK = 8;

    public OxygenFillerBlockEntity(BlockPos pos, BlockState state) {
        super(SuitModule.OXYGEN_FILLER_BE.get(), pos, state, 16000);
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return stack.getItem() instanceof OxygenTankItem || stack.getItem() instanceof ManeuveringUnitItem;
    }

    public static boolean hasAir(Level level, BlockPos pos) {
        return Environment.canBreathe(level, pos.above()) || Environment.canBreathe(level, pos);
    }

    /** Fills a stack by one tick's worth; returns true when something was pumped. */
    public static boolean pump(ItemStack stack) {
        if (stack.getItem() instanceof OxygenTankItem) {
            return OxygenTankItem.fill(stack, FILL_RATE) > 0;
        }
        if (stack.getItem() instanceof ManeuveringUnitItem) {
            return ManeuveringUnitItem.fill(stack, FILL_RATE / 2) > 0;
        }
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OxygenFillerBlockEntity be) {
        boolean working = false;
        ItemStack stack = be.getStack();
        if (!stack.isEmpty() && (level.getGameTime() + pos.asLong()) % 20 == 0) {
            be.airCached = hasAir(level, pos);
        }
        if (!stack.isEmpty() && be.airCached && be.energy.getEnergyStored() >= FE_PER_TICK) {
            ItemStack copy = stack.copy();
            if (pump(copy)) {
                be.energy.consume(FE_PER_TICK);
                be.items.setStackInSlot(0, copy);
                working = true;
            }
        }
        if (state.getValue(OxygenFillerBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(OxygenFillerBlock.LIT, working), Block.UPDATE_CLIENTS);
        }
    }

    private boolean airCached = true;
}
