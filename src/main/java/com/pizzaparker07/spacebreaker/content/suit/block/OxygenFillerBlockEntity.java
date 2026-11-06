package com.pizzaparker07.spacebreaker.content.suit.block;

import com.pizzaparker07.spacebreaker.content.suit.ManeuveringUnitItem;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fills oxygen tanks (and Maneuvering Units). Needs FE. Where the planet's own air is breathable (Earth) it
 * compresses that air; everywhere else it needs oxygen piped in (gas or liquid oxygen from ISRU machines).
 * The air of a room pressurised by an Oxygen Distributor does NOT count: that oxygen came out of a tank.
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

    /** The planet's own atmosphere is breathable here (sealed rooms do not count). */
    public static boolean naturalAir(Level level) {
        return naturalAir(Environment.body(level), Bodies.isOrbit(level));
    }

    /** Natural air for a body (no body = Earth-like dimension such as the Nether: breathable). */
    public static boolean naturalAir(java.util.Optional<com.pizzaparker07.spacebreaker.core.body.CelestialBody> body, boolean orbit) {
        return body.map(b -> b.atmosphere().breathable() && !orbit).orElse(true);
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

    /**
     * One tick of filling. Tanks get oxygen for free only from natural breathable air; otherwise it comes out of
     * the piped-in buffer. Maneuvering Units take compressed gas from the same sources. Returns true when it pumped.
     */
    public boolean pumpTick(boolean naturalAir) {
        ItemStack stack = getStack();
        if (stack.isEmpty() || energy.getEnergyStored() < FE_PER_TICK) {
            return false;
        }
        boolean oxygenItem = stack.getItem() instanceof OxygenTankItem;
        int available = naturalAir ? FILL_RATE : Math.min(FILL_RATE, storedOxygen);
        if (available <= 0) {
            return false;
        }
        ItemStack copy = stack.copy();
        int pumped = oxygenItem ? OxygenTankItem.fill(copy, available) : ManeuveringUnitItem.fill(copy, available / 2);
        if (pumped <= 0) {
            return false;
        }
        if (!naturalAir) {
            takeStored(oxygenItem ? pumped : pumped * 2);
        }
        energy.consume(FE_PER_TICK);
        items.setStackInSlot(0, copy);
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OxygenFillerBlockEntity be) {
        boolean working = be.pumpTick(naturalAir(level));
        if (state.getValue(OxygenFillerBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(OxygenFillerBlock.LIT, working), Block.UPDATE_CLIENTS);
        }
    }

}
