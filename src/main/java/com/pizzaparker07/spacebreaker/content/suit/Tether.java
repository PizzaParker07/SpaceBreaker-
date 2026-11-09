package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.content.suit.net.TetherPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.pizzaparker07.spacebreaker.core.env.TetherAnchor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Safety tether: a line from the suit's waist to a block (a station hull, a rocket, a handrail) or to an entity that
 * offers a clip ring ({@link com.pizzaparker07.spacebreaker.core.env.TetherAnchor}, a spacecraft). While
 * clipped the wearer cannot drift further than {@link #LENGTH} blocks from the anchor, which also stops a
 * fall once the line is taut. Movement is held on the client like all player movement (client.suit
 * TetherClient), the server checks the line: it comes loose when the anchor block goes, the wearer changes
 * dimension, drops the tether item or is pulled far beyond its length.
 */
public final class Tether {
    public static final double LENGTH = 16.0;
    /** Further than this (teleport, lag) the clip lets go instead of yanking the wearer back. */
    public static final double BREAK_DISTANCE = LENGTH + 8.0;
    /** Suited players drifting this close to a moving anchor (a craft) are carried along with it. */
    public static final double CO_MOVE_RANGE = 32.0;

    private Tether() {
    }

    /** Where the line is clipped on a block: the middle of the anchor block. */
    public static Vec3 anchorPoint(BlockPos anchor) {
        return Vec3.atCenterOf(anchor);
    }

    /** Where the line leaves the suit: the waist, at the back of the belt. */
    public static Vec3 waist(Player player) {
        return player.position().add(0, player.getBbHeight() * 0.5, 0);
    }

    private static boolean sameWorld(Player player, SurvivalData d) {
        return d.tetherDim != null && player.level().dimension().location().equals(d.tetherDim);
    }

    /** The anchor block, when clipped to a block in this world. */
    public static Optional<BlockPos> anchor(Player player) {
        SurvivalData d = LifeSupport.data(player);
        if (d.tether == null || !sameWorld(player, d)) {
            return Optional.empty();
        }
        return Optional.of(d.tether);
    }

    /** The anchor entity, when clipped to one in this world that is loaded (server). */
    public static Optional<Entity> anchorEntity(Player player) {
        SurvivalData d = LifeSupport.data(player);
        if (d.tetherEntity == null || !sameWorld(player, d) || !(player.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        Entity e = level.getEntity(d.tetherEntity);
        return e instanceof TetherAnchor && e.isAlive() ? Optional.of(e) : Optional.empty();
    }

    /** Clipped to anything (a block or an entity) in this world. */
    public static boolean isTethered(Player player) {
        SurvivalData d = LifeSupport.data(player);
        return (d.tether != null || d.tetherEntity != null) && sameWorld(player, d);
    }

    /** Where the line holds now: the anchor block's centre or the entity's clip ring (server). */
    public static Optional<Vec3> anchorPoint(Player player) {
        Optional<BlockPos> block = anchor(player);
        if (block.isPresent()) {
            return Optional.of(anchorPoint(block.get()));
        }
        return anchorEntity(player).map(e -> ((TetherAnchor) e).tetherPoint(player));
    }

    public static void clip(Player player, BlockPos anchor) {
        SurvivalData d = LifeSupport.data(player);
        d.tether = anchor.immutable();
        d.tetherEntity = null;
        d.tetherDim = player.level().dimension().location();
        clipped(player, anchorPoint(anchor));
    }

    /** Clips the line to an entity's ring (it must be a {@link TetherAnchor} that accepts the player). */
    public static boolean clip(Player player, Entity anchor) {
        if (!(anchor instanceof TetherAnchor a) || !a.acceptsTether(player)) {
            return false;
        }
        SurvivalData d = LifeSupport.data(player);
        d.tether = null;
        d.tetherEntity = anchor.getUUID();
        d.tetherDim = player.level().dimension().location();
        clipped(player, a.tetherPoint(player));
        return true;
    }

    private static void clipped(Player player, Vec3 at) {
        player.level().playSound(null, at.x, at.y, at.z, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8F, 1.3F);
        player.displayClientMessage(Component.translatable("message.spacebreaker.tether.clipped", (int) LENGTH), true);
        sync(player);
    }

    public static void release(Player player, String reasonKey) {
        SurvivalData d = LifeSupport.data(player);
        if (d.tether == null && d.tetherEntity == null) {
            return;
        }
        d.tether = null;
        d.tetherEntity = null;
        d.tetherDim = null;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 0.8F, 1.1F);
        player.displayClientMessage(Component.translatable(reasonKey), true);
        sync(player);
    }

    /** What the clients need to draw and hold this player's line. */
    public static TetherPayload payload(Player player) {
        return new TetherPayload(player.getId(), anchor(player), anchorEntity(player).map(Entity::getId));
    }

    /** Tells the wearer and everyone who can see them where the line is. */
    public static void sync(Player player) {
        if (player instanceof ServerPlayer sp) {
            com.pizzaparker07.spacebreaker.content.suit.net.SuitNetwork.sendNearAndSelf(sp, payload(sp));
        }
    }

    /** Server tick: checks that the line still holds; a taut line also takes the fall out of the wearer. */
    public static void tick(Player player) {
        SurvivalData d = LifeSupport.data(player);
        if (d.tether == null && d.tetherEntity == null) {
            return;
        }
        if (!sameWorld(player, d)) {
            release(player, "message.spacebreaker.tether.released");
            return;
        }
        if (player.tickCount % 10 == 0) {
            boolean lost = d.tether != null
                    ? !player.level().isLoaded(d.tether) || player.level().getBlockState(d.tether).isAir()
                    : anchorEntity(player).isEmpty();
            if (lost) {
                release(player, "message.spacebreaker.tether.anchor_lost");
                return;
            }
            if (!player.isCreative() && !player.getInventory().hasAnyMatching(s -> s.is(SuitModule.SAFETY_TETHER.get()))) {
                release(player, "message.spacebreaker.tether.released");
                return;
            }
        }
        Optional<Vec3> at = anchorPoint(player);
        if (at.isEmpty()) {
            return; // an entity anchor between checks, not loaded this tick
        }
        double dist = waist(player).distanceTo(at.get());
        if (dist > BREAK_DISTANCE) {
            release(player, "message.spacebreaker.tether.snapped");
        } else if (dist >= LENGTH - 0.5) {
            player.resetFallDistance();
        }
    }

    /**
     * The velocity a wearer at {@code pos} moving with {@code velocity} may keep: once the line is taut the
     * outward part is taken away and a gentle pull brings an overshoot back to the length of the line.
     */
    public static Vec3 constrain(Vec3 pos, Vec3 velocity, Vec3 anchor, double length) {
        Vec3 out = pos.subtract(anchor);
        double dist = out.length();
        if (dist <= length || dist < 1e-6) {
            return velocity;
        }
        Vec3 n = out.scale(1.0 / dist);
        double outward = velocity.dot(n);
        Vec3 v = outward > 0 ? velocity.subtract(n.scale(outward * 1.05)) : velocity;
        return v.subtract(n.scale(Math.min(0.25, (dist - length) * 0.15)));
    }
}
