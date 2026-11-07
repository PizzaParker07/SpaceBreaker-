package com.pizzaparker07.spacebreaker.content.worldgen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Client copy of the micrometeorite shower over the player's dimension (set by {@link MeteorShowerPayload}). */
public final class ShowerClientState {
    private static ResourceKey<Level> dimension;
    private static long endMillis;

    private ShowerClientState() {
    }

    static void receive(MeteorShowerPayload payload) {
        dimension = payload.dimension();
        endMillis = payload.ticksLeft() <= 0 ? 0 : System.currentTimeMillis() + payload.ticksLeft() * 50L;
    }

    /** True while a shower falls over the given dimension. */
    public static boolean active(ResourceKey<Level> dim) {
        return dim.equals(dimension) && System.currentTimeMillis() < endMillis;
    }

    public static void clear() {
        dimension = null;
        endMillis = 0;
    }
}
