package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.RtgFuelPelletItem;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Radioisotope thermoelectric generator: thermocouples around a shielded pellet make a small, steady output day and
 * night, in vacuum or in the dark of the outer system. Output follows the pellet's activity, so it fades slowly over
 * in-game weeks. One pellet is the heat source; extra pellets in the slot only wait their turn. The casing is
 * shielded: a pellet inside gives no dose.
 */
public class RtgBlockEntity extends MachineBlockEntity {
    /** FE/t from a fresh pellet. */
    public static final int OUTPUT = 40;
    public static final MachineLayout LAYOUT = new MachineLayout()
            .energy(10, 17).slot(79, 52, MachineLayout.Role.FUEL).progress(80, 33, MachineLayout.Progress.FLAME);

    public RtgBlockEntity(BlockPos pos, BlockState state) {
        super(MachinesModule.RTG.type().get(), pos, state, LAYOUT, 20_000, 0, 400, List.of());
    }

    /** Output in FE/t for a pellet with this activity. */
    public static int output(double activity) {
        return (int) Math.round(OUTPUT * activity);
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof RtgFuelPelletItem;
    }

    @Override
    protected boolean tickServer(Level level, BlockPos pos, BlockState state) {
        ItemStack pellet = items.getStackInSlot(0);
        int out = 0;
        maxProgress = 1000;
        if (pellet.isEmpty()) {
            status = 5;
            progress = 0;
        } else {
            RtgFuelPelletItem.stamp(pellet, level);
            double a = RtgFuelPelletItem.activity(pellet, level.getGameTime());
            progress = (int) Math.round(a * 1000);
            out = output(a);
            status = 0;
            if (out > 0) {
                energy.generate(out);
            }
        }
        pushEnergy(level, pos, List.of(Direction.values()), 400);
        return out > 0;
    }
}
