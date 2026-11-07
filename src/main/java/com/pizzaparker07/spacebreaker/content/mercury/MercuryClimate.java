package com.pizzaparker07.spacebreaker.content.mercury;

import com.pizzaparker07.spacebreaker.content.worldgen.ColdTraps;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.SkyMath;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shade on Mercury. With no air to carry heat, ground the sun cannot see is cold even at noon: anything that blocks the
 * line to the sun (a scarp, a crater rim, a roof you build) drops the temperature from the scorching sunlit value to
 * {@link #SHADE_C}, which a first-tier suit can stand. Near the terminator the sun is low and shadows are long, so the
 * day side can be crossed from shadow to shadow; at noon there is little shade but what you make. Cold traps keep
 * their own, colder value.
 */
public final class MercuryClimate {
    /** Temperature in shade on the day side, °C. */
    public static final double SHADE_C = -60;
    /** How far toward the sun we look for something casting a shadow, blocks. */
    public static final int REACH = 48;
    private static final Map<Long, Boolean> CACHE = new ConcurrentHashMap<>();

    private MercuryClimate() {
    }

    static void register() {
        Environment.addTemperatureModifier(MercuryClimate::offsetC);
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 5 && player.level().dimension() == MercuryModule.MERCURY) {
                notify(player);
            }
        });
    }

    /** The direction to the sun at a position on Mercury, or null when the sun is down (or this is not a planet world). */
    public static Vector3d sun(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || !(server.getChunkSource().getGenerator() instanceof PlanetChunkGenerator gen)) {
            return null;
        }
        double lat = PlanetTerrain.latitude(pos.getZ(), gen.terrain().profile().poleDistance());
        Vector3d s = SkyMath.sunDirection(level.getTimeOfDay(1F), lat);
        return s.y > 0.01 ? s : null;
    }

    /** True if something solid lies between this position and the sun within {@link #REACH} blocks. */
    public static boolean shadowed(Level level, BlockPos pos, Vector3d sun) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (double d = 1; d <= REACH; d += 0.5) {
            p.set(x + sun.x * d, y + sun.y * d, z + sun.z * d);
            if (p.getY() >= level.getMaxBuildHeight()) {
                return false;
            }
            BlockState st = level.getBlockState(p);
            if (!st.isAir() && st.canOcclude()) {
                return true;
            }
        }
        return false;
    }

    /** Sunlit temperature replaced by the shade value (never warmer than sunlit, never colder than {@link #SHADE_C}). */
    public static double shaded(double sunlitC) {
        return Math.min(sunlitC, SHADE_C);
    }

    static boolean inShade(Level level, BlockPos pos) {
        Vector3d sun = sun(level, pos);
        if (sun == null) {
            return false;
        }
        long key = pos.asLong() * 31 + level.getGameTime() / 20;
        if (CACHE.size() > 4096) {
            CACHE.clear();
        }
        return CACHE.computeIfAbsent(key, k -> shadowed(level, pos, sun));
    }

    static double offsetC(Level level, BlockPos pos) {
        if (level.dimension() != MercuryModule.MERCURY || ColdTraps.inColdTrap(level, pos) || !inShade(level, pos)) {
            return 0;
        }
        // undo the sunlit warming of the base temperature
        double base = Environment.body(level).map(b -> {
            double sunH = Math.max(0.0, Math.sin(Environment.sunElevation(level)));
            return b.climate().nightTempC() + (b.climate().dayTempC() - b.climate().nightTempC()) * Math.sqrt(sunH);
        }).orElse(0.0);
        return shaded(base) - base;
    }

    private static void notify(ServerPlayer player) {
        BlockPos eyes = BlockPos.containing(player.getEyePosition());
        boolean in = sun(player.level(), eyes) != null && inShade(player.level(), eyes);
        var tag = player.getPersistentData();
        if (tag.getBoolean("spacebreaker_mercury_shade") == in) {
            return;
        }
        tag.putBoolean("spacebreaker_mercury_shade", in);
        player.displayClientMessage(Component.translatable(in ? "message.spacebreaker.mercury_shade.enter" : "message.spacebreaker.mercury_shade.leave",
                Math.round(Environment.temperatureC(player.level(), eyes))), true);
    }
}
