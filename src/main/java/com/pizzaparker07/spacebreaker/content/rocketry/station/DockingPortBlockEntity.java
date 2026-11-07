package com.pizzaparker07.spacebreaker.content.rocketry.station;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketFluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/** Passes liquids from the station's pipes into the docked rocket (see {@link DockingPortBlock}). */
public class DockingPortBlockEntity extends BlockEntity {
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
            RocketEntity r = docked();
            return r == null ? 0 : new RocketFluidHandler(r).getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return new RocketFluidHandler(null).isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            RocketEntity r = docked();
            return r == null ? 0 : r.fill(resource, action.simulate());
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

    public DockingPortBlockEntity(BlockPos pos, BlockState state) {
        super(RocketryModule.DOCKING_PORT_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            DockingPorts.get(server).add(worldPosition, getBlockState().getValue(DockingPortBlock.FACING));
        }
    }

    public IFluidHandler handler() {
        return handler;
    }

    /** The parked rocket nearest to this port, if one is close enough. */
    @Nullable
    public RocketEntity docked() {
        return dockedAt(level, worldPosition);
    }

    @Nullable
    public static RocketEntity dockedAt(@Nullable Level level, BlockPos port) {
        if (level == null) {
            return null;
        }
        Vec3 c = port.getCenter();
        return level.getEntitiesOfClass(RocketEntity.class, new AABB(port).inflate(DockingPortBlock.RANGE),
                        r -> r.state() == FlightState.ORBIT || r.state() == FlightState.IDLE)
                .stream().min(Comparator.comparingDouble(r -> r.distanceToSqr(c))).orElse(null);
    }

    /** Whether a docking port is within reach of this rocket. */
    public static boolean nearPort(RocketEntity rocket) {
        Level level = rocket.level();
        BlockPos at = rocket.blockPosition();
        int r = (int) Math.ceil(DockingPortBlock.RANGE);
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-r, -r, -r), at.offset(r, r + 6, r))) {
            if (level.getBlockState(p).is(RocketryModule.DOCKING_PORT.get())) {
                return true;
            }
        }
        return false;
    }
}
