package com.pizzaparker07.spacebreaker.client.light;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Moving lights for lamps that point somewhere: helmet lamps, rover headlights. A feature registers a {@link Source}
 * that lists its beams; this class hands them to whichever backend draws light:
 * <ul>
 *   <li>with a dynamic lights mod (LambDynamicLights, see {@code compat/dynlights}) every beam becomes a real light
 *       along its line, so the blocks themselves light up, smooth and steady, also under shader packs;</li>
 *   <li>without one, {@link BeamPools} lays a soft pool of light where each beam lands (and a faint glow around the
 *       lamp), fading smoothly towards the edge.</li>
 * </ul>
 * Client only.
 */
public final class LightBeams {
    /**
     * One beam: from the lamp to where it ends ({@code hit}: on a surface, else at its full reach).
     *
     * @param key        stable per lamp (e.g. the wearer's id), so the backends can smooth it from frame to frame
     * @param luminance  light level along the beam for a dynamic lights mod, 0..15
     * @param poolRadius radius of the soft pool where it lands (fallback), in blocks
     * @param strength   pool brightness 0..1 (fallback)
     * @param nearGlow   faint light right around the lamp (fallback), 0 for none
     * @param spotLuminance light level of a short bright spot where the beam lands (dynamic lights), 0 for none: a
     *                      narrow long beam reads as a bright pool far ahead rather than an evenly lit line
     */
    public record Beam(Object key, Vec3 from, Vec3 to, boolean hit, net.minecraft.core.Direction face, int luminance,
                       double poolRadius, double strength, double nearGlow, int spotLuminance) {
        public Beam(Object key, Vec3 from, Vec3 to, boolean hit, net.minecraft.core.Direction face, int luminance,
                    double poolRadius, double strength, double nearGlow) {
            this(key, from, to, hit, face, luminance, poolRadius, strength, nearGlow, 0);
        }
    }

    /** Lists the beams that shine now; called once per frame (fallback) and once per tick (dynamic lights). */
    @FunctionalInterface
    public interface Source {
        void collect(Minecraft mc, float partialTick, Consumer<Beam> out);
    }

    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();
    /** Set by the dynamic lights bridge once the mod has taken our beams; the fallback pools then stay off. */
    private static volatile boolean dynamicLights;

    private LightBeams() {
    }

    public static void register(Source source) {
        SOURCES.add(source);
    }

    public static List<Beam> collect(Minecraft mc, float partialTick) {
        List<Beam> beams = new ArrayList<>();
        if (mc.level == null) {
            return beams;
        }
        for (Source s : SOURCES) {
            s.collect(mc, partialTick, beams::add);
        }
        return beams;
    }

    public static boolean dynamicLights() {
        return dynamicLights;
    }

    public static void setDynamicLights(boolean on) {
        dynamicLights = on;
    }
}
