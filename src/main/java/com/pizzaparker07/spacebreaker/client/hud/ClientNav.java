package com.pizzaparker07.spacebreaker.client.hud;

import com.pizzaparker07.spacebreaker.content.suit.Navigation;
import com.pizzaparker07.spacebreaker.content.suit.net.NavPayload;
import net.minecraft.core.BlockPos;

/** Client copy of the way-home target sent by the server once a second. */
public final class ClientNav {
    private static Navigation.Kind kind;
    private static BlockPos target;

    private ClientNav() {
    }

    public static void accept(NavPayload payload) {
        target = payload.target().orElse(null);
        kind = target == null ? null : Navigation.Kind.values()[Math.max(0, Math.min(3, payload.kind()))];
    }

    public static BlockPos target() {
        return target;
    }

    public static Navigation.Kind kind() {
        return kind;
    }
}
