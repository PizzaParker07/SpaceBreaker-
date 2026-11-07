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
    /** Upper limit on the sunlight multiplier near the sun (hot cells lose efficiency). */
    public static final double MAX_SOLAR_FACTOR = 3.0;
    private static final float SLEW_PER_TICK = (float) Math.toRadians(0.6);
    /** Output lost when the cells are fully covered in dust (some light still scatters through). */
    public static final float MAX_DUST_LOSS = 0.4F;
    /** How often settling dust is added, ticks. */
    public static final int DUST_INTERVAL = 20;

    private final int peak;
    private final Buffer energy;
    private int lastOutput;
    private double carry;
    private double exactOutput;
    /** Dust cover on the cells, 0 = clean, 1 = fully covered. */
    private float dust;

    // client-side animation state
    private float tilt;
    private float tiltO;
    private boolean tiltInitialised;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        this(MachinesModule.SOLAR_PANEL_BE.get(), pos, state, PEAK_OUTPUT);
    }

    /** @param peak FE/t with the sun square on the cells at Earth's distance */
    public SolarPanelBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state, int peak) {
        super(type, pos, state);
        this.peak = peak;
        this.energy = new Buffer(peak * 400, peak * 4);
    }

    public IEnergyStorage getEnergy() {
        return energy;
    }

    public int peak() {
        return peak;
    }

    public int getLastOutput() {
        return lastOutput;
    }

    /** Output this tick before whole-FE rounding (FE/t). */
    public double getExactOutput() {
        return exactOutput;
    }

    public float dust() {
        return dust;
    }

    public void setDust(float value) {
        float next = Mth.clamp(value, 0F, 1F);
        if (next != dust) {
            // clients only need the cover in 5% steps (and the moment it is wiped clean)
            boolean visible = next == 0F || (int) (next * 20) != (int) (dust * 20);
            dust = next;
            setChanged();
            if (visible && level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    /** Removes some dust (e.g. a dust devil passing over or a wipe); returns how much was taken off. */
    public float clean(float amount) {
        float before = dust;
        setDust(dust - Math.max(0F, amount));
        return before - dust;
    }

    /** Fraction of output kept under a given dust cover. */
    public static float dustFactor(float dust) {
        return 1F - MAX_DUST_LOSS * Mth.clamp(dust, 0F, 1F);
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
        return irradiance(sun, false);
    }

    /** As {@link #irradiance(float)}; with no air there is no extra loss when the sun is low. */
    public static float irradiance(float sun, boolean vacuum) {
        if (Math.abs(sun) >= Mth.HALF_PI) {
            return 0F;
        }
        float incidence = Mth.cos(sun - targetTilt(sun));            // cosine loss beyond the tracker limit
        float elevation = Mth.HALF_PI - Math.abs(sun);              // sun height above the horizon
        float airMass = vacuum ? 1F : (float) Math.pow(Math.max(0F, Mth.sin(elevation)), 0.35); // thicker air near the horizon
        return Math.max(0F, incidence * airMass);
    }

    /**
     * Cloud cover from rain and storms. Other dimensions share the Overworld's weather flags in vanilla, so only
     * count weather that actually falls here (biome precipitation) and never in vacuum.
     */
    public static float weatherFactor(Level level, BlockPos pos) {
        return weatherFactor(com.pizzaparker07.spacebreaker.core.env.Environment.isVacuum(level), level.isRainingAt(pos.above()), level.isThundering());
    }

    /** @param rainingHere rain actually falling at this spot (not the shared world flag) */
    public static float weatherFactor(boolean vacuum, boolean rainingHere, boolean thundering) {
        if (vacuum || !rainingHere) {
            return 1F;
        }
        return thundering ? 0.2F : 0.45F;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity be) {
        int produced = 0;
        if (level.dimensionType().hasSkyLight() && level.canSeeSky(pos.above())) {
            float factor = irradiance(sunAngle(level, 1F), com.pizzaparker07.spacebreaker.core.env.Environment.isVacuum(level))
                    // sunlight at this body's distance from the sun, after dust storms and clouds (capped: cells overheat)
                    * (float) Math.min(MAX_SOLAR_FACTOR, com.pizzaparker07.spacebreaker.core.env.Environment.solarFactor(level, pos));
            factor *= weatherFactor(level, pos);
            factor *= dustFactor(be.dust);
            if (level.getGameTime() % DUST_INTERVAL == 0) {
                double rate = com.pizzaparker07.spacebreaker.core.env.Environment.dustRate(level, pos);
                if (rate > 0) {
                    be.setDust(be.dust + (float) (rate * DUST_INTERVAL));
                }
            }
            // the orbit's shadow comes in through Environment.solarFactor (rocketry registers it as a sunlight modifier)
            // keep the fraction: far from the sun a panel makes well under 1 FE/t, and rounding would lose all of it
            be.exactOutput = be.peak * factor;
            be.carry += be.exactOutput;
            produced = (int) be.carry;
            be.carry -= produced;
        } else {
            be.carry = 0;
            be.exactOutput = 0;
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
        tag.putFloat("dust", dust);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putFloat("dust", dust);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setStored(tag.getInt("energy"));
        dust = Mth.clamp(tag.getFloat("dust"), 0F, 1F);
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
