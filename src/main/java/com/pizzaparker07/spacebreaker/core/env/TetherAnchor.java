package com.pizzaparker07.spacebreaker.core.env;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * An entity a safety tether can be clipped to (a spacecraft's hatch ring, ...), implemented by the entity. The
 * wearer uses the tether item on it; the line then holds to {@link #tetherPoint} wherever the entity moves.
 *
 * <p>A craft that moves through the world blocks (a flight scene is centred on its main body, not on the craft)
 * reports how far it moves each tick in {@link #frameVelocity}: tethered wearers and suited players drifting near it
 * are carried along by that much, so an EVA beside a moving craft stays beside it.
 */
public interface TetherAnchor {
    /** Where the line is clipped, in world coordinates. */
    Vec3 tetherPoint(Player player);

    /** Whether this player may clip a line here now. */
    default boolean acceptsTether(Player player) {
        return true;
    }

    /** The anchor's own motion, blocks per tick, carried over to tethered and nearby EVA players. */
    default Vec3 frameVelocity() {
        return Vec3.ZERO;
    }
}
