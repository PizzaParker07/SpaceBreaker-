package com.pizzaparker07.spacebreaker.compat.client;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

/**
 * Optional GPU timing of our render passes (GL timer queries), for profiling only: enabled with the JVM property
 * {@code -Dspacebreaker.compat.profile=true}; otherwise every call is a no-op. Results are read a few frames later so
 * the CPU never waits for the GPU, and averages are logged every 60 frames together with the frame rate.
 */
public final class PassTimer {
    public static final boolean ENABLED = Boolean.getBoolean("spacebreaker.compat.profile");
    private static final int RING = 4;
    private static final int LOG_EVERY = 60;

    private final String name;
    private final int[] queries = new int[RING];
    private final boolean[] pending = new boolean[RING];
    private int index;
    private double sumMs;
    private int samples;
    private int frames;

    public PassTimer(String name) {
        this.name = name;
    }

    public void begin() {
        if (!ENABLED) {
            return;
        }
        if (queries[0] == 0) {
            for (int i = 0; i < RING; i++) {
                queries[i] = GL15.glGenQueries();
            }
        }
        collect(index);
        GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, queries[index]);
    }

    public void end() {
        if (!ENABLED || queries[0] == 0) {
            return;
        }
        GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
        pending[index] = true;
        index = (index + 1) % RING;
        if (++frames >= LOG_EVERY) {
            Spacebreaker.LOGGER.info("[compat-profile] {}: {} ms/frame GPU over {} samples, {} fps", name,
                    samples == 0 ? "n/a" : String.format("%.3f", sumMs / samples), samples, Minecraft.getInstance().getFps());
            frames = 0;
            sumMs = 0;
            samples = 0;
        }
    }

    private void collect(int i) {
        if (!pending[i]) {
            return;
        }
        long ns = GL33.glGetQueryObjecti64(queries[i], GL15.GL_QUERY_RESULT);
        pending[i] = false;
        sumMs += ns / 1.0e6;
        samples++;
    }
}
