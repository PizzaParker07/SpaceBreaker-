package com.pizzaparker07.spacebreaker.content.rocketry.entity;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketGeometry;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketLayout;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketMath;
import com.pizzaparker07.spacebreaker.content.rocketry.item.RocketItem;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Transfers;
import com.pizzaparker07.spacebreaker.content.rocketry.net.RocketActionPayload;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchPad;
import com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketPart;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketParts;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.OptionalDouble;

/**
 * A rocket standing on a pad, flying, parked in orbit or landed. The server runs the flight; clients get
 * the state, active stage, fuel and timer through synced data and draw effects from them.
 * The design never changes; staging only raises {@link #activeStage()}, and once every stage is gone the
 * entity is just the crew capsule.
 */
public class RocketEntity extends Entity implements IEntityWithComplexSpawn {
    public static final int COUNTDOWN_TICKS = 200;
    public static final int MOON_COUNTDOWN_TICKS = 100;
    public static final int IGNITION_TICKS = 60;
    public static final double ORBIT_Y = 160;
    public static final int BURN_TICKS = 60;
    public static final int TRANSFER_TICKS = 120;
    /** Ascent progress by which the rocket stands upright again (the orbit view takes over just after). */
    public static final double LEVEL_OUT_AT = 0.95;
    /** Before a landing, the ground under the landing spot is loaded this many ticks ahead. */
    public static final int PRELOAD_TICKS = 60;
    private static final net.minecraft.server.level.TicketType<Integer> LANDING_TICKET =
            net.minecraft.server.level.TicketType.create("spacebreaker_landing", Integer::compare, PRELOAD_TICKS + 200);
    public static final int STAGING_COAST_TICKS = 24;

    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TIMER = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_DEST = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.STRING);
    /** Body circled while in the shared orbit dimension ("" elsewhere). */
    private static final EntityDataAccessor<String> DATA_ORBIT_BODY = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_BURNING = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_TILT = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.FLOAT);
    /** Mission target of an uncrewed rocket (a location id, empty when none). */
    private static final EntityDataAccessor<String> DATA_MISSION = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.STRING);
    @Nullable
    private java.util.UUID owner;
    private int autoTravel;
    private long dockCheckedAt = Long.MIN_VALUE;
    /** Approaching a station: thrusters puff (synced for the client effects). */
    private static final EntityDataAccessor<Boolean> DATA_DOCKING = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.BOOLEAN);
    /** Cargo route for the console: status (see {@link RouteStatus}) in the low byte, filled hold slots above it. */
    private static final EntityDataAccessor<Integer> DATA_ROUTE = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);

    /** What an automatic cargo route is doing, as the launch console shows it. */
    public enum RouteStatus {
        OFF, LOADING, WAIT_CARGO, WAIT_FUEL, FLYING
    }
    /** Entity event: the docking latches closed (client camera jolt). */
    public static final byte EVENT_DOCKED = 70;
    /** How far out from the port a rocket arriving in orbit starts its approach, blocks. */
    public static final double APPROACH_DISTANCE = 36;
    /** A rocket bringing a station core builds a new station unless a docking port is this close (blocks). */
    public static final double STATION_REACH = 512;
    private static final int JOLT_TICKS = 14;
    /** Where the approach ends, while approaching. */
    @Nullable
    private Vec3 dockTarget;
    /** The port this rocket last docked at (tall rockets reach past the port's block scan). */
    @Nullable
    private BlockPos dockedPort;
    /** Game time of the last propellant taken on (warning lights spin while a rocket is being fuelled). */
    private long lastFilled = -1_000_000L;

    /** Whether propellant went in within the last {@code ticks}. */
    public boolean fuelledRecently(int ticks) {
        return level().getGameTime() - lastFilled < ticks;
    }

    /** Client: ticks left of the docking jolt. */
    private int jolt;
    /** Automatic cargo route: the two ends (location ids), whether it runs, and the wait before the next step. */
    private String routeA = "", routeB = "";
    private boolean routeOn;
    private int routeWait;
    /** Where the rocket loads and unloads at its current end (docking port or landing beacon). */
    @Nullable
    private BlockPos routeAnchor;
    private boolean routeToldFuel;
    /** The hold of a cargo module (27 slots), made when first needed. */
    @Nullable
    private net.minecraft.world.SimpleContainer cargo;
    private boolean dockedCache;
    /** Share of the current ascent burn already flown (0..1), so clients can start the transit view in time. */
    private static final EntityDataAccessor<Float> DATA_ASCENT = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<CompoundTag> DATA_FUEL = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private RocketDesign design = RocketDesign.EMPTY;
    private double[] fuel = new double[0];
    /** RP-1 and LOX pumped in but not yet paired into propellant (kg). */
    private double rp1Kg;
    private double ch4Kg;
    private double h2Kg;
    private double loxKg;

    // flight bookkeeping (server)
    private double legCost;
    private double legDelivered;
    private double timeScale = 1;
    private int ascentFirstStage;
    private int coast;
    private double homeX = Double.NaN;
    /** This flight has been to another world (orbit or surface away from Earth): its Earth landing is a safe return. */
    private boolean beenAway;
    private double homeZ = Double.NaN;
    private int requiredShield;
    private boolean teleporting;
    private int burnStartStage;

    // caches
    private RocketLayout layout;
    private RocketGeometry geometry;
    private int geometryStage = -1;

    public RocketEntity(EntityType<? extends RocketEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static RocketEntity create(Level level, RocketDesign design, Vec3 pos, float yaw) {
        RocketEntity rocket = new RocketEntity(RocketryModule.ROCKET_ENTITY.get(), level);
        rocket.design = design;
        rocket.fuel = new double[rocket.layout().stages().size()];
        rocket.syncFuel();
        rocket.moveTo(pos.x, pos.y, pos.z, yaw, 0);
        rocket.refreshDimensions();
        return rocket;
    }

    // ------------------------------------------------------------------ synced data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STATE, 0);
        builder.define(DATA_STAGE, 0);
        builder.define(DATA_TIMER, 0);
        builder.define(DATA_DEST, Location.EARTH_ORBIT.id());
        builder.define(DATA_ORBIT_BODY, "");
        builder.define(DATA_BURNING, false);
        builder.define(DATA_TILT, 0F);
        builder.define(DATA_ASCENT, 0F);
        builder.define(DATA_MISSION, "");
        builder.define(DATA_DOCKING, false);
        builder.define(DATA_ROUTE, 0);
        builder.define(DATA_FUEL, new CompoundTag());
    }

    public FlightState state() {
        return FlightState.byId(entityData.get(DATA_STATE));
    }

    private void setState(FlightState state) {
        entityData.set(DATA_STATE, state.ordinal());
    }

    public int activeStage() {
        return entityData.get(DATA_STAGE);
    }

    private void setActiveStage(int stage) {
        entityData.set(DATA_STAGE, stage);
        refreshDimensions();
    }

    /** Phase timer in ticks (counts down in COUNTDOWN, up elsewhere). */
    public int timer() {
        return entityData.get(DATA_TIMER);
    }

    private void setTimer(int t) {
        entityData.set(DATA_TIMER, t);
    }

    public Location destination() {
        return Location.parse(entityData.get(DATA_DEST)).orElse(Location.EARTH_SURFACE);
    }

    public boolean burning() {
        return entityData.get(DATA_BURNING);
    }

    private void setBurning(boolean b) {
        entityData.set(DATA_BURNING, b);
    }

    /** Pitch of the gravity turn in degrees (towards +x). */
    /** Who launched it (for uncrewed missions: who gets the probe's samples and the messages). */
    @Nullable
    public java.util.UUID owner() {
        return owner;
    }

    public void setOwner(@Nullable java.util.UUID owner) {
        this.owner = owner;
    }

    public java.util.Optional<Location> missionTarget() {
        String id = entityData.get(DATA_MISSION);
        return id.isEmpty() ? java.util.Optional.empty() : Location.parse(id);
    }

    public void setMissionTarget(@Nullable Location target) {
        entityData.set(DATA_MISSION, target == null ? "" : target.id());
    }

    /** Uncrewed rockets carry a satellite or probe and are flown from the launch console. */
    public boolean isUncrewed() {
        return com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.payload(this).isPresent();
    }

    /** Burn on to the mission target after this many ticks in orbit. */
    public void scheduleTravel(int ticks) {
        autoTravel = Math.max(1, ticks);
    }

    public float ascentProgress() {
        return entityData.get(DATA_ASCENT);
    }

    public float tilt() {
        return entityData.get(DATA_TILT);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_STAGE.equals(key)) {
            refreshDimensions();
        }
        if (DATA_FUEL.equals(key) && level().isClientSide) {
            readFuel(entityData.get(DATA_FUEL));
        }
    }

    private void syncFuel() {
        CompoundTag tag = new CompoundTag();
        writeFuel(tag);
        entityData.set(DATA_FUEL, tag);
    }

    private void writeFuel(CompoundTag tag) {
        ListTag list = new ListTag();
        for (double f : fuel) {
            CompoundTag e = new CompoundTag();
            e.putDouble("kg", f);
            list.add(e);
        }
        tag.put("stages", list);
        tag.putDouble("rp1", rp1Kg);
        tag.putDouble("ch4", ch4Kg);
        tag.putDouble("h2", h2Kg);
        tag.putDouble("lox", loxKg);
    }

    private void readFuel(CompoundTag tag) {
        ListTag list = tag.getList("stages", Tag.TAG_COMPOUND);
        fuel = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            fuel[i] = list.getCompound(i).getDouble("kg");
        }
        rp1Kg = tag.getDouble("rp1");
        ch4Kg = tag.getDouble("ch4");
        h2Kg = tag.getDouble("h2");
        loxKg = tag.getDouble("lox");
    }

    // ------------------------------------------------------------------ design, layout, geometry

    public RocketDesign design() {
        return design;
    }

    public RocketLayout layout() {
        if (layout == null) {
            layout = RocketLayout.of(design, RocketParts.lookup(level().registryAccess()));
            if (fuel.length != layout.stages().size()) {
                double[] f = new double[layout.stages().size()];
                System.arraycopy(fuel, 0, f, 0, Math.min(fuel.length, f.length));
                fuel = f;
            }
        }
        return layout;
    }

    public RocketGeometry geometry() {
        int stage = activeStage();
        if (geometry == null || geometryStage != stage) {
            geometry = RocketGeometry.ofActive(layout(), stage);
            geometryStage = stage;
        }
        return geometry;
    }

    public double[] fuel() {
        return fuel;
    }

    public double fuelFraction() {
        RocketLayout l = layout();
        double cap = 0, have = 0;
        for (int i = activeStage(); i < l.stages().size(); i++) {
            cap += l.stages().get(i).propellantKg();
            have += fuel[i];
        }
        return cap <= 0 ? 0 : have / cap;
    }

    public boolean fullyFuelled() {
        return fuelFraction() > 0.999;
    }

    /** Δv left from the active stage up (vacuum, or the ascent blend on an atmospheric surface). */
    public double deltaV() {
        Location here = location();
        double atmosphere = !here.orbit() && here.atmosphere() ? RocketMath.ASCENT_ATMOSPHERE : 0;
        return RocketMath.totalDeltaV(layout(), activeStage(), fuel, atmosphere);
    }

    public Location location() {
        String ob = entityData.get(DATA_ORBIT_BODY);
        return com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.at(level().dimension(),
                ob.isEmpty() ? null : net.minecraft.resources.ResourceLocation.tryParse(ob)).orElse(Location.EARTH_SURFACE);
    }

    public boolean isCapsuleOnly() {
        return activeStage() >= layout().stages().size();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (design.isEmpty()) {
            return super.getDimensions(pose);
        }
        double h = Math.max(1.0, geometry().height() / 16.0);
        return EntityDimensions.fixed(1.8F, (float) h);
    }

    /** Client only: whether this rocket was seen under parachutes, and the tick it touched down (for the collapsing canopy). */
    public transient boolean chuteSeen;
    public transient int chuteLandedAt = -1;

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        double seat = (geometry().capsuleBottom() + 3) / 16.0;
        double tilt = Math.toRadians(tilt());
        // crew of more than one sit side by side across the capsule
        int seats = layout().payload().seats();
        int index = Math.max(0, getPassengers().indexOf(passenger));
        double side = seats > 1 ? (index - (Math.min(seats, getPassengers().size()) - 1) / 2.0) * 0.5 : 0;
        Vec3 across = new Vec3(0, 0, side).yRot((float) Math.toRadians(-getYRot()));
        return new Vec3(Math.sin(tilt) * seat + across.x, Math.cos(tilt) * seat - 0.35, across.z);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < Math.max(1, layout().payload().seats());
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    /** Whether the crew may climb out right now (never in flight: there is nothing to stand on). */
    public boolean canDismount() {
        return teleporting || state() == FlightState.IDLE || state() == FlightState.COUNTDOWN && timer() > IGNITION_TICKS
                || state() == FlightState.ORBIT && docked();
    }

    /** In orbit next to a station's docking port: the crew may step out into the station and back in. */
    public boolean docked() {
        if (state() != FlightState.ORBIT || docking()) {
            return false;
        }
        long now = level().getGameTime();
        if (now != dockCheckedAt) {
            // a block scan around the rocket: cheap enough once a tick, and pipes may ask many times per tick
            dockCheckedAt = now;
            double reach = com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPortBlock.RANGE + getBbHeight();
            dockedCache = dockedPort != null && level().getBlockState(dockedPort).is(com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule.DOCKING_PORT.get())
                    && dockedPort.getCenter().distanceToSqr(position()) < reach * reach
                    || com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPortBlockEntity.nearPort(this);
        }
        return dockedCache;
    }

    /** A station module still to hand over (delivered modules stay on the rocket's design but count no more). */
    private boolean moduleDelivered;

    /** The station module this rocket carries and has not delivered yet. */
    public java.util.Optional<com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule> carriedModule() {
        if (design.isEmpty() || moduleDelivered) {
            return java.util.Optional.empty();
        }
        return com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.payloadId(this)
                .flatMap(com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule::byPart);
    }

    /**
     * Docked with a module on board: it goes into the station core's console (a core module that just built its
     * station is used up by that). Without a console in reach it is set down by the port.
     */
    private void deliverModule(BlockPos port) {
        var module = carriedModule().orElse(null);
        if (module == null || !(level() instanceof ServerLevel server)) {
            return;
        }
        moduleDelivered = true;
        if (coreBuiltHere) {
            coreBuiltHere = false;
            tellOwner(Component.translatable("message.spacebreaker.station_module.core_built").withStyle(ChatFormatting.GREEN));
            return;
        }
        ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                com.pizzaparker07.spacebreaker.Spacebreaker.id(module.part())));
        var core = com.pizzaparker07.spacebreaker.content.rocketry.station.StationCoreBlockEntity.near(server, port);
        if (core != null && core.store(stack)) {
            tellOwner(Component.translatable("message.spacebreaker.station_module.delivered", stack.getHoverName()).withStyle(ChatFormatting.GREEN));
        } else {
            net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(server,
                    port.getX() + 0.5, port.getY() + 1.5, port.getZ() + 0.5, stack);
            drop.setNoGravity(true);
            drop.setDeltaMovement(Vec3.ZERO);
            server.addFreshEntity(drop);
            tellOwner(Component.translatable("message.spacebreaker.station_module.no_console", stack.getHoverName()).withStyle(ChatFormatting.GOLD));
        }
    }

    /** Set when a core module built a new station on arrival, until the rocket docks at it. */
    private boolean coreBuiltHere;

    private void tellOwner(Component message) {
        notifyCrew(message);
        if (isUncrewed() && level() instanceof ServerLevel server && owner() != null) {
            ServerPlayer p = server.getServer().getPlayerList().getPlayer(owner());
            if (p != null) {
                p.displayClientMessage(message, false);
            }
        }
    }

    /** Whether this rocket carries a cargo module. */
    public boolean hasCargoHold() {
        return !design.isEmpty() && com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.payload(this)
                .map(p -> p.type() == com.pizzaparker07.spacebreaker.content.rocketry.part.PartType.CARGO).orElse(false);
    }

    /**
     * Sets up a cargo route between where the rocket is launched from and its mission target. It only runs between
     * two places away from Earth's surface (a station and a base, say): there the rocket comes down on a beacon.
     */
    public void startRoute(Location from, Location to) {
        routeA = from.id();
        routeB = to.id();
        routeOn = hasCargoHold() && com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.routeAllowed(from, to);
        routeToldFuel = false;
    }

    public boolean routeOn() {
        return routeOn;
    }

    /** The other end of the route from {@code at}, if {@code at} is one of its ends. */
    public java.util.Optional<Location> routeOtherEnd(Location at) {
        if (at.id().equals(routeA)) {
            return Location.parse(routeB);
        }
        if (at.id().equals(routeB)) {
            return Location.parse(routeA);
        }
        return java.util.Optional.empty();
    }

    /** Runs the next route step in {@code ticks}, from this anchor. */
    public void scheduleRoute(int ticks, @Nullable BlockPos anchor) {
        routeWait = Math.max(1, ticks);
        if (anchor != null) {
            routeAnchor = anchor.immutable();
        }
    }

    @Nullable
    public BlockPos routeAnchor() {
        return routeAnchor;
    }

    /** Tells the owner once per wait that the route is held up for fuel. */
    public boolean firstFuelWarning() {
        boolean first = !routeToldFuel;
        routeToldFuel = true;
        return first;
    }

    public void fuelledForRoute() {
        routeToldFuel = false;
    }

    public RouteStatus routeStatus() {
        int v = entityData.get(DATA_ROUTE) & 0xFF;
        return v < RouteStatus.values().length ? RouteStatus.values()[v] : RouteStatus.OFF;
    }

    /** Filled slots of the hold, as last synced. */
    public int holdFilled() {
        return entityData.get(DATA_ROUTE) >>> 8;
    }

    private void syncRoute() {
        if (!hasCargoHold()) {
            return;
        }
        int filled = 0;
        for (int i = 0; i < cargo().getContainerSize(); i++) {
            filled += cargo().getItem(i).isEmpty() ? 0 : 1;
        }
        RouteStatus st = !routeOn ? RouteStatus.OFF
                : routeWait <= 0 ? RouteStatus.FLYING
                : routeToldFuel ? RouteStatus.WAIT_FUEL
                : filled == 0 ? RouteStatus.WAIT_CARGO
                : RouteStatus.LOADING;
        int v = st.ordinal() | filled << 8;
        if (entityData.get(DATA_ROUTE) != v) {
            entityData.set(DATA_ROUTE, v);
        }
    }

    private void tickRoute() {
        if (routeOn && routeWait > 0 && --routeWait == 0) {
            com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.routeDepart(this);
        }
    }

    /** The cargo hold (27 slots); empty for rockets without a cargo module. */
    public net.minecraft.world.SimpleContainer cargo() {
        if (cargo == null) {
            cargo = new net.minecraft.world.SimpleContainer(27);
        }
        return cargo;
    }

    /** A crewed capsule is a pressurised cabin: whoever sits in it breathes without a suit. */
    public boolean hasCabin() {
        return !design.isEmpty() && layout().payload().seats() > 0;
    }

    /**
     * Oxygen source for {@code Environment}: air at this block if the eyes of someone seated in a crewed capsule
     * are there. Climbing out (into space, or a station) leaves the cabin air behind.
     */
    public static boolean cabinAir(Level level, BlockPos pos) {
        for (net.minecraft.world.entity.player.Player p : level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                new net.minecraft.world.phys.AABB(pos).inflate(0.1))) {
            if (p.getVehicle() instanceof RocketEntity r && r.hasCabin() && BlockPos.containing(p.getEyePosition()).equals(pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Where a descent from orbit starts: straight above the nearest landing beacon of that world if it has one
     * (a base to come home to), otherwise {@code fallback}. The height of {@code fallback} is kept.
     */
    /** How far winds can carry a descent through air off its landing beacon, blocks (airless descents are exact). */
    public static final double LANDING_DRIFT = 6;
    /** Dust storm strength above which mission control warns against a launch. */
    public static final float STORM_WARNING = 0.3F;

    public static Vec3 landingSpot(ServerLevel level, Vec3 fallback) {
        return com.pizzaparker07.spacebreaker.content.rocketry.station.LandingBeacons.get(level).nearest(fallback)
                .map(b -> new Vec3(b.getX() + 0.5, fallback.y, b.getZ() + 0.5)).orElse(fallback);
    }

    /** Flying in towards a docking port. */
    public boolean docking() {
        return entityData.get(DATA_DOCKING);
    }

    /** Client: docking jolt strength, 1 at the latch, fading to 0. */
    public float jolt(float partialTick) {
        return jolt <= 0 ? 0F : Mth.clamp((jolt - partialTick) / JOLT_TICKS, 0F, 1F);
    }

    /**
     * Where the rocket (its base) parks at a port: on top of an upward port, under a downward one, and beside a
     * sideways one with the capsule level with the hatch.
     */
    public static Vec3 dockPoint(BlockPos port, Direction facing, double height, double width) {
        Vec3 c = port.getCenter();
        return switch (facing) {
            case UP -> new Vec3(c.x, port.getY() + 2, c.z);
            case DOWN -> new Vec3(c.x, port.getY() - 1 - height, c.z);
            default -> c.add(Vec3.atLowerCornerOf(facing.getNormal()).scale(1.5 + width / 2)).add(0, 1.0 - height, 0);
        };
    }

    /** Places the rocket {@code distance} blocks out from the port and starts the approach. */
    public void approachPort(BlockPos port, Direction facing, double distance) {
        Vec3 end = dockPoint(port, facing, getBbHeight(), getBbWidth());
        Vec3 start = end.add(Vec3.atLowerCornerOf(facing.getNormal()).scale(distance));
        moveTo(start.x, start.y, start.z);
        setDeltaMovement(Vec3.ZERO);
        dockTarget = end;
        dockedPort = port.immutable();
        entityData.set(DATA_DOCKING, true);
    }

    /** Arrived in orbit: if a station is in this orbit, fly to its nearest docking port. */
    private void seekStation(Location to) {
        if (!(level() instanceof ServerLevel server) || isUncrewed() && !hasCargoHold() && carriedModule().isEmpty()) {
            return;
        }
        boolean shared = Location.Orbits.SHARED.equals(to.dimension());
        var ports = com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPorts.get(server);
        boolean nearby = ports.nearest(position(), p -> (!shared
                || com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.bodyForSharedX(p.pos().getX()).filter(to.body()::equals).isPresent())
                && p.pos().getCenter().distanceTo(position()) < STATION_REACH).isPresent();
        if (!nearby && carriedModule().orElse(null) == com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.CORE) {
            // the first module of a new station: unfold the core ahead of the rocket and dock on its roof
            BlockPos centre = blockPosition().offset(24, -4, 0);
            String who = owner() == null ? "" : java.util.Optional.ofNullable(server.getServer().getProfileCache())
                    .flatMap(c -> c.get(owner())).map(com.mojang.authlib.GameProfile::getName).orElse("");
            if (com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.buildCore(server, centre, Direction.EAST, who)) {
                coreBuiltHere = true;
                approachPort(com.pizzaparker07.spacebreaker.content.rocketry.station.StationModule.corePort(centre), Direction.UP, APPROACH_DISTANCE);
                notifyCrew(Component.translatable("message.spacebreaker.docking.approach"));
                return;
            }
        }
        com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPorts.get(server).nearest(position(), p -> !shared
                        || com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.bodyForSharedX(p.pos().getX()).filter(to.body()::equals).isPresent())
                .ifPresent(p -> {
                    approachPort(p.pos(), p.facing(), APPROACH_DISTANCE);
                    notifyCrew(Component.translatable("message.spacebreaker.docking.approach"));
                });
    }

    private void tickDocking() {
        Vec3 to = dockTarget.subtract(position());
        double d = to.length();
        if (d < 0.06) {
            setPos(dockTarget.x, dockTarget.y, dockTarget.z);
            dockTarget = null;
            entityData.set(DATA_DOCKING, false);
            dockCheckedAt = Long.MIN_VALUE;
            BlockPos at = dockedPort != null ? dockedPort : blockPosition();
            level().playSound(null, at, SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 1.6F, 0.55F);
            level().playSound(null, at, SoundEvents.PISTON_CONTRACT, SoundSource.NEUTRAL, 1.0F, 0.6F);
            level().broadcastEntityEvent(this, EVENT_DOCKED);
            notifyCrew(Component.translatable("message.spacebreaker.docked"));
            deliverModule(at);
            if (hasCargoHold()) {
                com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.unloadCargo(this, at, location());
                com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.routeArrived(this, at, location());
            }
            return;
        }
        // fast from far out, then an ever slower creep for the last metres
        double step = Math.min(d, Mth.clamp(d * 0.05, 0.04, 1.0));
        Vec3 next = position().add(to.scale(step / d));
        setPos(next.x, next.y, next.z);
        if (tickCount % 12 == 0) {
            level().playSound(null, blockPosition().above((int) getBbHeight()), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.35F, 1.7F);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_DOCKED) {
            jolt = JOLT_TICKS;
        } else {
            super.handleEntityEvent(id);
        }
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(RocketryModule.PROPELLANT_CARTRIDGE.get())) {
            if (!level().isClientSide && state() == FlightState.IDLE) {
                fillCompletely();
                level().playSound(null, blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 1F, 0.6F);
                player.displayClientMessage(Component.translatable("message.spacebreaker.rocket.fuelled"), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        // buckets, canisters and any other item fluid container holding RP-1 or liquid oxygen
        if (state() == FlightState.IDLE && !held.isEmpty()) {
            var content = net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(held);
            if (content.isPresent() && Propellant.isRocketLiquid(content.get())) {
                if (!level().isClientSide) {
                    var result = net.neoforged.neoforge.fluids.FluidUtil.tryEmptyContainer(held, new RocketFluidHandler(this),
                            Integer.MAX_VALUE, player, true);
                    if (result.isSuccess()) {
                        player.setItemInHand(hand, result.getResult());
                        player.displayClientMessage(Component.translatable("message.spacebreaker.rocket.loaded", content.get().getHoverName()), true);
                    } else {
                        player.displayClientMessage(Component.translatable("message.spacebreaker.rocket.tank_full", content.get().getHoverName()), true);
                    }
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        if (hasCargoHold() && player.isSecondaryUseActive() && held.isEmpty() && !routeA.isEmpty()
                && (state() == FlightState.IDLE || docked())) {
            // sneak-use with an empty hand switches the automatic route on and off
            if (!level().isClientSide) {
                routeOn = !routeOn && Location.parse(routeA).flatMap(a -> Location.parse(routeB).map(b ->
                        com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.routeAllowed(a, b))).orElse(false);
                if (routeOn) {
                    scheduleRoute(com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.ROUTE_WAIT, null);
                }
                player.displayClientMessage(Component.translatable(routeOn ? "message.spacebreaker.cargo.route_on" : "message.spacebreaker.cargo.route_off"), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (hasCargoHold() && !player.isSecondaryUseActive() && (state() == FlightState.IDLE || docked())) {
            // open the cargo hold (on the pad to load it, at the station or base to unload)
            if (!level().isClientSide) {
                player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> net.minecraft.world.inventory.ChestMenu.threeRows(id, inv, cargo()),
                        Component.translatable("container.spacebreaker.cargo_hold")));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isSecondaryUseActive() || player.getVehicle() == this) {
            return InteractionResult.PASS;
        }
        if (state() != FlightState.IDLE && state() != FlightState.COUNTDOWN && !docked()) {
            return InteractionResult.PASS;
        }
        if (layout().payload().seats() <= 0) {
            if (!level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.spacebreaker.rocket.no_seat"), true);
            }
            return InteractionResult.CONSUME;
        }
        if (!level().isClientSide) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || isInvulnerableTo(source)) {
            return false;
        }
        if (!(source.getEntity() instanceof Player player) || state() != FlightState.IDLE || isVehicle()) {
            return false;
        }
        if (!player.isSecondaryUseActive()) {
            player.displayClientMessage(Component.translatable("message.spacebreaker.rocket.pick_up_hint"), true);
            return false;
        }
        // pick the rocket back up (unflown rockets return as a rocket item; flown capsules as a capsule)
        ItemStack drop;
        if (activeStage() == 0) {
            drop = RocketItem.of(design);
        } else {
            drop = new ItemStack(hasCargoHold() ? RocketryModule.CARGO_MODULE.get() : RocketryModule.CREW_CAPSULE.get());
        }
        if (cargo != null) {
            net.minecraft.world.Containers.dropContents(level(), blockPosition(), cargo);
        }
        if (!player.getAbilities().instabuild || activeStage() > 0) {
            spawnAtLocation(drop, 1F);
        }
        discard();
        return true;
    }

    /** Fills every remaining stage (creative propellant cartridge). */
    public void fillCompletely() {
        RocketLayout l = layout();
        for (int i = 0; i < fuel.length; i++) {
            fuel[i] = i >= activeStage() ? l.stages().get(i).propellantKg() : 0;
        }
        rp1Kg = 0;
        ch4Kg = 0;
        h2Kg = 0;
        loxKg = 0;
        syncFuel();
    }

    /** How often a rocket waiting in orbit loses its boil-off, ticks. */
    public static final int BOIL_OFF_TICKS = 1200;

    /** Boils off {@code days} worth of cryogenic propellant from the stages still on the rocket. Returns kg lost. */
    public double boilOff(double days) {
        if (days <= 0) {
            return 0;
        }
        double lost = com.pizzaparker07.spacebreaker.content.rocketry.design.BoilOff.apply(layout(), activeStage(), fuel, days, SbConfig.realism());
        if (lost > 0) {
            syncFuel();
        }
        return lost;
    }

    /** Pump liquid in (mB of RP-1 or LOX, kerolox stages only). Returns the amount accepted. */
    public int fill(boolean oxidiser, int millibuckets, boolean simulate) {
        return fill(oxidiser ? null : Propellant.KEROLOX, millibuckets, simulate);
    }

    /** Pump in any rocket liquid: a fuel (RP-1, liquid methane) or liquid oxygen. Returns the amount accepted. */
    public int fill(net.neoforged.neoforge.fluids.FluidStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(Propellant.LOX_TAG)) {
            return fill((Propellant) null, stack.getAmount(), simulate);
        }
        Propellant p = Propellant.forFuel(stack);
        return p == null ? 0 : fill(p, stack.getAmount(), simulate);
    }

    private double buffer(Propellant p) {
        return switch (p) {
            case METHALOX -> ch4Kg;
            case HYDROLOX, NUCLEAR -> h2Kg;
            default -> rp1Kg;
        };
    }

    private void addBuffer(Propellant p, double kg) {
        switch (p) {
            case METHALOX -> ch4Kg += kg;
            case HYDROLOX, NUCLEAR -> h2Kg += kg;
            default -> rp1Kg += kg;
        }
    }

    /**
     * Pump in mB of a stage fuel ({@code fuel}) or of liquid oxygen ({@code fuel == null}). Each stage takes the
     * fuel its engine burns; fuel and oxygen are paired into propellant at that engine's mixture ratio and
     * stored from the lowest stage up. Returns the amount accepted.
     */
    public int fill(@Nullable Propellant fuel, int millibuckets, boolean simulate) {
        if (state() != FlightState.IDLE && state() != FlightState.COUNTDOWN && !docked() || millibuckets <= 0) {
            return 0;
        }
        RocketLayout l = layout();
        double roomKg = 0;
        for (int i = activeStage(); i < fuel().length; i++) {
            RocketLayout.Stage s = l.stages().get(i);
            double space = s.propellantKg() - this.fuel[i];
            if (fuel == null) {
                roomKg += space * (1 - s.propellant().fuelFraction());
            } else if (s.propellant().takes(fuel)) {
                roomKg += space * s.propellant().fuelFraction();
            }
        }
        double density = fuel == null ? Propellant.LOX_DENSITY : fuel.fuelDensity();
        roomKg = Math.max(0, roomKg - (fuel == null ? loxKg : buffer(fuel)));
        int accepted = (int) Math.min(millibuckets, Math.floor(roomKg / density));
        if (accepted <= 0 || simulate) {
            return Math.max(0, accepted);
        }
        lastFilled = level().getGameTime();
        if (fuel == null) {
            loxKg += accepted * density;
        } else {
            addBuffer(fuel, accepted * density);
        }
        for (int i = activeStage(); i < this.fuel.length; i++) {
            RocketLayout.Stage s = l.stages().get(i);
            Propellant p = s.propellant();
            double f = p.fuelFraction();
            // a nuclear stage burns hydrogen alone: nothing to pair with oxygen
            double pairs = p.needsOxygen() ? Math.min(buffer(p) / f, loxKg / (1 - f)) : buffer(p);
            double add = Math.min(s.propellantKg() - this.fuel[i], pairs);
            if (add <= 1e-9) {
                continue;
            }
            this.fuel[i] += add;
            addBuffer(p, -add * f);
            loxKg -= add * (1 - f);
        }
        syncFuel();
        return accepted;
    }

    /** Space left for each liquid, in mB (RP-1 or LOX). */
    public int roomFor(boolean oxidiser) {
        return fill(oxidiser, Integer.MAX_VALUE, true);
    }

    /** Space left for a stage fuel ({@code null}: liquid oxygen), in mB. */
    public int roomFor(@Nullable Propellant fuel) {
        return fill(fuel, Integer.MAX_VALUE, true);
    }

    // ------------------------------------------------------------------ crew commands

    /** Star map choice (by location id). */
    public void handleTravel(ServerPlayer player, String target) {
        Location.parse(target).ifPresent(to -> tryTravel(player, to));
    }

    public void handleCrewAction(ServerPlayer player, int action, int arg) {
        switch (action) {
            case RocketActionPayload.LAUNCH -> {
                if (state() == FlightState.COUNTDOWN) {
                    abortCountdown(player);
                } else {
                    tryStartCountdown(player);
                }
            }
            case RocketActionPayload.ABORT -> abortCountdown(player);
            case RocketActionPayload.TRAVEL -> { }
            default -> {
            }
        }
    }

    /** Why the rocket cannot launch from here right now, or null when it can. */
    @Nullable
    public Component launchProblem() {
        if (state() != FlightState.IDLE) {
            return Component.translatable("message.spacebreaker.launch.busy");
        }
        if (layout().stages().isEmpty() || !layout().problems().isEmpty()) {
            return Component.translatable("message.spacebreaker.launch.no_stage");
        }
        Location here = location();
        Location to = launchTarget(here);
        if (to == null) {
            return Component.translatable("message.spacebreaker.launch.nowhere");
        }
        if (here.equals(Location.EARTH_SURFACE) && LaunchPad.padUnder(level(), position()) == null) {
            return Component.translatable("message.spacebreaker.launch.no_pad");
        }
        double g = Environment.gravity(level());
        double twr = RocketMath.twr(layout(), activeStage(), fuel, g);
        if (twr < 1.05) {
            return Component.translatable("message.spacebreaker.launch.twr", String.format(java.util.Locale.ROOT, "%.2f", twr));
        }
        OptionalDouble cost = Transfers.cost(here, to, layout(), activeStage(), SbConfig.realism());
        if (cost.isEmpty() || deltaV() < cost.getAsDouble()) {
            return Component.translatable("message.spacebreaker.launch.delta_v",
                    String.format(java.util.Locale.ROOT, "%.0f", deltaV()), String.format(java.util.Locale.ROOT, "%.0f", cost.orElse(0)));
        }
        return null;
    }

    @Nullable
    public static Location launchTarget(Location here) {
        return here.orbit() ? null : com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(here.body(), true).orElse(null);
    }

    public boolean tryStartCountdown(@Nullable ServerPlayer by) {
        Component problem = launchProblem();
        if (problem != null) {
            if (by != null) {
                by.displayClientMessage(problem.copy().withStyle(ChatFormatting.RED), false);
            }
            return false;
        }
        if (!MissionPlanner.canReturnAfter(layout(), activeStage(), fuel, location(), launchTarget(location()), SbConfig.realism()) && by != null) {
            by.displayClientMessage(Component.translatable("message.spacebreaker.warn.no_return").withStyle(ChatFormatting.GOLD), false);
        }
        if (by != null) {
            owner = by.getUUID();
        }
        float storm = com.pizzaparker07.spacebreaker.content.mars.MarsWeather.dusty(level())
                ? com.pizzaparker07.spacebreaker.content.mars.MarsWeather.stormStrength(level()) : 0F;
        if (storm > STORM_WARNING) {
            // a launch into a dust storm is allowed, but mission control says what it thinks of it
            notifyCrew(Component.translatable("message.spacebreaker.warn.dust_storm_launch").withStyle(ChatFormatting.GOLD));
        }
        setState(FlightState.COUNTDOWN);
        setTimer(location().equals(Location.EARTH_SURFACE) ? COUNTDOWN_TICKS : MOON_COUNTDOWN_TICKS);
        if (Double.isNaN(homeX) && location().equals(Location.EARTH_SURFACE)) {
            homeX = getX();
            homeZ = getZ();
        }
        return true;
    }

    public void abortCountdown(@Nullable ServerPlayer by) {
        if (state() == FlightState.COUNTDOWN && timer() > 0) {
            setState(FlightState.IDLE);
            setBurning(false);
            setTimer(0);
            if (by != null) {
                by.displayClientMessage(Component.translatable("message.spacebreaker.launch.aborted"), true);
            }
        }
    }

    /** Validates and starts a transfer from an orbit (chosen on the star map). */
    public boolean tryTravel(@Nullable ServerPlayer by, Location to) {
        Location here = location();
        if (state() != FlightState.ORBIT) {
            return false;
        }
        OptionalDouble cost = Transfers.cost(here, to, layout(), activeStage(), SbConfig.realism());
        if (cost.isEmpty()) {
            tell(by, Component.translatable("message.spacebreaker.travel.no_route").withStyle(ChatFormatting.RED));
            return false;
        }
        if (to.dimension() != null && level().getServer().getLevel(to.dimension()) == null) {
            tell(by, Component.translatable("message.spacebreaker.travel.unavailable",
                    to.name()).withStyle(ChatFormatting.RED));
            return false;
        }
        int rating = Transfers.reentryRating(here, to);
        if (rating > layout().payload().heatShield()) {
            tell(by, Component.translatable("message.spacebreaker.warn.heat_shield").withStyle(ChatFormatting.GOLD));
        }
        boolean reentry = to.isEarth() && !to.orbit();
        RocketMath.Burn burn = RocketMath.burn(layout(), activeStage(), fuel, cost.getAsDouble(), 0);
        if (!burn.complete()) {
            tell(by, Component.translatable("message.spacebreaker.travel.delta_v",
                    String.format(java.util.Locale.ROOT, "%.0f", deltaV()), String.format(java.util.Locale.ROOT, "%.0f", cost.getAsDouble())).withStyle(ChatFormatting.RED));
            return false;
        }
        burnStartStage = activeStage();
        fuel = burn.fuel();
        for (int s : burn.separated()) {
            spawnSpentStage(s, true);
        }
        setActiveStage(Math.min(burn.activeStage(), layout().stages().size()));
        syncFuel();
        requiredShield = rating;
        entityData.set(DATA_DEST, to.id());
        setState(FlightState.BURN);
        setBurning(!reentry || !isCapsuleOnly());
        setTimer(0);
        level().playSound(null, blockPosition(), com.pizzaparker07.spacebreaker.content.audio.SbSounds.ROCKET_IGNITION.get(), SoundSource.NEUTRAL, 2F, 1.1F);
        return true;
    }

    /** Message to whoever commands the rocket: the player who asked, otherwise the crew and the owner. */
    private void tell(@Nullable ServerPlayer by, Component message) {
        if (by != null) {
            by.displayClientMessage(message, false);
        } else {
            notifyCrew(message);
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (design.isEmpty()) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }
        if (level().isClientSide) {
            if (jolt > 0) {
                jolt--;
            }
            if (docking() && tickCount % 3 == 0) {
                // attitude thrusters puffing around the capsule
                double y = getY() + getBbHeight() - 1.2;
                for (int i = 0; i < 2; i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    double r = getBbWidth() / 2 + 0.1;
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.POOF, getX() + Math.cos(a) * r, y, getZ() + Math.sin(a) * r,
                            Math.cos(a) * 0.08, 0, Math.sin(a) * 0.08);
                }
            }
            return;
        }
        teleporting = false;
        if (tickCount % 20 == 0) {
            syncRoute();
        }
        Vec3 v = getDeltaMovement();
        if (v.lengthSqr() > 0) {
            setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        }
        switch (state()) {
            case IDLE -> {
                setDeltaMovement(Vec3.ZERO);
                tickRoute();
            }
            case COUNTDOWN -> tickCountdown();
            case ASCENT -> tickAscent();
            case ORBIT -> {
                setDeltaMovement(Vec3.ZERO);
                // waiting in orbit, cryogenic propellant slowly boils off (a game day is a day)
                if (tickCount % BOIL_OFF_TICKS == 0) {
                    boilOff((double) BOIL_OFF_TICKS / 24_000.0);
                }
                if (dockTarget != null) {
                    tickDocking();
                } else if (docked()) {
                    tickRoute();
                }
                if (autoTravel > 0 && --autoTravel == 0) {
                    Location target = missionTarget().orElse(null);
                    if (target == null || !tryTravel(null, target)) {
                        com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.deploy(this, location());
                    }
                }
            }
            case BURN -> tickBurn();
            case TRANSFER -> tickTransfer();
            case DESCENT -> tickDescent();
            case REENTRY -> tickReentry();
            case PARACHUTE -> tickParachute();
        }
    }

    private void tickCountdown() {
        int t = timer() - 1;
        setTimer(t);
        if (t % 20 == 0 && t > 0) {
            level().playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.NEUTRAL, 1.5F, t <= IGNITION_TICKS ? 1.4F : 1.0F);
        }
        if (t == IGNITION_TICKS) {
            setBurning(true);
            level().playSound(null, blockPosition(), com.pizzaparker07.spacebreaker.content.audio.SbSounds.ROCKET_IGNITION.get(), SoundSource.NEUTRAL, 4F, 1F);
        }
        if (t <= 0) {
            beginAscent();
        }
    }

    private void beginAscent() {
        Location here = location();
        Location to = launchTarget(here);
        OptionalDouble cost = to == null ? OptionalDouble.empty() : Transfers.cost(here, to, layout(), activeStage(), SbConfig.realism());
        if (cost.isEmpty()) {
            setState(FlightState.IDLE);
            setBurning(false);
            return;
        }
        legCost = cost.getAsDouble();
        legDelivered = 0;
        entityData.set(DATA_ASCENT, 0F);
        ascentFirstStage = activeStage();
        double atmosphere = !here.orbit() && here.atmosphere() ? RocketMath.ASCENT_ATMOSPHERE : 0;
        // run the whole burn once to learn how long it really takes, then compress it to a watchable ascent
        double realSeconds = 0;
        double[] f = fuel.clone();
        RocketMath.Burn burn = RocketMath.burn(layout(), activeStage(), f, legCost, atmosphere);
        for (int s = activeStage(); s <= Math.min(burn.activeStage(), layout().stages().size() - 1); s++) {
            double used = fuel[s] - burn.fuel()[s];
            realSeconds += RocketMath.burnTime(layout().stages().get(s), used, s == activeStage() ? atmosphere : 0);
        }
        double watch = here.atmosphere() ? 30 : 14;
        timeScale = Math.max(1, realSeconds / watch);
        entityData.set(DATA_DEST, to.id());
        setState(FlightState.ASCENT);
        setTimer(0);
        setBurning(true);
        coast = 0;
        award(com.pizzaparker07.spacebreaker.content.research.Milestones.LAUNCH);
        LaunchPad.Result pad = LaunchPad.padUnder(level(), position());
        if (pad != null) {
            LaunchPad.onLiftoff(level(), pad);
        }
    }

    /** Shortest climb shown, ticks: on a tiny asteroid the burn to orbit is a nudge, but the rocket still lifts off. */
    public static final int MIN_ASCENT_TICKS = 100;

    /** The climb to orbit is over when its Δv is delivered and it has been seen to fly for a few seconds. */
    public static boolean ascentDone(double delivered, double cost, int ticks) {
        return delivered >= cost - 1e-6 && ticks >= MIN_ASCENT_TICKS;
    }

    private void tickAscent() {
        int t = timer() + 1;
        setTimer(t);
        RocketLayout l = layout();
        int s = activeStage();
        double g = Environment.gravity(level());
        Vec3 v = getDeltaMovement();
        double vy = v.y;
        if (coast > 0) {
            coast--;
            setBurning(coast == 0);
            vy = Math.max(0.2, vy - 0.004);
        } else if (s < l.stages().size() && l.stages().get(s).hasEngine() && fuel[s] > 1e-6) {
            RocketLayout.Stage stage = l.stages().get(s);
            double atmosphere = s == ascentFirstStage && location().atmosphere() ? RocketMath.ASCENT_ATMOSPHERE : 0;
            double isp = stage.isp(atmosphere);
            double m0 = RocketMath.mass(l, s, fuel);
            double flow = stage.thrustKn() * 1000.0 / (isp * RocketPart.G0);
            double dm = Math.min(fuel[s], flow * timeScale / 20.0);
            double dv = RocketMath.deltaV(isp, m0, m0 - dm);
            if (legDelivered + dv >= legCost) {
                dm = RocketMath.propellantFor(isp, m0, legCost - legDelivered);
                dv = legCost - legDelivered;
            }
            fuel[s] = Math.max(0, fuel[s] - dm);
            legDelivered += dv;
            double twr = RocketMath.twr(l, s, fuel, g);
            double accel = Mth.clamp(0.004 + 0.008 * (twr - 1), 0.003, 0.03);
            vy = Math.min(5.0, vy + accel);
            if (t % 10 == 0) {
                syncFuel();
            }
        } else if (s < l.stages().size()) {
            // stage dry: separate it and coast a moment before the next one lights
            spawnSpentStage(s, false);
            setActiveStage(s + 1);
            setBurning(false);
            coast = STAGING_COAST_TICKS;
            level().playSound(null, blockPosition(), com.pizzaparker07.spacebreaker.content.audio.SbSounds.ROCKET_STAGE_SEPARATION.get(), SoundSource.NEUTRAL, 3F, 1F);
            if (s + 1 >= l.stages().size()) {
                failAscent();
                return;
            }
        } else {
            failAscent();
            return;
        }
        // gentle gravity turn on Earth so it looks like a real ascent
        float tilt = location().atmosphere() ? (float) Mth.clamp((t - 200) * 0.03, 0, 14) : 0;
        // the last part of the climb levels out, so the rocket stands as it will in orbit when the view hands over
        tilt *= (float) Mth.clamp((LEVEL_OUT_AT - ascentProgress()) / 0.1, 0, 1);
        entityData.set(DATA_TILT, tilt);
        double vx = vy * Math.sin(Math.toRadians(tilt));
        setDeltaMovement(vx, vy, 0);
        if (legCost > 0 && t % 2 == 0) {
            entityData.set(DATA_ASCENT, (float) Mth.clamp(legDelivered / legCost, 0, 1));
        }
        if (ascentDone(legDelivered, legCost, t)) {
            syncFuel();
            reachOrbit();
        }
    }

    private void failAscent() {
        // not enough Δv: the capsule separates and comes down under its parachute
        for (int s = activeStage(); s < layout().stages().size(); s++) {
            spawnSpentStage(s, false);
        }
        setActiveStage(layout().stages().size());
        setBurning(false);
        entityData.set(DATA_TILT, 0F);
        setState(location().atmosphere() && layout().payload().parachute() ? FlightState.PARACHUTE : FlightState.DESCENT);
        setTimer(0);
        notifyCrew(Component.translatable("message.spacebreaker.ascent.failed").withStyle(ChatFormatting.RED));
    }

    private void reachOrbit() {
        Location to = destination();
        entityData.set(DATA_ASCENT, 1F);
        entityData.set(DATA_TILT, 0F);
        arrive(to);
    }

    private void tickBurn() {
        int t = timer() + 1;
        setTimer(t);
        setDeltaMovement(Vec3.ZERO);
        if (t >= BURN_TICKS) {
            setBurning(false);
            setState(FlightState.TRANSFER);
            setTimer(0);
        }
    }

    private void tickTransfer() {
        int t = timer() + 1;
        setTimer(t);
        if (t == TRANSFER_TICKS - PRELOAD_TICKS) {
            preloadLanding(destination());
        }
        Location dest = destination();
        if (t == 20 && dest != null && dest.isEarth() && !dest.orbit() && activeStage() < layout().stages().size()) {
            // coming home only the capsule re-enters: the stages separate here in orbit, in sight, not at the hand-over
            for (int s = activeStage(); s < layout().stages().size(); s++) {
                spawnSpentStage(s, true);
            }
            setActiveStage(layout().stages().size());
            level().playSound(null, blockPosition(), com.pizzaparker07.spacebreaker.content.audio.SbSounds.ROCKET_STAGE_SEPARATION.get(), SoundSource.NEUTRAL, 3F, 1F);
        }
        if (t >= TRANSFER_TICKS) {
            arrive(destination());
        }
    }

    /** Loads the ground where a landing will come down, so the descent starts over finished terrain. */
    private void preloadLanding(@org.jetbrains.annotations.Nullable Location to) {
        if (to == null || to.orbit() || !(level() instanceof ServerLevel server)) {
            return;
        }
        ServerLevel target = server.getServer().getLevel(to.dimension());
        if (target == null) {
            return;
        }
        double x = to.isEarth() ? (Double.isNaN(homeX) ? target.getSharedSpawnPos().getX() + 0.5 : homeX + 48) : getX() % 30000.0;
        double z = to.isEarth() ? (Double.isNaN(homeZ) ? target.getSharedSpawnPos().getZ() + 0.5 : homeZ + 16) : getZ() % 30000.0;
        target.getChunkSource().addRegionTicket(LANDING_TICKET, new net.minecraft.world.level.ChunkPos(BlockPos.containing(x, 0, z)), 4, getId());
    }

    /** Moves the rocket (and crew) to the destination's dimension and starts the right phase there. */
    private void arrive(Location to) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        // the coast there took real days: cryogenic propellant in the tanks boiled off on the way
        boilOff(Transfers.coastSeconds(location(), to) / com.pizzaparker07.spacebreaker.content.rocketry.design.BoilOff.SECONDS_PER_DAY);
        ServerLevel target = server.getServer().getLevel(to.dimension());
        if (target == null) {
            // destination not installed: stay where we are, in orbit
            notifyCrew(Component.translatable("message.spacebreaker.travel.unavailable", to.name()).withStyle(ChatFormatting.RED));
            setState(location().orbit() ? FlightState.ORBIT : FlightState.IDLE);
            setBurning(false);
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 pos;
        FlightState next;
        Vec3 speed = Vec3.ZERO;
        entityData.set(DATA_ORBIT_BODY, to.orbit() && Location.Orbits.SHARED.equals(to.dimension()) ? to.body().toString() : "");
        if (to.orbit()) {
            // in the shared orbit space every body gets its own region so different flights never meet
            double offset = Location.Orbits.SHARED.equals(to.dimension()) ? com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.sharedRegionX(to.body()) : 0;
            double x = getX() % 2048.0 + offset;
            pos = new Vec3(x, ORBIT_Y, getZ());
            next = FlightState.ORBIT;
        } else if (!to.atmosphere()) {
            pos = landingSpot(target, new Vec3(getX() % 30000.0, target.getMaxBuildHeight() + 40, getZ() % 30000.0));
            next = FlightState.DESCENT;
            speed = new Vec3(0, -2.5, 0);
        } else if (!to.isEarth()) {
            // entry into another world's air: aerobrake behind the shield, chutes, then a short retro-burn
            pos = landingSpot(target, new Vec3(getX() % 30000.0, Math.max(target.getMaxBuildHeight() + 200, 500), getZ() % 30000.0));
            if (LANDING_DRIFT > 0 && !pos.equals(new Vec3(getX() % 30000.0, pos.y, getZ() % 30000.0))) {
                // winds carry a parachute descent a little off the beacon, much further in a dust storm
                float storm = com.pizzaparker07.spacebreaker.content.mars.MarsWeather.dusty(target)
                        ? com.pizzaparker07.spacebreaker.content.mars.MarsWeather.stormStrength(target) : 0F;
                double a = random.nextDouble() * Math.PI * 2, d = random.nextDouble() * LANDING_DRIFT * (1 + 3 * storm);
                pos = pos.add(Math.cos(a) * d, 0, Math.sin(a) * d);
            }
            next = FlightState.REENTRY;
            speed = new Vec3(0, -3.2, 0);
        } else {
            {
                double x = Double.isNaN(homeX) ? target.getSharedSpawnPos().getX() + 0.5 : homeX + 48;
                double z = Double.isNaN(homeZ) ? target.getSharedSpawnPos().getZ() + 0.5 : homeZ + 16;
                pos = new Vec3(x, Math.max(target.getMaxBuildHeight() + 260, 600), z);
                next = FlightState.REENTRY;
                speed = new Vec3(0, -3.2, 0);
                // stages are left in orbit; only the capsule comes home
                setActiveStage(layout().stages().size());
            }
        }
        setState(next);
        setTimer(0);
        setBurning(next == FlightState.DESCENT);
        teleporting = true;
        // the crew travel separately and arrive straight in their seats, so the camera does not jump while the rocket
        // is still on its way to the client (it draws a stand-in until then)
        List<Entity> crew = target == level() ? List.of() : List.copyOf(getPassengers());
        crew.forEach(Entity::stopRiding);
        Entity moved = target == level() ? this : changeDimension(new DimensionTransition(target, pos, speed, getYRot(), 0, DimensionTransition.DO_NOTHING));
        teleporting = false;
        if (moved instanceof RocketEntity r) {
            r.teleporting = true;
            for (Entity e : crew) {
                Vec3 seat = r.getPassengerRidingPosition(e).subtract(e.getVehicleAttachmentPoint(r));
                Entity there;
                if (e instanceof ServerPlayer sp) {
                    sp.teleportTo(target, seat.x, seat.y, seat.z, sp.getYRot(), sp.getXRot());
                    there = sp;
                } else {
                    there = e.changeDimension(new DimensionTransition(target, seat, Vec3.ZERO, e.getYRot(), e.getXRot(), DimensionTransition.DO_NOTHING));
                }
                if (there != null) {
                    there.startRiding(r, true);
                }
            }
            r.teleporting = false;
            if (moved == this) {
                moveTo(pos.x, pos.y, pos.z);
                setDeltaMovement(speed);
            }
            if (to.orbit()) {
                r.seekStation(to);
            }
            r.onArrived(to);
        }
    }

    private void onArrived(Location to) {
        if (!to.orbit() && !to.isEarth() && level() instanceof ServerLevel here
                && com.pizzaparker07.spacebreaker.content.rocketry.station.LandingBeacons.get(here).size() > 0) {
            notifyCrew(Component.translatable("message.spacebreaker.landing_beacon.guided"));
        }
        if (!to.body().equals(Location.EARTH_SURFACE.body())) {
            beenAway = true;
        }
        if (!to.orbit() && !to.isEarth() && !layout().hasLegsFrom(activeStage())) {
            notifyCrew(Component.translatable("message.spacebreaker.warn.no_legs").withStyle(ChatFormatting.GOLD));
        }
        if (to.orbit()) {
            award(com.pizzaparker07.spacebreaker.content.research.Milestones.ORBIT);
            toastCrew(Component.translatable("toast.spacebreaker.orbit.title"), to.name());
        }
        if (isUncrewed()) {
            com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.arrived(this, to);
        }
    }

    @Override
    public void restoreFrom(Entity entity) {
        super.restoreFrom(entity);
        if (entity instanceof RocketEntity old) {
            owner = old.owner;
            autoTravel = old.autoTravel;
            teleporting = true; // passengers re-mount right after this
            homeX = old.homeX;
            beenAway = old.beenAway;
            homeZ = old.homeZ;
            requiredShield = old.requiredShield;
        }
    }

    private double groundY() {
        BlockPos p = blockPosition();
        if (!level().hasChunkAt(p)) {
            return Double.NaN;
        }
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ());
    }

    private void tickDescent() {
        int t = timer() + 1;
        setTimer(t);
        teleporting = false;
        double ground = groundY();
        if (Double.isNaN(ground)) {
            setDeltaMovement(0, -0.5, 0);
            return;
        }
        double h = getY() - ground;
        double vy = -Mth.clamp(h * 0.035, 0.09, 2.5);
        setDeltaMovement(0, vy, 0);
        setBurning(true);
        if (h < 14 && level() instanceof ServerLevel sl) {
            kickUpDust(sl, ground, h);
        }
        if (h <= Math.abs(vy) + 0.01) {
            setPos(getX(), ground, getZ());
            touchDown(Math.abs(vy) > 0.3 || !layout().hasLegsFrom(activeStage()));
        }
    }

    private void kickUpDust(ServerLevel sl, double ground, double h) {
        BlockPos below = BlockPos.containing(getX(), ground - 1, getZ());
        BlockState state = level().getBlockState(below);
        if (state.isAir()) {
            return;
        }
        int n = (int) (14 - h) * 2;
        for (int i = 0; i < n; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 1.5 + random.nextDouble() * 2.5;
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), getX() + Math.cos(a) * r, ground + 0.1, getZ() + Math.sin(a) * r,
                    1, Math.cos(a) * 0.5, 0.05, Math.sin(a) * 0.5, 0.25);
        }
        if (h < 6) {
            sl.sendParticles(ParticleTypes.POOF, getX(), ground + 0.2, getZ(), 4, 1.5, 0.05, 1.5, 0.08);
        }
    }

    private void touchDown(boolean hard) {
        setDeltaMovement(Vec3.ZERO);
        setBurning(false);
        setState(FlightState.IDLE);
        setTimer(0);
        if (isUncrewed()) {
            com.pizzaparker07.spacebreaker.content.rocketry.mission.UncrewedMissions.landed(this, location());
        }
        if (hard) {
            for (Entity p : getPassengers()) {
                p.hurt(damageSources().fall(), 6F);
            }
            notifyCrew(Component.translatable("message.spacebreaker.landing.hard").withStyle(ChatFormatting.GOLD));
        } else {
            if (location().equals(Location.MOON_SURFACE)) {
                award(com.pizzaparker07.spacebreaker.content.research.Milestones.MOON_LANDING);
            } else if (location().equals(Location.EARTH_SURFACE) && isCapsuleOnly() && beenAway) {
                award(com.pizzaparker07.spacebreaker.content.research.Milestones.SAFE_RETURN);
            }
            notifyCrew(Component.translatable("message.spacebreaker.landing.ok", location().name()).withStyle(ChatFormatting.GREEN));
        }
        if (!location().equals(Location.EARTH_SURFACE) && !MissionPlanner.canReturnHome(layout(), activeStage(), fuel, location(), SbConfig.realism())) {
            boolean refuel = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.canRefuel(location().body());
            notifyCrew(Component.translatable(refuel ? "message.spacebreaker.refuel_here" : "message.spacebreaker.warn.stranded")
                    .withStyle(refuel ? ChatFormatting.YELLOW : ChatFormatting.GOLD));
        }
    }

    private void tickReentry() {
        int t = timer() + 1;
        setTimer(t);
        teleporting = false;
        double ground = groundY();
        double vy = t < 140 ? -3.2 : Math.min(-1.2, getDeltaMovement().y + 0.03);
        setDeltaMovement(0, vy, 0);
        if (requiredShield > layout().payload().heatShield() && t % 20 == 0 && t < 160) {
            for (Entity p : getPassengers()) {
                p.igniteForSeconds(2);
                p.hurt(damageSources().onFire(), 3F);
            }
        }
        if (level() instanceof ServerLevel sl && t % 2 == 0 && t < 150) {
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() - 0.2, getZ(), 6, 0.6, 0.1, 0.6, 0.02);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 2, 0.4, 0.4, 0.4, 0.01);
        }
        if (!Double.isNaN(ground) && getY() - ground < 140) {
            if (!location().isEarth() && !layout().payload().parachute()) {
                setState(FlightState.DESCENT);
                setTimer(60);
            } else if (layout().payload().parachute()) {
                setState(FlightState.PARACHUTE);
                setTimer(0);
                level().playSound(null, blockPosition(), com.pizzaparker07.spacebreaker.content.audio.SbSounds.ROCKET_PARACHUTE_DEPLOY.get(), SoundSource.NEUTRAL, 3F, 1F);
            } else if (getY() - ground <= Math.abs(vy)) {
                setPos(getX(), ground, getZ());
                touchDown(true);
            }
        }
    }

    private void tickParachute() {
        int t = timer() + 1;
        setTimer(t);
        teleporting = false;
        double ground = groundY();
        double target = t < 40 ? -0.9 : -0.28;
        double vy = Mth.lerp(0.06, getDeltaMovement().y, target);
        setDeltaMovement(0, vy, 0);
        if (Double.isNaN(ground)) {
            return;
        }
        if (!location().isEarth() && getY() - ground < 24) {
            // chutes alone are not enough on other worlds: cut them and land on the engines
            setState(FlightState.DESCENT);
            setTimer(60);
            return;
        }
        if (getY() - ground <= Math.abs(vy) + 0.01) {
            setPos(getX(), ground, getZ());
            boolean water = level().getFluidState(BlockPos.containing(getX(), ground - 0.5, getZ())).isSource();
            if (water && level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SPLASH, getX(), ground + 0.2, getZ(), 60, 1.2, 0.2, 1.2, 0.3);
                sl.sendParticles(ParticleTypes.BUBBLE_POP, getX(), ground + 0.1, getZ(), 30, 1.2, 0.1, 1.2, 0.05);
                level().playSound(null, blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 2F, 0.8F);
            } else if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.POOF, getX(), ground + 0.2, getZ(), 20, 1.0, 0.1, 1.0, 0.05);
            }
            touchDown(false);
        }
    }

    private void spawnSpentStage(int stage, boolean inOrbit) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        RocketGeometry full = geometry();
        double y = 0;
        for (RocketGeometry.Placement p : full.placements()) {
            if (p.part() == layout().stages().get(stage).firstPart()) {
                y = p.y() / 16.0;
                break;
            }
        }
        SpentStageEntity spent = SpentStageEntity.create(sl, design, stage, position().add(0, y, 0), getDeltaMovement(), tilt(), inOrbit);
        sl.addFreshEntity(spent);
    }

    /** Research milestones (advancements) for everyone aboard. */
    private void award(String milestone) {
        for (Entity p : getPassengers()) {
            if (p instanceof ServerPlayer sp) {
                com.pizzaparker07.spacebreaker.content.research.Milestones.award(sp, milestone);
            }
        }
    }

    /** A short toast to the crew (or the owner of an empty rocket) instead of a chat line. */
    private void toastCrew(Component title, Component line) {
        boolean any = false;
        for (Entity p : getPassengers()) {
            if (p instanceof ServerPlayer player) {
                com.pizzaparker07.spacebreaker.content.rocketry.net.RocketToastPayload.send(player, title, line);
                any = true;
            }
        }
        if (!any && getPassengers().isEmpty() && owner != null && level().getServer() != null) {
            ServerPlayer o = level().getServer().getPlayerList().getPlayer(owner);
            if (o != null) {
                com.pizzaparker07.spacebreaker.content.rocketry.net.RocketToastPayload.send(o, title, line);
            }
        }
    }

    private void notifyCrew(Component message) {
        for (Entity p : getPassengers()) {
            if (p instanceof Player player) {
                player.displayClientMessage(message, false);
            }
        }
        if (getPassengers().isEmpty() && owner != null && level().getServer() != null) {
            ServerPlayer o = level().getServer().getPlayerList().getPlayer(owner);
            if (o != null) {
                o.displayClientMessage(message, false);
            }
        }
    }

    /** Used by the emergency return beacon: skip everything and bring the capsule home. */
    public void emergencyReturn() {
        if (state() == FlightState.REENTRY || state() == FlightState.PARACHUTE) {
            return;
        }
        requiredShield = 0;
        entityData.set(DATA_DEST, Location.EARTH_SURFACE.id());
        setBurning(false);
        setState(FlightState.TRANSFER);
        setTimer(TRANSFER_TICKS / 2);
    }

    /** Which body the rocket circles in the shared orbit dimension (scripted scenes and tests). */
    public void setOrbitBody(net.minecraft.resources.ResourceLocation body) {
        entityData.set(DATA_ORBIT_BODY, body.toString());
    }

    /** Puts the rocket straight into a flight phase (scripted scenes, tests and admin tools). */
    /** Where a staged scene is headed (for showcase and tests). */
    public void stageDestination(Location to) {
        entityData.set(DATA_DEST, to.id());
    }

    public void stageScene(FlightState state, int timer, int activeStage, boolean burning) {
        setActiveStage(Math.min(activeStage, layout().stages().size()));
        setState(state);
        setTimer(timer);
        setBurning(burning);
        if (state == FlightState.DESCENT || state == FlightState.REENTRY || state == FlightState.PARACHUTE) {
            setDeltaMovement(0, state == FlightState.REENTRY ? -3.2 : -0.5, 0);
        }
    }

    public void setHome(double x, double z) {
        homeX = x;
        homeZ = z;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        design = RocketDesign.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("design")).result().orElse(RocketDesign.EMPTY);
        layout = null;
        geometry = null;
        readFuel(tag.getCompound("fuel"));
        entityData.set(DATA_STATE, tag.getInt("state"));
        dockTarget = tag.contains("dockX") ? new Vec3(tag.getDouble("dockX"), tag.getDouble("dockY"), tag.getDouble("dockZ")) : null;
        entityData.set(DATA_DOCKING, dockTarget != null);
        dockedPort = tag.contains("dockPort") ? BlockPos.of(tag.getLong("dockPort")) : null;
        entityData.set(DATA_STAGE, tag.getInt("stage"));
        entityData.set(DATA_TIMER, tag.getInt("timer"));
        entityData.set(DATA_DEST, tag.getString("dest"));
        entityData.set(DATA_ORBIT_BODY, tag.getString("orbitBody"));
        entityData.set(DATA_BURNING, tag.getBoolean("burning"));
        entityData.set(DATA_TILT, tag.getFloat("tilt"));
        legCost = tag.getDouble("legCost");
        legDelivered = tag.getDouble("legDelivered");
        timeScale = Math.max(1, tag.getDouble("timeScale"));
        ascentFirstStage = tag.getInt("ascentFirst");
        coast = tag.getInt("coast");
        moduleDelivered = tag.getBoolean("moduleDelivered");
        coreBuiltHere = tag.getBoolean("coreBuiltHere");
        homeX = tag.contains("homeX") ? tag.getDouble("homeX") : Double.NaN;
        beenAway = tag.getBoolean("beenAway");
        routeA = tag.getString("routeA");
        routeB = tag.getString("routeB");
        routeOn = tag.getBoolean("routeOn");
        routeWait = tag.getInt("routeWait");
        routeAnchor = tag.contains("routeAnchor") ? BlockPos.of(tag.getLong("routeAnchor")) : null;
        if (tag.contains("cargo")) {
            cargo().clearContent();
            net.minecraft.world.ContainerHelper.loadAllItems(tag.getCompound("cargo"), cargo().getItems(), registryAccess());
        }
        entityData.set(DATA_MISSION, tag.getString("mission"));
        autoTravel = tag.getInt("autoTravel");
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        homeZ = tag.contains("homeZ") ? tag.getDouble("homeZ") : Double.NaN;
        requiredShield = tag.getInt("requiredShield");
        syncFuel();
        refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        RocketDesign.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, design).result().ifPresent(t -> tag.put("design", t));
        CompoundTag f = new CompoundTag();
        writeFuel(f);
        tag.put("fuel", f);
        tag.putInt("state", entityData.get(DATA_STATE));
        if (dockTarget != null) {
            tag.putDouble("dockX", dockTarget.x);
            tag.putDouble("dockY", dockTarget.y);
            tag.putDouble("dockZ", dockTarget.z);
        }
        if (dockedPort != null) {
            tag.putLong("dockPort", dockedPort.asLong());
        }
        tag.putInt("stage", activeStage());
        tag.putInt("timer", timer());
        tag.putString("dest", entityData.get(DATA_DEST));
        tag.putString("mission", entityData.get(DATA_MISSION));
        tag.putInt("autoTravel", autoTravel);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putString("orbitBody", entityData.get(DATA_ORBIT_BODY));
        tag.putBoolean("burning", burning());
        tag.putFloat("tilt", tilt());
        tag.putDouble("legCost", legCost);
        tag.putDouble("legDelivered", legDelivered);
        tag.putDouble("timeScale", timeScale);
        tag.putInt("ascentFirst", ascentFirstStage);
        tag.putInt("coast", coast);
        if (!Double.isNaN(homeX)) {
            tag.putDouble("homeX", homeX);
            tag.putDouble("homeZ", homeZ);
        }
        tag.putInt("requiredShield", requiredShield);
        tag.putBoolean("beenAway", beenAway);
        tag.putBoolean("moduleDelivered", moduleDelivered);
        tag.putBoolean("coreBuiltHere", coreBuiltHere);
        tag.putString("routeA", routeA);
        tag.putString("routeB", routeB);
        tag.putBoolean("routeOn", routeOn);
        tag.putInt("routeWait", routeWait);
        if (routeAnchor != null) {
            tag.putLong("routeAnchor", routeAnchor.asLong());
        }
        if (cargo != null && !cargo.isEmpty()) {
            // keep every stack in its slot
            tag.put("cargo", net.minecraft.world.ContainerHelper.saveAllItems(new CompoundTag(), cargo.getItems(), registryAccess()));
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        RocketDesign.STREAM_CODEC.encode(buffer, design);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        design = RocketDesign.STREAM_CODEC.decode(buffer);
        layout = null;
        geometry = null;
        readFuel(entityData.get(DATA_FUEL));
        refreshDimensions();
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        // server sends the position every tick; follow it directly so riders do not lag behind
        setPos(x, y, z);
        setRot(yRot, xRot);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512 * 512;
    }

    public List<Entity> crew() {
        return getPassengers();
    }

    public boolean isTeleporting() {
        return teleporting;
    }
}
