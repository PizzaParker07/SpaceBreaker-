package com.pizzaparker07.spacebreaker.content.vehicles;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.RtgFuelPelletItem;
import com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Two-seat battery-electric planetary rover. Hub motors on four wheels, a cargo bay, and two upgrades: a solar
 * panel on the roof and a small RTG that takes a fuel pellet. Charges from any powered block it is parked next to
 * (a battery bank, a cable end). The driver's client simulates the drive and the server checks it through vanilla's
 * vehicle packets; energy, upgrades and damage are server authoritative and synced to everyone nearby.
 */
public class RoverEntity extends Entity implements com.pizzaparker07.spacebreaker.core.env.PressurizedVehicle {
    /** Battery of the standard rover; each {@link RoverType} has its own. */
    public static final int CAPACITY = 400_000;
    /** Roof panel: a small array, so a fraction of a solar tracker's peak. */
    public static final int SOLAR_PEAK = 6;
    /** Rover RTG: a smaller heat source than the stationary generator. */
    public static final int RTG_OUTPUT = 10;
    /** Pulled from a powered neighbour while parked, FE per tick. */
    public static final int CHARGE_RATE = 200;
    public static final int CARGO_SLOTS = 27;

    private static final EntityDataAccessor<Integer> ENERGY = SynchedEntityData.defineId(RoverEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> UPGRADES = SynchedEntityData.defineId(RoverEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(RoverEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> HURT = SynchedEntityData.defineId(RoverEntity.class, EntityDataSerializers.INT);
    /** Dust on the roof panel, 0 = clean, 1 = fully covered (same loss as a solar tracker). */
    private static final EntityDataAccessor<Float> DUST = SynchedEntityData.defineId(RoverEntity.class, EntityDataSerializers.FLOAT);
    /** Cabin oxygen in breathing ticks (pressurised rover only). */
    private static final EntityDataAccessor<Integer> CABIN_O2 = SynchedEntityData.defineId(RoverEntity.class, EntityDataSerializers.INT);
    /** Cabin oxygen tank, breathing ticks: 64,000 mB of oxygen gas (8,000 mB of LOX); about 1.8 hours for one rider. */
    public static final int CABIN_O2_CAPACITY = 128_000;
    /** Seat height above the rover's base, blocks. */
    public static final double SEAT_HEIGHT = 1.3 * 0.66;
    /** Life support draw while anyone is aboard, FE/t. */
    public static final int CABIN_POWER = 4;
    private static final byte SOLAR = 1;
    private static final byte RTG = 2;
    private static final byte PELLET = 4;

    private final RoverType roverType = VehiclesModule.typeOf(getType());
    private final SimpleContainer cargo = new SimpleContainer(roverType.cargoSlots()) {
        @Override
        public boolean stillValid(Player player) {
            return isAlive() && player.distanceToSqr(RoverEntity.this) < 64;
        }
    };
    private ItemStack pellet = ItemStack.EMPTY;
    private double speed;
    private float deltaYaw;
    private double travelled;
    private int energyServer;
    /** Client: wheel turn (radians) and suspension compression for the renderer. */
    public float wheelSpin;
    public float wheelSpinO;
    public float suspension;
    public float suspensionO;
    /** Client: body tilt following the ground under the wheels (radians): front up, and the model's +X side up. */
    public float bodyPitch;
    public float bodyPitchO;
    public float bodyRoll;
    public float bodyRollO;
    /** Client: how far the leaning body sits above the rover's feet (the mean of the ground under the wheels). */
    public float bodyLift;
    public float bodyLiftO;
    private float suspensionVel;
    private boolean wasOnGround = true;
    private double lastVy;

    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private float lerpYRot;

    public RoverEntity(EntityType<? extends RoverEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    // ------------------------------------------------------------------ data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ENERGY, 0).define(UPGRADES, (byte) 0).define(DAMAGE, 0F).define(HURT, 0).define(DUST, 0F).define(CABIN_O2, 0);
    }

    private net.minecraft.world.inventory.MenuType<?> cargoMenu() {
        return switch (roverType.cargoSlots() / 9) {
            case 1 -> net.minecraft.world.inventory.MenuType.GENERIC_9x1;
            case 2 -> net.minecraft.world.inventory.MenuType.GENERIC_9x2;
            case 4 -> net.minecraft.world.inventory.MenuType.GENERIC_9x4;
            default -> net.minecraft.world.inventory.MenuType.GENERIC_9x3;
        };
    }

    // ------------------------------------------------------------------ cabin (pressurised rover)

    public int cabinOxygen() {
        return entityData.get(CABIN_O2);
    }

    public void setCabinOxygen(int ticks) {
        entityData.set(CABIN_O2, Mth.clamp(ticks, 0, CABIN_O2_CAPACITY));
    }

    private double breathed;

    @Override
    public boolean cabinPressurized() {
        return roverType.cabin() && cabinOxygen() > 0 && energy() > 0;
    }

    @Override
    public double radiationFactor() {
        return roverType.cabin() ? 0.5 : 1.0;
    }

    @Override
    public boolean breathe(LivingEntity rider, double breathingTicks) {
        if (!cabinPressurized() || rider.getVehicle() != this) {
            return false;
        }
        breathed += Math.max(0, breathingTicks);
        int whole = (int) breathed;
        if (whole > 0) {
            breathed -= whole;
            setCabinOxygen(cabinOxygen() - whole);
        }
        return true;
    }

    /** Fills the cabin tank from a fluid handler (oxygen gas or LOX); returns breathing ticks added. */
    public int fillCabin(net.neoforged.neoforge.fluids.capability.IFluidHandler from, int maxMb) {
        if (!roverType.cabin() || from == null) {
            return 0;
        }
        int room = CABIN_O2_CAPACITY - cabinOxygen();
        for (int t = 0; t < from.getTanks() && room > 0; t++) {
            net.neoforged.neoforge.fluids.FluidStack in = from.getFluidInTank(t);
            int per = com.pizzaparker07.spacebreaker.content.suit.block.GasMachineBlockEntity.ticksPerMb(in);
            if (per <= 0) {
                continue;
            }
            int mb = Math.min(maxMb, room / per);
            if (mb <= 0) {
                continue;
            }
            net.neoforged.neoforge.fluids.FluidStack got = from.drain(in.copyWithAmount(mb), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            int added = got.getAmount() * per;
            setCabinOxygen(cabinOxygen() + added);
            return added;
        }
        return 0;
    }

    public RoverType roverType() {
        return roverType;
    }

    public int energy() {
        return entityData.get(ENERGY);
    }

    public void setEnergy(int fe) {
        energyServer = Mth.clamp(fe, 0, roverType.capacity());
        entityData.set(ENERGY, energyServer);
    }

    public float energyFraction() {
        return energy() / (float) roverType.capacity();
    }

    private boolean flag(byte f) {
        return (entityData.get(UPGRADES) & f) != 0;
    }

    private void setFlag(byte f, boolean on) {
        byte b = entityData.get(UPGRADES);
        entityData.set(UPGRADES, (byte) (on ? b | f : b & ~f));
    }

    public boolean hasSolar() {
        return flag(SOLAR);
    }

    public float dust() {
        return entityData.get(DUST);
    }

    public void setDust(float dust) {
        entityData.set(DUST, Mth.clamp(dust, 0F, 1F));
    }

    public boolean hasRtg() {
        return flag(RTG);
    }

    public boolean hasPellet() {
        return flag(PELLET);
    }

    public SimpleContainer cargo() {
        return cargo;
    }

    public double speed() {
        return speed;
    }

    public void installSolar() {
        setFlag(SOLAR, true);
    }

    public void installRtg(ItemStack fuel) {
        setFlag(RTG, true);
        if (!fuel.isEmpty()) {
            pellet = fuel.copyWithCount(1);
            RtgFuelPelletItem.stamp(pellet, level());
            setFlag(PELLET, true);
        }
    }

    // ------------------------------------------------------------------ riding

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 2;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        int i = getPassengers().indexOf(passenger);
        double z = i <= 0 ? 0.25 : -0.55;
        // the seats are the same height in every model, whatever the cabin above them
        return new Vec3(0, SEAT_HEIGHT, z).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        super.positionRider(passenger, move);
        passenger.setYRot(passenger.getYRot() + deltaYaw);
        passenger.setYHeadRot(passenger.getYHeadRot() + deltaYaw);
        if (passenger instanceof LivingEntity living) {
            living.yBodyRot = getYRot();
        }
    }

    /**
     * Riders step off to the side, not onto the roof: left side first, then right, behind, in front, each checked for
     * a safe spot to stand (like getting out of a boat). Only if all four are blocked does the vanilla spot on top apply.
     */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 left = new Vec3(forward.z, 0, -forward.x);
        double side = getBbWidth() / 2 + passenger.getBbWidth() / 2 + 0.25;
        Vec3[] tries = {left.scale(side), left.scale(-side), forward.scale(-(side + 0.6)), forward.scale(side + 0.6)};
        for (Vec3 off : tries) {
            Vec3 at = position().add(off);
            BlockPos base = BlockPos.containing(at);
            for (BlockPos p : new BlockPos[]{base, base.above(), base.below()}) {
                double floor = level().getBlockFloorHeight(p);
                if (!net.minecraft.world.entity.vehicle.DismountHelper.isBlockFloorValid(floor)) {
                    continue;
                }
                Vec3 spot = new Vec3(at.x, p.getY() + floor, at.z);
                for (net.minecraft.world.entity.Pose pose : passenger.getDismountPoses()) {
                    net.minecraft.world.phys.AABB box = passenger.getLocalBoundsForPose(pose);
                    if (net.minecraft.world.entity.vehicle.DismountHelper.canDismountTo(level(), passenger, box.move(spot))) {
                        passenger.setPose(pose);
                        return spot;
                    }
                }
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        float rel = Mth.wrapDegrees(passenger.getYRot() - getYRot());
        float clamped = Mth.clamp(rel, -120F, 120F);
        passenger.yRotO += clamped - rel;
        passenger.setYRot(passenger.getYRot() + clamped - rel);
        passenger.setYHeadRot(passenger.getYRot());
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    /** The rear deck and the antenna reach past the hitbox: keep the model drawn while any of it is on screen. */
    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(1.2, 0.6, 1.2);
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(VehiclesModule.itemOf(roverType));
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide) {
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ChestMenu(cargoMenu(), id, inv, cargo, roverType.cargoSlots() / 9),
                        Component.translatable("container.spacebreaker.rover")));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.is(net.minecraft.world.item.Items.BRUSH) && hasSolar() && dust() > 0.01F) {
            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                setDust(0F);
                server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                                level().getBlockState(getBlockPosBelowThatAffectsMyMovement())),
                        getX(), getY() + getBbHeight(), getZ(), 14, 0.5, 0.05, 0.5, 0.02);
                playSound(SoundEvents.BRUSH_GENERIC, 0.8F, 1.1F);
                held.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND
                        ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                player.displayClientMessage(Component.translatable("message.spacebreaker.panel_brushed"), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (roverType.cabin() && !held.isEmpty()) {
            ItemStack one = held.copyWithCount(1);
            var handler = net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(one);
            if (handler.isPresent() && handler.get().getTanks() > 0
                    && com.pizzaparker07.spacebreaker.content.suit.block.GasMachineBlockEntity.ticksPerMb(handler.get().getFluidInTank(0)) > 0) {
                if (!level().isClientSide) {
                    var h = handler.get();
                    if (fillCabin(h, Integer.MAX_VALUE) > 0) {
                        // hand back the emptied (or partly emptied) container in place of the full one
                        ItemStack emptied = h.getContainer();
                        held.shrink(1);
                        if (held.isEmpty()) {
                            player.setItemInHand(hand, emptied);
                        } else if (!player.getInventory().add(emptied)) {
                            player.drop(emptied, false);
                        }
                        playSound(SoundEvents.BUCKET_EMPTY, 0.6F, 1.4F);
                    }
                    player.displayClientMessage(Component.translatable("message.spacebreaker.rover.cabin_o2",
                            Math.round(cabinOxygen() * 100F / CABIN_O2_CAPACITY)), true);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        if (held.is(MachinesModule.SOLAR_PANEL_ITEM.get()) && !hasSolar()) {
            if (!level().isClientSide) {
                installSolar();
                held.consume(1, player);
                playSound(SoundEvents.IRON_TRAPDOOR_CLOSE, 1F, 1.2F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.is(MachinesModule.RTG.item().get()) && !hasRtg()) {
            if (!level().isClientSide) {
                installRtg(ItemStack.EMPTY);
                held.consume(1, player);
                playSound(SoundEvents.IRON_TRAPDOOR_CLOSE, 1F, 0.8F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.getItem() instanceof RtgFuelPelletItem && hasRtg() && !hasPellet()) {
            if (!level().isClientSide) {
                installRtg(held);
                held.consume(1, player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (canAddPassenger(player) && !player.isPassenger()) {
            if (!level().isClientSide) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || isInvulnerableTo(source)) {
            return !isInvulnerableTo(source);
        }
        entityData.set(HURT, 10);
        entityData.set(DAMAGE, entityData.get(DAMAGE) + amount * 10F);
        markHurt();
        boolean creative = source.getEntity() instanceof Player p && p.getAbilities().instabuild;
        if (creative || entityData.get(DAMAGE) > 60F) {
            if (!creative) {
                spawnAtLocation(VehiclesModule.itemOf(roverType));
                if (hasSolar()) {
                    spawnAtLocation(MachinesModule.SOLAR_PANEL_ITEM.get());
                }
                if (hasRtg()) {
                    spawnAtLocation(MachinesModule.RTG.item().get());
                }
            }
            if (!pellet.isEmpty()) {
                spawnAtLocation(pellet);
            }
            Containers.dropContents(level(), blockPosition(), cargo);
            discard();
        }
        return true;
    }

    public int hurtTime() {
        return entityData.get(HURT);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        if (entityData.get(HURT) > 0) {
            entityData.set(HURT, entityData.get(HURT) - 1);
        }
        if (entityData.get(DAMAGE) > 0) {
            entityData.set(DAMAGE, Math.max(0, entityData.get(DAMAGE) - 0.2F));
        }
        super.tick();
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            drive();
        } else {
            tickLerp();
            deltaYaw = 0;
        }
        if (level().isClientSide) {
            clientEffects();
        } else {
            // the driver's client moves the rover between ticks, so measure from where it was last tick
            double moved = Double.isNaN(lastX) ? 0 : Math.hypot(getX() - lastX, getZ() - lastZ);
            lastX = getX();
            lastZ = getZ();
            lastMoved = moved < 4 ? moved : 0;
            serverPower(lastMoved);
            if (onGround() && tickCount % 2 == 0) {
                leaveTracks();
            }
        }
    }

    /** Top of the liquid this rover sits in (world y), or NaN when it is not in any. */
    private double liquidSurface() {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int y0 = Mth.floor(getY() + 0.2);
        if (level().getFluidState(p.set(Mth.floor(getX()), y0, Mth.floor(getZ()))).isEmpty()
                && level().getFluidState(p.set(Mth.floor(getX()), y0 - 1, Mth.floor(getZ()))).isEmpty()) {
            return Double.NaN;
        }
        int y = level().getFluidState(p.set(Mth.floor(getX()), y0, Mth.floor(getZ()))).isEmpty() ? y0 - 1 : y0;
        while (y < y0 + 4 && !level().getFluidState(p.set(Mth.floor(getX()), y + 1, Mth.floor(getZ()))).isEmpty()) {
            y++;
        }
        var fluid = level().getFluidState(p.set(Mth.floor(getX()), y, Mth.floor(getZ())));
        return y + fluid.getHeight(level(), p);
    }

    /** Whether this rover is afloat right now (amphibious types on a liquid). */
    public boolean afloat() {
        return afloat;
    }

    private boolean afloat;

    @Override
    public boolean isPushedByFluid() {
        return !roverType.amphibious();
    }

    private void drive() {
        if (roverType.amphibious()) {
            double surface = liquidSurface();
            afloat = !Double.isNaN(surface) && surface > getY() + 0.1;
            if (afloat) {
                swim(surface);
                return;
            }
        }
        double g = Environment.gravityFactor(level());
        LivingEntity driver = getControllingPassenger();
        double throttle = 0;
        float steer = 0;
        // the hand brake (jump key) works with an empty battery too: it is a mechanical lock on the wheels
        boolean handBrake = driver != null && driver.jumping;
        if (driver != null && energy() > 0) {
            throttle = Mth.clamp(driver.zza, -1F, 1F);
            steer = Mth.clamp(driver.xxa, -1F, 1F);
        }
        BlockPos under = getBlockPosBelowThatAffectsMyMovement();
        double grip = onGround() ? Math.min(1.0, RoverPhysics.grip(level().getBlockState(under).getFriction(level(), under, this)) * roverType.grip()) : 0;
        speed = RoverPhysics.speed(speed, throttle, handBrake, onGround(), RoverPhysics.traction(g) * Math.max(grip, 0.02),
                RoverPhysics.rollingDrag(g) * Math.max(grip, 0.02), roverType.maxSpeed(), roverType.accel());
        deltaYaw = RoverPhysics.yawChange(speed, steer, onGround() ? grip : 0);
        setYRot(getYRot() + deltaYaw);
        Vec3 v = getDeltaMovement();
        double vy = onGround() && v.y <= 0 ? 0 : v.y;
        vy = RoverPhysics.fall(vy, g);
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        // the tyres steer the ground velocity towards the heading as far as the surface lets them; airborne, it keeps going
        double[] h = RoverPhysics.slide(v.x, v.z, -Mth.sin(yaw) * speed, Mth.cos(yaw) * speed, RoverPhysics.lateralGrip(grip, g));
        setDeltaMovement(h[0], vy, h[1]);
        double before = getDeltaMovement().y;
        move(MoverType.SELF, getDeltaMovement());
        if (onGround() && before < 0) {
            double up = RoverPhysics.bounce(before);
            setDeltaMovement(getDeltaMovement().x, up, getDeltaMovement().z);
            if (up > 0) {
                speed *= 0.9;
            }
        }
        if (horizontalCollision) {
            speed *= 0.5;
        }
    }

    /** Draft of the floating hull below the liquid surface, blocks. */
    public static final double DRAFT = 0.45;

    /** Afloat: buoyancy holds the hull at its waterline, the propeller pushes, the rudder turns; no tyre grip. */
    private void swim(double surface) {
        LivingEntity driver = getControllingPassenger();
        double throttle = 0;
        float steer = 0;
        if (driver != null && energy() > 0) {
            throttle = Mth.clamp(driver.zza, -1F, 1F);
            steer = Mth.clamp(driver.xxa, -1F, 1F);
        }
        speed = RoverPhysics.swimSpeed(speed, throttle, roverType.swimSpeed());
        deltaYaw = RoverPhysics.yawChange(speed, steer, 0.6);
        setYRot(getYRot() + deltaYaw);
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 v = getDeltaMovement();
        double vy = RoverPhysics.buoyancy(v.y, surface - DRAFT - getY());
        // water carries the hull on: it drifts towards the heading rather than turning on the spot
        double[] h = RoverPhysics.slide(v.x, v.z, -Mth.sin(yaw) * speed, Mth.cos(yaw) * speed, 0.15);
        setDeltaMovement(h[0], vy, h[1]);
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision) {
            if (speed > 0.02) {
                // nosing into a bank: the wheels catch the shore and haul the hull up out of the water
                setDeltaMovement(getDeltaMovement().x, 0.42, getDeltaMovement().z);
            } else {
                speed *= 0.5;
            }
        }
    }

    private void serverPower(double moved) {
        int fe = energyServer;
        travelled += moved;
        if (travelled >= 0.25) {
            // the driver's client steers the rover, so the server checks for itself whether it is afloat
            boolean swimming = roverType.amphibious() && !Double.isNaN(liquidSurface()) && liquidSurface() > getY() + 0.1;
            fe -= RoverPhysics.driveCost(travelled, swimming ? roverType.fePerBlockSwimming() : roverType.fePerBlock());
            travelled = 0;
        }
        if (hasSolar() && level().canSeeSky(blockPosition().above())) {
            float sun = SolarPanelBlockEntity.irradiance(SolarPanelBlockEntity.sunAngle(level(), 1F), Environment.isVacuum(level()));
            gain += SOLAR_PEAK * sun * Math.min(SolarPanelBlockEntity.MAX_SOLAR_FACTOR, Environment.solarFactor(level(), blockPosition()))
                    * SolarPanelBlockEntity.weatherFactor(level(), blockPosition())
                    * SolarPanelBlockEntity.dustFactor(dust());
            if (tickCount % SolarPanelBlockEntity.DUST_INTERVAL == 0) {
                double rate = Environment.dustRate(level(), blockPosition());
                if (rate > 0) {
                    setDust(dust() + (float) (rate * SolarPanelBlockEntity.DUST_INTERVAL));
                }
            }
        }
        if (hasSolar() && tickCount % 20 == 0 && dust() > 0.01F && !level().getEntitiesOfClass(
                com.pizzaparker07.spacebreaker.content.mars.entity.DustWhirl.class,
                getBoundingBox().inflate(com.pizzaparker07.spacebreaker.content.mars.entity.DustWhirl.REACH, 3, com.pizzaparker07.spacebreaker.content.mars.entity.DustWhirl.REACH)).isEmpty()) {
            // a passing dust whirl scours the roof panel clean, as it does a parked tracker (and as it did for real rovers)
            setDust(0F);
            for (Entity rider : getPassengers()) {
                if (rider instanceof Player p) {
                    p.displayClientMessage(Component.translatable("message.spacebreaker.rover.whirl_cleaned"), true);
                }
            }
        }
        if (hasRtg() && !pellet.isEmpty()) {
            gain += RTG_OUTPUT * RtgFuelPelletItem.activity(pellet, level().getGameTime());
        }
        int whole = (int) gain;
        gain -= whole;
        fe += whole;
        if (tickCount % 10 == 0 && lastMoved < 0.02 && fe < roverType.capacity()) {
            fe += chargeFromNeighbours(Math.min(CHARGE_RATE * 10, roverType.capacity() - fe));
        }
        if (roverType.cabin()) {
            if (isVehicle() && cabinOxygen() > 0) {
                fe -= CABIN_POWER;   // fans, scrubbers and heaters
            }
            if (tickCount % 10 == 0 && lastMoved < 0.02 && cabinOxygen() < CABIN_O2_CAPACITY) {
                topUpCabin();
            }
        }
        fe = Math.max(0, fe);
        if (fe != energyServer) {
            setEnergy(fe);
        }
    }

    private double gain;
    private double lastMoved;
    private double lastX = Double.NaN;
    private double lastZ = Double.NaN;

    /** Parked beside a tank, a gas pipe or anything else holding oxygen: refill the cabin tank from it. */
    private void topUpCabin() {
        var box = getBoundingBox().inflate(0.6, 0.1, 0.6);
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY - 0.5, box.minZ), BlockPos.containing(box.maxX, box.minY + 1, box.maxZ))) {
            for (int i = -1; i < 6; i++) {
                var h = level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, p,
                        i < 0 ? null : net.minecraft.core.Direction.from3DDataValue(i));
                if (h != null && fillCabin(h, 1_000) > 0) {
                    return;
                }
            }
        }
    }

    private int chargeFromNeighbours(int want) {
        var box = getBoundingBox().inflate(0.6, 0.1, 0.6);
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY - 0.5, box.minZ), BlockPos.containing(box.maxX, box.minY + 1, box.maxZ))) {
            // machines often give power from one face only (a battery bank's front), so try them all
            for (int i = -1; i < 6; i++) {
                IEnergyStorage s = level().getCapability(Capabilities.EnergyStorage.BLOCK, p, i < 0 ? null : net.minecraft.core.Direction.from3DDataValue(i));
                if (s != null && s.canExtract()) {
                    int got = s.extractEnergy(want, false);
                    if (got > 0) {
                        return got;
                    }
                }
            }
        }
        return 0;
    }

    /** Wheel ruts in soft soil that keeps prints (lunar regolith, Martian soil): uses the soil's own print state. */
    private void leaveTracks() {
        if (lastMoved < 0.02) {
            return;
        }
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        for (int side = -1; side <= 1; side += 2) {
            for (int axle = -1; axle <= 1; axle += 2) {
                double wx = getX() + fz * 0.7 * side * -1 + fx * 0.65 * axle;
                double wz = getZ() - fx * 0.7 * side * -1 + fz * 0.65 * axle;
                BlockPos below = BlockPos.containing(wx, getY() - 0.2, wz);
                BlockState state = level().getBlockState(below);
                BlockState printed = printed(state, side);
                if (printed != state) {
                    level().setBlock(below, printed, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private BlockState printed(BlockState state, int side) {
        for (Property<?> prop : state.getProperties()) {
            if (!prop.getName().equals("print")) {
                continue;
            }
            Comparable<?> now = state.getValue(prop);
            if (!(now instanceof net.minecraft.util.StringRepresentable sr) || !sr.getSerializedName().equals("none")) {
                return state;
            }
            Comparable<?> pick = null;
            String want = side < 0 ? "left" : "right";
            for (Object value : prop.getPossibleValues()) {
                String name = ((net.minecraft.util.StringRepresentable) value).getSerializedName();
                if (name.equals("track")) {
                    pick = (Comparable<?>) value;
                    break;
                }
                if (name.equals(want)) {
                    pick = (Comparable<?>) value;
                }
            }
            if (pick == null) {
                return state;
            }
            BlockState out = state.setValue((Property) prop, (Comparable) pick);
            for (Property<?> f : out.getProperties()) {
                if (f.getName().equals("facing") && f.getPossibleValues().contains(getDirection())) {
                    out = out.setValue((Property) f, (Comparable) getDirection());
                }
            }
            return out;
        }
        return state;
    }

    /** Top of the ground under a point, searched a little above and below the rover's feet. */
    private double groundAt(double x, double z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int top = Mth.floor(getY() + 1.2);
        for (int y = top; y >= Mth.floor(getY() - 1.6); y--) {
            p.set(Mth.floor(x), y, Mth.floor(z));
            var shape = level().getBlockState(p).getCollisionShape(level(), p);
            if (!shape.isEmpty()) {
                return y + shape.max(net.minecraft.core.Direction.Axis.Y);
            }
        }
        return getY() - 1.6;
    }

    /** Leans the body with the ground under the wheels (client only, smoothed). */
    private void updateTilt() {
        bodyPitchO = bodyPitch;
        bodyRollO = bodyRoll;
        bodyLiftO = bodyLift;
        float a = (180F - getYRot()) * Mth.DEG_TO_RAD;
        // the renderer's model space after its yaw turn: +X = (cos a, -sin a), front (-Z) = (-sin a, -cos a)
        double fx = -Mth.sin(a), fz = -Mth.cos(a);
        double sx = Mth.cos(a), sz = -Mth.sin(a);
        float pitch = 0;
        float roll = 0;
        float lift = 0;
        if (onGround()) {
            double front = groundAt(getX() + fx * 0.75, getZ() + fz * 0.75);
            double rear = groundAt(getX() - fx * 0.75, getZ() - fz * 0.75);
            double plus = groundAt(getX() + sx * 0.8, getZ() + sz * 0.8);
            double minus = groundAt(getX() - sx * 0.8, getZ() - sz * 0.8);
            pitch = RoverPhysics.tilt(front, rear, 1.5);
            roll = RoverPhysics.tilt(plus, minus, 1.6);
            // the body pivots about its middle, so lift it to the mean ground: the low wheels stay on the ground
            lift = (float) Mth.clamp((front + rear + plus + minus) / 4 - getY(), 0, 0.75);
        }
        bodyPitch += (pitch - bodyPitch) * 0.3F;
        bodyRoll += (roll - bodyRoll) * 0.3F;
        bodyLift += (lift - bodyLift) * 0.3F;
    }

    private void clientEffects() {
        updateTilt();
        wheelSpinO = wheelSpin;
        double moved = Math.hypot(getX() - xo, getZ() - zo);
        double forward = -Mth.sin(getYRot() * Mth.DEG_TO_RAD) * (getX() - xo) + Mth.cos(getYRot() * Mth.DEG_TO_RAD) * (getZ() - zo);
        wheelSpin += (float) (forward / 0.3);
        // suspension: a damped spring kicked by landings, softer in low gravity
        suspensionO = suspension;
        boolean ground = onGround();
        double vy = getY() - yo;
        if (ground && !wasOnGround && lastVy < -0.05) {
            suspensionVel += (float) Math.min(0.6, -lastVy * 1.5);
        }
        float k = 0.25F * (float) Math.max(0.3, Environment.gravityFactor(level()));
        suspensionVel += -suspension * k;
        suspensionVel *= 0.8F;
        suspension += suspensionVel;
        wasOnGround = ground;
        lastVy = vy;
        if (ground && moved > 0.08 && random.nextFloat() < Math.min(1.0, moved * 3)) {
            BlockState below = level().getBlockState(BlockPos.containing(getX(), getY() - 0.2, getZ()));
            if (below.getRenderShape() != RenderShape.INVISIBLE) {
                float yaw = getYRot() * Mth.DEG_TO_RAD;
                for (int side = -1; side <= 1; side += 2) {
                    double px = getX() + Mth.sin(yaw) * 0.9 + Mth.cos(yaw) * 0.75 * side;
                    double pz = getZ() - Mth.cos(yaw) * 0.9 + Mth.sin(yaw) * 0.75 * side;
                    level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below), px, getY() + 0.1, pz,
                            Mth.sin(yaw) * moved * 0.6, 0.08 + random.nextFloat() * 0.06, -Mth.cos(yaw) * moved * 0.6);
                }
            }
        }
    }

    // ------------------------------------------------------------------ client interpolation (as boats do)

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpSteps = 10;
    }

    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            lerpPositionAndRotationStep(lerpSteps, lerpX, lerpY, lerpZ, lerpYRot, getXRot());
            lerpSteps--;
        }
    }

    @Override
    public double lerpTargetX() {
        return lerpSteps > 0 ? lerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return lerpSteps > 0 ? lerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return lerpSteps > 0 ? lerpZ : getZ();
    }

    @Override
    public float lerpTargetYRot() {
        return lerpSteps > 0 ? lerpYRot : getYRot();
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setEnergy(tag.getInt("energy"));
        entityData.set(UPGRADES, tag.getByte("upgrades"));
        net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(roverType.cargoSlots(), ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(tag.getCompound("cargo"), items, registryAccess());
        for (int i = 0; i < roverType.cargoSlots(); i++) {
            cargo.setItem(i, items.get(i));
        }
        pellet = ItemStack.parseOptional(registryAccess(), tag.getCompound("pellet"));
        setFlag(PELLET, !pellet.isEmpty());
        setDust(tag.getFloat("dust"));
        setCabinOxygen(tag.getInt("cabin_o2"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("energy", energyServer);
        tag.putByte("upgrades", entityData.get(UPGRADES));
        tag.putFloat("dust", dust());
        if (roverType.cabin()) {
            tag.putInt("cabin_o2", cabinOxygen());
        }
        tag.put("cargo", net.minecraft.world.ContainerHelper.saveAllItems(new CompoundTag(), cargo.getItems(), registryAccess()));
        if (!pellet.isEmpty()) {
            tag.put("pellet", pellet.save(registryAccess()));
        }
    }

    @Override
    public void playerTouch(Player player) {
    }

    /** For the showcase and tests: set the drive state directly. */
    public void setSpeed(double speed) {
        this.speed = speed;
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        setDeltaMovement(-Mth.sin(yaw) * speed, getDeltaMovement().y, Mth.cos(yaw) * speed);
    }
}
