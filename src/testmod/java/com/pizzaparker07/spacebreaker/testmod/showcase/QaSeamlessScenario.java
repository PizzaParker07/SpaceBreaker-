package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * QA (beta.1 feedback, "launch and planet changes cut straight away"): rocketry's seamless trips as a player flies
 * them. Flight 1: the showcase pad rocket with the player and a second crew member (a fake player) launches, reaches
 * Earth orbit and comes back down to Earth. Flight 2: from Earth orbit to Moon orbit and down to the Moon's surface.
 * The client logs every tick (dimension, screen on top, seated or not, the rocket's state, whether the stand-in rocket
 * is drawn while a real rocket is already near) to {@code run-showcase/qa_seamless.tsv} and saves frames (every 10
 * ticks, every tick around a change of dimension); the server logs where each crew member is at every hand-over and
 * where each flight lands. Log lines "[showcase] seamless: …". A second crew member cannot be tried here: a fake player
 * is not in the world, and a server player on a connection with nothing at the other end has not agreed the mod's
 * network channels, so the mod's normal login sync refuses to send to it; that needs a second real client.
 */
final class QaSeamlessScenario implements ShowcaseScenario {
    private static final int SHOTS = 80, STEP = 100;
    private final ShowcaseScenario rocketry;

    QaSeamlessScenario(ShowcaseScenario rocketry) {
        this.rocketry = rocketry;
    }

    @Override
    public void build(ServerLevel level) {
        rocketry.build(level);
    }

    @Override
    public ResourceKey<Level> dimension() {
        return rocketry.dimension();
    }

    @Override
    public List<Shot> shots() {
        List<Shot> s = new ArrayList<>();
        s.add(new Shot("00_board", 6000, 0.5, 120, 0.5, 0F, 0F, true, CameraType.THIRD_PERSON_BACK, 60, Flight::begin, Recorder::start));
        for (int i = 1; i <= SHOTS; i++) {
            s.add(Shot.stay(String.format(Locale.ROOT, "%02d_flight", i), 6000, true, CameraType.THIRD_PERSON_BACK, STEP, p -> {
            }, null));
        }
        s.add(Shot.stay("99_end", 6000, true, CameraType.THIRD_PERSON_BACK, 5, p -> {
            ShowcaseDriver.burst = false;
            Flight.summary();
        }, Recorder::stop));
        return s;
    }

    /** Server side: the flight plan, run every server tick. */
    static final class Flight {
        private static boolean registered;
        private static int phase = -1;
        private static int wait;
        private static ServerPlayer pilot;
        private static ServerPlayer crew2;
        private static RocketEntity rocket;
        private static Vec3 launchedFrom;
        private static String lastDim = "";
        private static FlightState lastState;
        private static final List<String> notes = new ArrayList<>();

        static void begin(ServerPlayer p) {
            ServerLevel level = p.serverLevel();
            var rockets = level.getEntitiesOfClass(RocketEntity.class, new AABB(p.blockPosition()).inflate(256));
            if (rockets.isEmpty()) {
                log("no pad rocket found");
                return;
            }
            // the pad rocket with the three-seat capsule, so a second crew member can come along
            RocketEntity pad = rockets.get(0);
            rocket = RocketEntity.create(level, crewOfThree(), pad.position(), pad.getYRot());
            pad.discard();
            level.addFreshEntity(rocket);
            pilot = p;
            p.getAbilities().flying = false;
            p.setNoGravity(false);
            p.onUpdateAbilities();
            p.teleportTo(rocket.getX(), rocket.getY() + 2, rocket.getZ());
            rocket.fillCompletely();
            boolean seated = p.startRiding(rocket, true);
            boolean second = false; // see the class comment: no second client in this harness
            launchedFrom = rocket.position();
            ShowcaseDriver.burst = true;
            boolean go = rocket.tryStartCountdown(null);
            log(String.format(Locale.ROOT, "flight 1: pilot seated %s, second crew seated %s (seats %d), countdown %s, pad at %.1f %.1f %.1f",
                    seated, second, rocket.layout().payload().seats(), go, launchedFrom.x, launchedFrom.y, launchedFrom.z));
            phase = 0;
            if (!registered) {
                registered = true;
                NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, e -> tick(e.getServer()));
            }
        }

        private static void tick(MinecraftServer server) {
            if (phase < 0 || pilot == null) {
                return;
            }
            Entity v = pilot.getVehicle();
            if (v instanceof RocketEntity r) {
                rocket = r;
            }
            String dim = pilot.level().dimension().location().toString();
            FlightState st = rocket == null ? null : rocket.state();
            if (!dim.equals(lastDim) || st != lastState) {
                notes.add(String.format(Locale.ROOT, "tick %d: pilot in %s, seated %s, rocket %s in %s at y %.0f; second crew in %s, riding %s",
                        server.getTickCount(), dim, v instanceof RocketEntity, st,
                        rocket == null ? "-" : rocket.level().dimension().location(), rocket == null ? 0 : rocket.getY(),
                        crew2 == null ? "-" : crew2.level().dimension().location(),
                        crew2 == null ? "-" : crew2.getVehicle() == null ? "nothing" : crew2.getVehicle() == rocket ? "the rocket" : "another entity"));
                log(notes.get(notes.size() - 1));
                lastDim = dim;
                lastState = st;
            }
            if (rocket == null) {
                return;
            }
            switch (phase) {
                case 0 -> { // climbing: wait for Earth orbit, stay a little, then land back on the Earth
                    if (st == FlightState.ORBIT && Location.EARTH_ORBIT.equals(rocket.location()) && ++wait > 160) {
                        log("flight 1: de-orbit to the Earth's surface: " + rocket.tryTravel(null, Location.EARTH_SURFACE));
                        phase = 1;
                        wait = 0;
                    }
                }
                case 1 -> {
                    if (st == FlightState.IDLE && rocket.level().dimension() == Level.OVERWORLD && ++wait > 40) {
                        Vec3 at = rocket.position();
                        log(String.format(Locale.ROOT, "flight 1: landed at %.1f %.1f %.1f, %.1f blocks from the pad it left",
                                at.x, at.y, at.z, Math.hypot(at.x - launchedFrom.x, at.z - launchedFrom.z)));
                        startFlight2(server);
                    }
                }
                case 2 -> {
                    if (st == FlightState.ORBIT && Location.EARTH_ORBIT.equals(rocket.location()) && ++wait > 60) {
                        log("flight 2: transfer to Moon orbit: " + rocket.tryTravel(null, Location.MOON_ORBIT));
                        phase = 3;
                        wait = 0;
                    }
                }
                case 3 -> {
                    if (st == FlightState.ORBIT && Location.MOON_ORBIT.equals(rocket.location()) && ++wait > 100) {
                        log("flight 2: landing on the Moon: " + rocket.tryTravel(null, Location.MOON_SURFACE));
                        phase = 4;
                        wait = 0;
                    }
                }
                case 4 -> {
                    if (st == FlightState.IDLE && Location.MOON_SURFACE.dimension().equals(rocket.level().dimension()) && ++wait > 40) {
                        Vec3 at = rocket.position();
                        log(String.format(Locale.ROOT, "flight 2: landed on the Moon at %.1f %.1f %.1f (orbit position was %.1f %.1f)",
                                at.x, at.y, at.z, orbitX, orbitZ));
                        phase = 5;
                    }
                }
                default -> {
                }
            }
        }

        private static double orbitX, orbitZ;


        /** The Moon rocket with the large crew capsule (three seats). */
        private static com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign crewOfThree() {
            List<net.minecraft.resources.ResourceLocation> parts = new ArrayList<>();
            for (String id : new String[]{"kerosene_engine", "fuel_tank_large", "fuel_tank_large", "fuel_tank_large", "fins", "interstage",
                    "vacuum_engine", "fuel_tank_large", "fuel_tank_large", "decoupler", "vacuum_engine", "fuel_tank_medium", "landing_legs",
                    "heat_shield", "crew_capsule_large", "parachute"}) {
                parts.add(com.pizzaparker07.spacebreaker.Spacebreaker.id(id));
            }
            return new com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign(parts);
        }

        private static void startFlight2(MinecraftServer server) {
            ServerLevel orbit = server.getLevel(Location.EARTH_ORBIT.dimension());
            if (orbit == null) {
                log("no Earth orbit world");
                phase = 5;
                return;
            }
            pilot.stopRiding();
            RocketEntity r = RocketEntity.create(orbit, crewOfThree(), new Vec3(0.5, RocketEntity.ORBIT_Y, 0.5), 0F);
            r.fillCompletely();
            orbit.addFreshEntity(r);
            r.stageScene(FlightState.ORBIT, 0, 1, false);
            pilot.teleportTo(orbit, 0.5, RocketEntity.ORBIT_Y + 1, 0.5, 0F, 0F);
            boolean seated = pilot.startRiding(r, true);
            boolean second = false;
            rocket = r;
            orbitX = r.getX();
            orbitZ = r.getZ();
            log("flight 2: in Earth orbit, pilot seated " + seated + ", second crew seated " + second);
            phase = 2;
            wait = 0;
        }

        static void summary() {
            log("summary: " + notes.size() + " hand-overs/state changes logged, phase reached " + phase + " of 5");
            phase = -1;
        }

        private static void log(String msg) {
            ShowcaseDriver.log("seamless: " + msg);
        }
    }

    /** Client side: every tick logged, frames saved. */
    static final class Recorder {
        private static boolean registered;
        private static int tick = -1;
        private static String lastDim = "", lastScreen = "";
        private static int denseUntil;
        private static int unseated, ghostWithRocket, loadingScreens;
        private static final StringBuilder LOG = new StringBuilder("tick\tdimension\tscreen\tseated\tstate\tghost\trockets_near\ty\tframe\n");
        private static Method ghost;

        static void start() {
            tick = 0;
            if (!registered) {
                registered = true;
                NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> onTick());
            }
        }

        static void stop() {
            if (tick < 0) {
                return;
            }
            write();
            ShowcaseDriver.log(String.format(Locale.ROOT, "seamless: client: %d ticks recorded; loading/progress screens %d ticks; "
                    + "not seated during the flights %d ticks; stand-in drawn with a real rocket near %d ticks", tick, loadingScreens, unseated, ghostWithRocket));
            tick = -1;
        }

        private static boolean ghost() {
            try {
                if (ghost == null) {
                    ghost = Class.forName("com.pizzaparker07.spacebreaker.client.rocketry.TransitView").getDeclaredMethod("ghost");
                    ghost.setAccessible(true);
                }
                return (boolean) ghost.invoke(null);
            } catch (ReflectiveOperationException e) {
                return false;
            }
        }

        private static void onTick() {
            if (tick < 0) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            String screen = mc.screen == null ? "-" : mc.screen.getClass().getSimpleName();
            String dim = mc.level == null ? "(none)" : mc.level.dimension().location().toString();
            if (!dim.equals(lastDim) || !screen.equals(lastScreen)) {
                denseUntil = tick + 40;
                lastDim = dim;
                lastScreen = screen;
            }
            var pl = mc.player;
            boolean seated = pl != null && pl.getVehicle() instanceof RocketEntity;
            String state = seated ? ((RocketEntity) pl.getVehicle()).state().name() : "-";
            boolean g = ghost();
            int near = pl == null || mc.level == null ? 0
                    : mc.level.getEntitiesOfClass(RocketEntity.class, pl.getBoundingBox().inflate(48)).size();
            if (g && near > 0) {
                ghostWithRocket++;
            }
            if (screen.contains("Loading") || screen.contains("Receiving") || screen.contains("Progress") || screen.contains("Generic")) {
                loadingScreens++;
            }
            if (!seated && Flight.phase >= 0 && Flight.phase < 5 && tick > 80) {
                unseated++;
            }
            String frame = "";
            if (tick % 10 == 0 || tick <= denseUntil) {
                frame = String.format(Locale.ROOT, "qa_seamless_%05d.png", tick);
                String f = frame;
                Screenshot.grab(mc.gameDirectory, f, mc.getMainRenderTarget(), msg -> { });
            }
            LOG.append(tick).append('\t').append(dim).append('\t').append(screen).append('\t').append(seated).append('\t').append(state)
                    .append('\t').append(g).append('\t').append(near).append('\t')
                    .append(pl == null ? "" : String.format(Locale.ROOT, "%.1f", pl.getY())).append('\t').append(frame).append('\n');
            tick++;
            if (tick % 200 == 0) {
                write();
            }
        }

        private static void write() {
            try {
                Files.writeString(Path.of(Minecraft.getInstance().gameDirectory.getPath(), "qa_seamless.tsv"), LOG.toString());
            } catch (IOException ex) {
                ShowcaseDriver.log("seamless: could not write log " + ex);
            }
        }
    }
}
