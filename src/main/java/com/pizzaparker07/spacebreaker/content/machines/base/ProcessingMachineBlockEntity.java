package com.pizzaparker07.spacebreaker.content.machines.base;

import com.pizzaparker07.spacebreaker.content.machines.recipe.MachineInput;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A machine that runs {@link ProcessingRecipe}s: picks the recipe matching its input slots / tool slot /
 * input tank, draws FE every tick while working and delivers items to output slots and fluids to output tanks.
 */
public class ProcessingMachineBlockEntity extends MachineBlockEntity {
    protected final MachineSpec spec;
    @Nullable protected ProcessingRecipe current;

    public ProcessingMachineBlockEntity(MachineSpec spec, BlockPos pos, BlockState state) {
        super(spec.type().get(), pos, state, spec.layout(), spec.energyCapacity(), spec.maxReceive(), 0, spec.tanks());
        this.spec = spec;
    }

    public @Nullable ProcessingRecipe currentRecipe() {
        return current;
    }

    protected MachineInput input() {
        List<ItemStack> in = new ArrayList<>();
        for (int s : slots(MachineLayout.Role.INPUT)) {
            in.add(items.getStackInSlot(s));
        }
        int toolSlot = slotIndex(MachineLayout.Role.TOOL, 0);
        ItemStack tool = toolSlot >= 0 ? items.getStackInSlot(toolSlot) : ItemStack.EMPTY;
        FluidStack fluid = FluidStack.EMPTY;
        for (MachineTank t : tanks) {
            if (t.input) {
                fluid = t.getFluid();
                break;
            }
        }
        return new MachineInput(in, tool, fluid);
    }

    protected @Nullable ProcessingRecipe findRecipe(Level level, MachineInput input) {
        if (input.isEmpty()) {
            return null;
        }
        Optional<RecipeHolder<ProcessingRecipe>> found = level.getRecipeManager().getRecipeFor(spec.recipe().get().type().get(), input, level);
        return found.map(RecipeHolder::value).orElse(null);
    }

    /** Extra condition checked every tick (e.g. a multiblock being complete). Return a status code or 0. */
    protected int extraCheck(Level level, BlockPos pos) {
        return 0;
    }

    @Override
    protected boolean tickServer(Level level, BlockPos pos, BlockState state) {
        if (consumeContentsChanged()) {
            ProcessingRecipe found = findRecipe(level, input());
            if (found != current) {
                current = found;
                progress = 0;
            }
        }
        if (!tanks.isEmpty() && level.getGameTime() % 2 == 0) {
            pushFluids(level, pos, 200);
        }
        if (current == null) {
            progress = 0;
            maxProgress = 0;
            status = 0;
            return false;
        }
        ProcessingRecipe r = current;
        maxProgress = r.time;
        int extra = extraCheck(level, pos);
        if (extra != 0) {
            status = extra;
            return false;
        }
        if (!canOutput(r)) {
            status = 2;
            return false;
        }
        if (!energy.consume(r.energy)) {
            status = 1;
            return false;
        }
        status = 0;
        if (++progress >= r.time) {
            craft(level, r);
            progress = 0;
            markContentsChanged();
        }
        return true;
    }

    protected boolean canOutput(ProcessingRecipe r) {
        List<Integer> outs = slots(MachineLayout.Role.OUTPUT);
        for (int k = 0; k < r.results.size(); k++) {
            if (k >= outs.size()) {
                return false;
            }
            ItemStack rest = items.insertItem(outs.get(k), r.results.get(k).copy(), true);
            if (!rest.isEmpty()) {
                return false;
            }
        }
        int t = 0;
        for (FluidStack f : r.fluidResults) {
            MachineTank tank = outputTank(t++);
            if (tank == null || tank.fillInternal(f.copy(), IFluidHandler.FluidAction.SIMULATE) < f.getAmount()) {
                return false;
            }
        }
        return true;
    }

    protected @Nullable MachineTank outputTank(int n) {
        int k = 0;
        for (MachineTank tank : tanks) {
            if (!tank.input && k++ == n) {
                return tank;
            }
        }
        return null;
    }

    protected void craft(Level level, ProcessingRecipe r) {
        MachineInput in = input();
        int[] assignment = r.assign(in.items());
        List<Integer> inSlots = slots(MachineLayout.Role.INPUT);
        if (assignment != null) {
            for (int i = 0; i < assignment.length; i++) {
                items.extractItem(inSlots.get(assignment[i]), r.ingredients.get(i).count(), false);
            }
        }
        r.fluidInput.ifPresent(f -> {
            for (MachineTank t : tanks) {
                if (t.input) {
                    t.drain(f.amount(), IFluidHandler.FluidAction.EXECUTE);
                    break;
                }
            }
        });
        List<Integer> outs = slots(MachineLayout.Role.OUTPUT);
        for (int k = 0; k < r.results.size(); k++) {
            items.insertItem(outs.get(k), r.results.get(k).copy(), false);
        }
        if (r.bonus.isPresent() && level.random.nextFloat() < r.bonusChance) {
            ItemStack bonus = r.bonus.get().copy();
            for (int s : outs) {
                bonus = items.insertItem(s, bonus, false);
                if (bonus.isEmpty()) {
                    break;
                }
            }
        }
        int t = 0;
        for (FluidStack f : r.fluidResults) {
            MachineTank tank = outputTank(t++);
            if (tank != null) {
                tank.fillInternal(f.copy(), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    /** Inserts into any output slot (used by subclasses). */
    protected ItemStack insertOutput(ItemStack stack, boolean simulate) {
        for (int s : slots(MachineLayout.Role.OUTPUT)) {
            stack = items.insertItem(s, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

}
