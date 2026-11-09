package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Client hook for any vehicle that changes dimension with the player aboard (rockets use it internally; a spacecraft
 * calls it too): call {@link #expect} on the client just before the server moves the vehicle. The loading screen is
 * then kept out of sight and, until the vehicle reaches the client in the new dimension, a stand-in of it is drawn
 * through its normal renderer where the player sits. The views either side of the change should match (see
 * {@link SpaceView}); this only keeps the picture going across it.
 */
public final class SeamlessHandOff {
    private SeamlessHandOff() {
    }

    /**
     * The next change of dimension is this vehicle's trip from {@code from} to {@code to}.
     *
     * @param vehicle what the player rides (drawn as the stand-in), or null to draw nothing in its place
     */
    public static void expect(@Nullable Location from, @Nullable Location to, @Nullable Entity vehicle) {
        Transit.Kind kind = to == null ? Transit.Kind.TRANSFER : from == null ? Transit.Kind.TRANSFER : Transit.kind(from, to);
        TransitView.expectVehicle(kind, from, to, vehicle);
    }
}
