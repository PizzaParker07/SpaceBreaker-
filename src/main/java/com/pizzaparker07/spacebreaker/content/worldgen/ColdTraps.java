package com.pizzaparker07.spacebreaker.content.worldgen;

import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cold traps: the floors of permanently shadowed polar craters never see the sun, so they stay far colder than
 * the night side (about -230 °C on the Moon). This cancels the daytime warming there and drops the floor
 * {@link #BELOW_NIGHT_C} under the body's night temperature: a first-tier suit cannot cope, a heated suit (or
 * enough thermal modules) can. Applies to every world whose terrain profile has shadowed polar craters.
 */
public final class ColdTraps {
    /** How much colder than the night side a shadowed crater floor is. */
    public static final double BELOW_NIGHT_C = 60;
    /** Height above the crater floor still inside the cold trap. */
    public static final int DEPTH = 8;
    private static final Map<Level, Map<Long, Integer>> FLOOR = new java.util.WeakHashMap<>();

    private ColdTraps() {
    }

    static void register() {
        Environment.addTemperatureModifier(ColdTraps::offsetC);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post.class, e -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player && player.tickCount % 20 == 0) {
                notify(player);
            }
        });
    }

    /** Tells a player on the action bar when they step into or out of a cold trap. */
    private static void notify(net.minecraft.server.level.ServerPlayer player) {
        boolean in = inColdTrap(player.level(), player.blockPosition());
        var tag = player.getPersistentData();
        if (tag.getBoolean("spacebreaker_cold_trap") == in) {
            return;
        }
        tag.putBoolean("spacebreaker_cold_trap", in);
        player.displayClientMessage(in
                ? net.minecraft.network.chat.Component.translatable("message.spacebreaker.cold_trap.enter",
                        Math.round(Environment.temperatureC(player.level(), player.blockPosition())))
                : net.minecraft.network.chat.Component.translatable("message.spacebreaker.cold_trap.leave"), true);
    }

    /** Height of the shadowed crater floor at this column, or {@code Integer.MIN_VALUE} outside cold traps. */
    public static int floor(ServerLevel level, int x, int z) {
        if (!(level.getChunkSource().getGenerator() instanceof PlanetChunkGenerator gen)) {
            return Integer.MIN_VALUE;
        }
        Map<Long, Integer> cache;
        synchronized (FLOOR) {
            cache = FLOOR.computeIfAbsent(level, l -> new ConcurrentHashMap<>());
        }
        if (cache.size() > 8192) {
            cache.clear();
        }
        return cache.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> gen.coldTrapFloor(x, z));
    }

    /** True if this position is inside a cold trap (shadowed crater floor, up to {@link #DEPTH} above it). */
    public static boolean inColdTrap(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        int floor = floor(server, pos.getX(), pos.getZ());
        return floor != Integer.MIN_VALUE && pos.getY() <= floor + DEPTH;
    }

    static double offsetC(Level level, BlockPos pos) {
        if (!inColdTrap(level, pos)) {
            return 0;
        }
        return Environment.body(level).map(b -> offsetC(b.climate().dayTempC(), b.climate().nightTempC(), Environment.sunElevation(level)))
                .orElse(0.0);
    }

    /** Temperature offset inside a cold trap: cancels the sun's warming and drops below the night temperature. */
    public static double offsetC(double dayC, double nightC, float sunElevation) {
        double sun = Math.max(0.0, Math.sin(sunElevation));
        return -(dayC - nightC) * Math.sqrt(sun) - BELOW_NIGHT_C;
    }
}
