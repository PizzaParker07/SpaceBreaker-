package com.pizzaparker07.spacebreaker.content.worldgen;

import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local solar time on other worlds. Every landable body except Earth gets its own day length, scaled from
 * the real solar day ({@link WorldgenConfig}), and its own clock offset (saved per dimension), so the sun
 * rises and sets at the body's own pace. The vanilla time-of-day of those dimensions is redirected here
 * (see the {@code Level} mixin), which keeps sky light, mob spawning, solar panels and the sky renderer consistent.
 * <p>
 * Day fraction convention matches vanilla: 0 = sunrise, 0.25 = noon, 0.5 = sunset, 0.75 = midnight.
 */
public final class PlanetTime {
    /**
     * A body's clock: day length in game ticks, an offset added to the shared day time, and an optional warp that
     * bends the evenly running (mean) day into the true position of the sun (e.g. Mercury's 3:2 resonance).
     */
    public record Clock(long dayTicks, long offset, java.util.function.DoubleUnaryOperator warp) {
        public Clock(long dayTicks, long offset) {
            this(dayTicks, offset, null);
        }

        /** The evenly running day fraction, before any warp. */
        public double meanFraction(long dayTime) {
            return Mth.positiveModulo((double) (dayTime + offset), (double) dayTicks) / dayTicks;
        }

        public double fraction(long dayTime) {
            double mean = meanFraction(dayTime);
            return warp == null ? mean : Mth.positiveModulo(warp.applyAsDouble(mean), 1.0);
        }

        /** Vanilla's celestial angle curve (0 = noon) for this clock. */
        public float timeOfDay(long dayTime) {
            double d0 = Mth.frac(fraction(dayTime) - 0.25);
            double d1 = 0.5 - Math.cos(d0 * Math.PI) / 2.0;
            return (float) (d0 * 2.0 + d1) / 3.0F;
        }
    }

    private static final Map<ResourceKey<Level>, Clock> SERVER = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, java.util.function.DoubleUnaryOperator> WARPS = new ConcurrentHashMap<>();

    /**
     * Bends one world's day: the warp maps the evenly running day fraction (0..1) to the fraction the sun is really
     * at. Only for worlds whose sun does not move evenly (Mercury); every other world is untouched.
     */
    public static void addWarp(ResourceKey<Level> dimension, java.util.function.DoubleUnaryOperator warp) {
        WARPS.put(dimension, warp);
    }
    private static final Map<ResourceKey<Level>, Clock> CLIENT = new ConcurrentHashMap<>();

    /** The clock of a level, if it has its own. */
    public static Optional<Clock> clock(Level level) {
        return Optional.ofNullable((level.isClientSide ? CLIENT : SERVER).get(level.dimension()));
    }

    /** Time of day in vanilla's convention (0 = noon), or a negative value if the level uses vanilla time. */
    public static float timeOfDay(Level level) {
        Clock c = (level.isClientSide ? CLIENT : SERVER).get(level.dimension());
        return c == null ? -1F : c.timeOfDay(level.getDayTime());
    }

    /** Day fraction (0 = sunrise, 0.25 = noon) of a level, own clock or vanilla. */
    public static double dayFraction(Level level) {
        return clock(level).map(c -> c.fraction(level.getDayTime()))
                .orElseGet(() -> Mth.positiveModulo(level.getDayTime(), 24000L) / 24000.0);
    }

    /** Game ticks in one solar day of a body with the given real day length. */
    public static long dayTicks(double realDayHours) {
        double ratio = Math.max(1e-3, Math.abs(realDayHours) / 24.0);
        double minutes = 20.0 * Math.pow(ratio, WorldgenConfig.exponent());
        minutes = Mth.clamp(minutes, WorldgenConfig.minMinutes(), WorldgenConfig.maxMinutes());
        return Math.max(1200L, Math.round(minutes * 60 * 20));
    }

    /** Sets the local (mean) time of a planet level, e.g. 0.25 for noon. Synced to every player. */
    public static void setDayFraction(ServerLevel level, double fraction) {
        Clock c = SERVER.get(level.dimension());
        if (c == null) {
            return;
        }
        long target = Math.round(Mth.frac(fraction) * c.dayTicks());
        long offset = Math.floorMod(target - level.getDayTime(), c.dayTicks());
        Clock updated = new Clock(c.dayTicks(), offset, c.warp());
        SERVER.put(level.dimension(), updated);
        Data data = data(level);
        data.offset = offset;
        data.setDirty();
        PlanetTimePayload payload = new PlanetTimePayload(level.dimension(), updated.dayTicks(), updated.offset());
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            send(p, payload);
        }
    }

    // ------------------------------------------------------------------ lifecycle

    static void onLevelLoad(ServerLevel level) {
        if (level.dimension() == Level.OVERWORLD) {
            return;
        }
        Optional<CelestialBody> body = Bodies.forLevel(level);
        if (body.isEmpty() || body.get().dimension().filter(level.dimension()::equals).isEmpty()) {
            return;
        }
        long ticks = dayTicks(body.get().rotation().dayLengthHours());
        SERVER.put(level.dimension(), new Clock(ticks, data(level).offset, WARPS.get(level.dimension())));
    }

    static void onLevelUnload(ServerLevel level) {
        SERVER.remove(level.dimension());
    }

    static void syncTo(ServerPlayer player) {
        SERVER.forEach((dim, c) -> send(player, new PlanetTimePayload(dim, c.dayTicks(), c.offset())));
    }

    /** Sends only to clients that have the mod's channel (a vanilla client on the server must not get it). */
    private static void send(ServerPlayer player, PlanetTimePayload payload) {
        if (player.connection != null && player.connection.hasChannel(PlanetTimePayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    static void receive(PlanetTimePayload payload) {
        CLIENT.put(payload.dimension(), new Clock(payload.dayTicks(), payload.offset(), WARPS.get(payload.dimension())));
    }

    /** Forget client clocks (on disconnect). */
    public static void clearClient() {
        CLIENT.clear();
    }

    private static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load), "spacebreaker_planet_time");
    }

    /** Saved clock offset of one planet dimension. */
    static final class Data extends SavedData {
        long offset;

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            d.offset = tag.getLong("offset");
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putLong("offset", offset);
            return tag;
        }
    }

    private PlanetTime() {
    }
}
