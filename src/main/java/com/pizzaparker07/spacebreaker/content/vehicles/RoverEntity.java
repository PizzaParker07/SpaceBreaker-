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
public class RoverEntity extends Entity {
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
    private static final byte SOLAR = 1;
    private static final byte RTG = 2;
    private static final byte PELLET = 4;

    private final SimpleContainer cargo = new SimpleContainer(CARGO_SLOTS) {
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
        builder.define(ENERGY, 0).define(UPGRADES, (byte) 0).define(DAMAGE, 0F).define(HURT, 0).define(DUST, 0F);
    }

    public int energy() {
        return entityData.get(ENERGY);
    }

    public void setEnergy(int fe) {
        energyServer = Mth.clamp(fe, 0, CAPACITY);
        entityData.set(ENERGY, energyServer);
    }

    public float energyFraction() {
        return energy() / (float) CAPACITY;
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
        return new Vec3(0, dimensions.height() * 0.66, z).yRot(-getYRot() * Mth.DEG_TO_RAD);
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

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(VehiclesModule.ROVER_ITEM.get());
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide) {
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.threeRows(id, inv, cargo),
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
                spawnAtLocation(VehiclesModule.ROVER_ITEM.get());
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

    private void drive() {
        double g = Environment.gravityFactor(level());
        LivingEntity driver = getControllingPassenger();
        double throttle = 0;
        float steer = 0;
        if (driver != null && energy() > 0) {
            throttle = Mth.clamp(driver.zza, -1F, 1F);
            steer = Mth.clamp(driver.xxa, -1F, 1F);
        }
        BlockPos under = getBlockPosBelowThatAffectsMyMovement();
        double grip = onGround() ? RoverPhysics.grip(level().getBlockState(under).getFriction(level(), under, this)) : 0;
        speed = RoverPhysics.speed(speed, throttle, onGround(), RoverPhysics.traction(g) * Math.max(grip, 0.02));
        deltaYaw = RoverPhysics.yawChange(speed, steer, onGround() ? grip : 0);
        setYRot(getYRot() + deltaYaw);
        Vec3 v = getDeltaMovement();
        double vy = onGround() && v.y <= 0 ? 0 : v.y;
        vy = RoverPhysics.fall(vy, g);
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        // the tyres steer the ground velocity towards the heading as far as the surface lets them; airborne, it keeps going
        double[] h = RoverPhysics.slide(v.x, v.z, -Mth.sin(yaw) * speed, Mth.cos(yaw) * speed, grip);
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

    private void serverPower(double moved) {
        int fe = energyServer;
        travelled += moved;
        if (travelled >= 0.25) {
            fe -= RoverPhysics.driveCost(travelled);
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
        if (hasSolar() && tickCount % 10 == 0 && dust() > 0 && !level().getEntitiesOfClass(
                com.pizzaparker07.spacebreaker.content.mars.entity.DustWhirl.class,
                getBoundingBox().inflate(com.pizzaparker07.spacebreaker.content.mars.entity.DustWhirl.REACH, 3, com.pizzaparker07.spacebreaker.content.mars.entity.DustWhirl.REACH)).isEmpty()) {
            // a passing dust whirl scours the roof panel clean, as it does a parked tracker
            setDust(0F);
        }
        if (hasRtg() && !pellet.isEmpty()) {
            gain += RTG_OUTPUT * RtgFuelPelletItem.activity(pellet, level().getGameTime());
        }
        int whole = (int) gain;
        gain -= whole;
        fe += whole;
        if (tickCount % 10 == 0 && lastMoved < 0.02 && fe < CAPACITY) {
            fe += chargeFromNeighbours(Math.min(CHARGE_RATE * 10, CAPACITY - fe));
        }
        if (fe != energyServer) {
            setEnergy(fe);
        }
    }

    private double gain;
    private double lastMoved;
    private double lastX = Double.NaN;
    private double lastZ = Double.NaN;

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

    private void clientEffects() {
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
        net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(CARGO_SLOTS, ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(tag.getCompound("cargo"), items, registryAccess());
        for (int i = 0; i < CARGO_SLOTS; i++) {
            cargo.setItem(i, items.get(i));
        }
        pellet = ItemStack.parseOptional(registryAccess(), tag.getCompound("pellet"));
        setFlag(PELLET, !pellet.isEmpty());
        setDust(tag.getFloat("dust"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("energy", energyServer);
        tag.putByte("upgrades", entityData.get(UPGRADES));
        tag.putFloat("dust", dust());
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
