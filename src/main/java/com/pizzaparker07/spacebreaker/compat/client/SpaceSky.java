package com.pizzaparker07.spacebreaker.compat.client;

import com.pizzaparker07.spacebreaker.compat.SpaceWorlds;
import net.minecraft.client.multiplayer.ClientLevel;

/** The current client world's {@link SpaceWorlds.Kind}, cached per level (asked every frame). */
public final class SpaceSky {
    private static ClientLevel cachedLevel;
    private static SpaceWorlds.Kind cachedKind = SpaceWorlds.Kind.VANILLA;

    private SpaceSky() {
    }

    public static SpaceWorlds.Kind kind(ClientLevel level) {
        if (level != cachedLevel) {
            cachedLevel = level;
            cachedKind = level == null ? SpaceWorlds.Kind.VANILLA : SpaceWorlds.classify(level);
        }
        return cachedKind;
    }

    public static void reset() {
        cachedLevel = null;
        cachedKind = SpaceWorlds.Kind.VANILLA;
    }
}
