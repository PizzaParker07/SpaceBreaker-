package com.pizzaparker07.spacebreaker.content.machines;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class SolarPanelBlockEntity extends BlockEntity {
    /** Peak output in FE/t with the sun square on the cells (tier 1 silicon). */
    public static final int PEAK_OUTPUT = 20;
    /** Mechanical limit of the tracker, radians either side of horizontal. */
    public static final float MAX_TILT = (float) Math.toRadians(60);
    private static final float SLEW_PER_TICK = (float) Math.toRadians(0.6);

    private final Buffer energy = new Buffer(8_000, PEAK_OUTPUT * 4);
    private int lastOutput;

    // client-side animation state
    private float tilt;
    private float tiltO;
    private boolean tiltInitialised;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(MachinesModule.SOLAR_PANEL_BE.get(), pos, state);
    }

    public IEnergyStorage getEnergy() {
        return energy;
    }

    public int getLastOutput() {
        return lastOutput;
    }

    /** Sun angle in radians, 0 = noon, negative = morning (east), positive = afternoon (west). */
    public static float sunAngle(Level level, float partialTick) {
        return Mth.wrapDegrees(level.getSunAngle(partialTick) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }

    /** Where the tracker wants to point for a given sun angle: follow the sun by day, stow flat at night. */
    public static float targetTilt(float sun) {
        if (Math.abs(sun) > Mth.HALF_PI) {
            return 0F;
        }
        return Mth.clamp(sun, -MAX_TILT, MAX_TILT);
    }

    /** Fraction of peak output for the current sun position (0..1), before weather and sky checks. */
    public static float irradiance(float sun) {
        if (Math.abs(sun) >= Mth.HALF_PI) {
            return 0F;
        }
        float incidence = Mth.cos(sun - targetTilt(sun));            // cosine loss beyond the tracker limit
        float elevation = Mth.HALF_PI - Math.abs(sun);              // sun height above the horizon
        float airMass = (float) Math.pow(Math.max(0F, Mth.sin(elevation)), 0.35); // thicker air near the horizon
        return Math.max(0F, incidence * airMass);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity be) {
        int produced = 0;
        if (level.dimensionType().hasSkyLight() && level.canSeeSky(pos.above())) {
            float factor = irradiance(sunAngle(level, 1F));
            if (level.isThundering()) {
                factor *= 0.2F;
            } else if (level.isRaining()) {
                factor *= 0.45F;
            }
            produced = Math.round(PEAK_OUTPUT * factor);
        }
        be.lastOutput = produced;
        if (produced > 0) {
            be.energy.generate(produced);
        }
        if (be.energy.getEnergyStored() > 0) {
            be.pushEnergy(level, pos);
        }
        if (produced > 0 && level.getGameTime() % 20 == 0) {
            be.setChanged();
        }
    }

    private void pushEnergy(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) {
                continue;
            }
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target != null && target.canReceive()) {
                int offered = energy.extractEnergy(energy.getEnergyStored(), true);
                int accepted = target.receiveEnergy(offered, false);
                energy.extractEnergy(accepted, false);
                if (energy.getEnergyStored() == 0) {
                    return;
                }
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity be) {
        float target = targetTilt(sunAngle(level, 1F));
        if (!be.tiltInitialised) {
            be.tilt = be.tiltO = target;
            be.tiltInitialised = true;
            return;
        }
        be.tiltO = be.tilt;
        be.tilt += Mth.clamp(target - be.tilt, -SLEW_PER_TICK, SLEW_PER_TICK);
    }

    public float getTilt(float partialTick) {
        return Mth.lerp(partialTick, tiltO, tilt);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setStored(tag.getInt("energy"));
    }

    /** Internal buffer: other blocks can only extract; the panel itself generates. */
    private static final class Buffer extends EnergyStorage {
        Buffer(int capacity, int maxExtract) {
            super(capacity, 0, maxExtract);
        }

        void generate(int amount) {
            energy = Math.min(capacity, energy + amount);
        }

        void setStored(int amount) {
            energy = Mth.clamp(amount, 0, capacity);
        }
    }
}
