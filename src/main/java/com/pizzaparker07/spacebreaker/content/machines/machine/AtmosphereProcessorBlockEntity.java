package com.pizzaparker07.spacebreaker.content.machines.machine;

import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.fluids.SbFluid;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineLayout;
import com.pizzaparker07.spacebreaker.content.machines.base.TankSpec;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Compresses the local atmosphere and separates its most useful gas: carbon dioxide on Mars and Venus,
 * oxygen from Earth's air, methane on Titan. Output scales with the gas fraction and the ambient pressure
 * (thin air takes longer, thick air is capped by the compressor). Does nothing in vacuum.
 */
public class AtmosphereProcessorBlockEntity extends MachineBlockEntity {
    public static final int ENERGY_PER_TICK = 40;
    public static final int CYCLE = 40;
    public static final int BASE_RATE = 200;
    public static final MachineLayout LAYOUT = new MachineLayout().energy(10, 17).progress(78, 35, MachineLayout.Progress.ARROW).tank(110, 17);
    /** Atmosphere composition keys we can separate, best gas first when fractions tie. */
    private static final Map<String, SbFluid> GASES = Map.of(
            "CO2", FluidsModule.CARBON_DIOXIDE, "O2", FluidsModule.OXYGEN, "CH4", FluidsModule.METHANE, "H2", FluidsModule.HYDROGEN);

    public record Yield(SbFluid gas, int amount) {
    }

    public AtmosphereProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(MachinesModule.ATMOSPHERE_PROCESSOR.type().get(), pos, state, LAYOUT, 40_000, 1_000, 0, List.of(TankSpec.out(16_000)));
    }

    /** What one cycle yields here, or null in vacuum / when no separable gas is present. */
    public static @Nullable Yield yieldAt(Level level, BlockPos pos) {
        if (Bodies.isOrbit(level)) {
            return null;
        }
        double pressure = Environment.pressureKpa(level, pos);
        if (pressure <= 0.01) {
            return null;
        }
        Map<String, Double> composition = Environment.body(level).map(b -> b.atmosphere().composition()).orElse(Map.of());
        SbFluid best = null;
        double fraction = 0;
        for (var e : composition.entrySet()) {
            SbFluid gas = GASES.get(e.getKey());
            if (gas != null && e.getValue() > fraction) {
                best = gas;
                fraction = e.getValue();
            }
        }
        if (best == null) {
            return null;
        }
        double pressureFactor = Mth.clamp(pressure / 10.0, 0.25, 2.0);
        return new Yield(best, Math.max(10, (int) Math.round(BASE_RATE * fraction * pressureFactor)));
    }

    @Override
    protected boolean tickServer(Level level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % 4 == 0) {
            pushFluids(level, pos, 500);
        }
        Yield y = yieldAt(level, pos);
        if (y == null || !level.getBlockState(pos.above()).isAir()) {
            status = 8;
            progress = 0;
            return false;
        }
        maxProgress = CYCLE;
        if (tanks.get(0).fillInternal(y.gas().stack(y.amount()), IFluidHandler.FluidAction.SIMULATE) < y.amount()) {
            status = 2;
            return false;
        }
        if (!energy.consume(ENERGY_PER_TICK)) {
            status = 1;
            return false;
        }
        status = 0;
        if (++progress >= CYCLE) {
            progress = 0;
            tanks.get(0).fillInternal(y.gas().stack(y.amount()), IFluidHandler.FluidAction.EXECUTE);
        }
        return true;
    }
}
