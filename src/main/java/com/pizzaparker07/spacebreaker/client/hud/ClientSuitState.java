package com.pizzaparker07.spacebreaker.client.hud;

import com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload;

/** Last suit status received from the server (read by the HUD and the Maneuvering Unit controls). */
public final class ClientSuitState {
    private static SuitStatusPayload status;
    private static long receivedAt;

    private ClientSuitState() {
    }

    public static void accept(SuitStatusPayload payload, long gameTime) {
        status = payload;
        receivedAt = gameTime;
    }

    /** The latest status, or null when none arrived (yet) or it is stale. */
    public static SuitStatusPayload get(long gameTime) {
        if (status == null || gameTime - receivedAt > 60 || gameTime < receivedAt) {
            return null;
        }
        return status;
    }

    public static void clear() {
        status = null;
    }
}
