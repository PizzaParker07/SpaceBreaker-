package com.pizzaparker07.spacebreaker.content.rocketry.pad;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketFluidHandler;
import com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** Pass-through fluid handler: fills the rocket on the nearby pad; holds nothing itself. */
public class FuelUmbilicalBlockEntity extends BlockEntity {
    private BlockPos padCenter;
    private long lastLookup = -1000;

    private final IFluidHandler handler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 4;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            RocketEntity r = rocket();
            return r == null ? 0 : r.roomFor(RocketFluidHandler.tankFuel(tank));
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 1 ? stack.is(Propellant.LOX_TAG) : stack.is(java.util.Objects.requireNonNull(RocketFluidHandler.tankFuel(tank)).fuelTag());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            RocketEntity r = rocket();
            if (r == null || resource.isEmpty()) {
                return 0;
            }
            return r.fill(resource, action.simulate());
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    public FuelUmbilicalBlockEntity(BlockPos pos, BlockState state) {
        super(RocketryModule.FUEL_UMBILICAL_BE.get(), pos, state);
    }

    public IFluidHandler handler() {
        return handler;
    }

    @Nullable
    public RocketEntity rocket() {
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        if (padCenter == null && level.getGameTime() - lastLookup > 40) {
            lastLookup = level.getGameTime();
            LaunchPad.Result r = LaunchPad.findNear(level, getBlockPos(), 5);
            padCenter = r == null ? null : r.center();
        }
        return padCenter == null ? null : LaunchPad.rocketOn(level, padCenter);
    }

    /** LOX boil-off venting while a fuelled rocket stands on the pad. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, FuelUmbilicalBlockEntity be) {
        if (level.random.nextInt(3) != 0) {
            return;
        }
        RocketEntity r = be.rocket();
        if (r != null && r.state() == com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState.IDLE && r.fuelFraction() > 0.05) {
            var facing = state.getValue(TowerAttachmentBlock.FACING);
            double x = pos.getX() + 0.5 + facing.getStepX() * 0.9;
            double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.9;
            level.addParticle(ParticleTypes.CLOUD, x, pos.getY() + 0.5, z, facing.getStepX() * 0.03, -0.01, facing.getStepZ() * 0.03);
        }
    }
}
