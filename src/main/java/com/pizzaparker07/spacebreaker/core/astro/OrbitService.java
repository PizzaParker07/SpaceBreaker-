package com.pizzaparker07.spacebreaker.core.astro;

import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Server-side planet positions and launch windows, cached per server. Time comes from the overworld's game
 * time (so it is the same in every dimension) and the server's {@code orbitalTimeScale}.
 * <pre>{@code
 * OrbitService orbits = OrbitService.get(server);
 * Ephemeris.Window w = orbits.window("spacebreaker:earth", "spacebreaker:mars");
 * long ticks = orbits.ticksUntil(w);          // 0 when the window is open
 * Vec3 mars = orbits.ephemeris().position("spacebreaker:mars");
 * }</pre>
 */
public final class OrbitService {
    private static final Map<MinecraftServer, OrbitService> BY_SERVER = new WeakHashMap<>();

    private final MinecraftServer server;
    private final Registry<CelestialBody> registry;
    private final Ephemeris ephemeris;

    private OrbitService(MinecraftServer server, Registry<CelestialBody> registry) {
        this.server = server;
        this.registry = registry;
        this.ephemeris = new Ephemeris(BodyAdapter.system(registry), this::simTime);
    }

    public static OrbitService get(MinecraftServer server) {
        Registry<CelestialBody> reg = Bodies.registry(server.registryAccess())
                .orElseThrow(() -> new IllegalStateException("celestial_body registry missing"));
        synchronized (BY_SERVER) {
            OrbitService s = BY_SERVER.get(server);
            if (s == null || s.registry != reg) {
                s = new OrbitService(server, reg);
                BY_SERVER.put(server, s);
            }
            return s;
        }
    }

    public static Optional<OrbitService> get(ServerLevel level) {
        return Bodies.registry(level.registryAccess()).isPresent() ? Optional.of(get(level.getServer())) : Optional.empty();
    }

    public Ephemeris ephemeris() {
        return ephemeris;
    }

    public BodySystem system() {
        return ephemeris.system();
    }

    /** Simulated seconds since J2000 now. */
    public double simTime() {
        return BodyAdapter.simTime(server.overworld());
    }

    public Ephemeris.Window window(String from, String to) {
        return ephemeris.window(from, to);
    }

    /** Game ticks until the window opens (0 if it is open). */
    public long ticksUntil(Ephemeris.Window w) {
        return GameClock.ticks(w.waitFrom(simTime()), BodyAdapter.timeScale());
    }
}
