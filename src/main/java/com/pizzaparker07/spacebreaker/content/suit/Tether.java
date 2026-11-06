package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.content.suit.net.TetherPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/**
 * Safety tether: a line from the suit's waist to a block (a station hull, a rocket, a handrail). While
 * clipped the wearer cannot drift further than {@link #LENGTH} blocks from the anchor, which also stops a
 * fall once the line is taut. Movement is held on the client like all player movement (client.suit
 * TetherClient), the server checks the line: it comes loose when the anchor block goes, the wearer changes
 * dimension, drops the tether item or is pulled far beyond its length.
 */
public final class Tether {
    public static final double LENGTH = 16.0;
    /** Further than this (teleport, lag) the clip lets go instead of yanking the wearer back. */
    public static final double BREAK_DISTANCE = LENGTH + 8.0;

    private Tether() {
    }

    /** Where the line is clipped: the middle of the anchor block. */
    public static Vec3 anchorPoint(BlockPos anchor) {
        return Vec3.atCenterOf(anchor);
    }

    /** Where the line leaves the suit: the waist, at the back of the belt. */
    public static Vec3 waist(Player player) {
        return player.position().add(0, player.getBbHeight() * 0.5, 0);
    }

    public static Optional<BlockPos> anchor(Player player) {
        SurvivalData d = LifeSupport.data(player);
        if (d.tether == null || !player.level().dimension().location().equals(d.tetherDim)) {
            return Optional.empty();
        }
        return Optional.of(d.tether);
    }

    public static void clip(Player player, BlockPos anchor) {
        SurvivalData d = LifeSupport.data(player);
        d.tether = anchor.immutable();
        d.tetherDim = player.level().dimension().location();
        player.level().playSound(null, anchor, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8F, 1.3F);
        player.displayClientMessage(Component.translatable("message.spacebreaker.tether.clipped", (int) LENGTH), true);
        sync(player);
    }

    public static void release(Player player, String reasonKey) {
        SurvivalData d = LifeSupport.data(player);
        if (d.tether == null) {
            return;
        }
        BlockPos was = d.tether;
        d.tether = null;
        d.tetherDim = null;
        player.level().playSound(null, was, SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 0.8F, 1.1F);
        player.displayClientMessage(Component.translatable(reasonKey), true);
        sync(player);
    }

    /** Tells the wearer and everyone who can see them where the line is. */
    public static void sync(Player player) {
        if (player instanceof ServerPlayer sp && sp.connection != null && sp.connection.hasChannel(TetherPayload.TYPE)) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp, new TetherPayload(sp.getId(), anchor(sp)));
        }
    }

    /** Server tick: checks that the line still holds; a taut line also takes the fall out of the wearer. */
    public static void tick(Player player) {
        SurvivalData d = LifeSupport.data(player);
        if (d.tether == null) {
            return;
        }
        if (!player.level().dimension().location().equals(d.tetherDim)) {
            release(player, "message.spacebreaker.tether.released");
            return;
        }
        if (player.tickCount % 10 == 0) {
            if (!player.level().isLoaded(d.tether) || player.level().getBlockState(d.tether).isAir()) {
                release(player, "message.spacebreaker.tether.anchor_lost");
                return;
            }
            if (!player.isCreative() && !player.getInventory().hasAnyMatching(s -> s.is(SuitModule.SAFETY_TETHER.get()))) {
                release(player, "message.spacebreaker.tether.released");
                return;
            }
        }
        double dist = waist(player).distanceTo(anchorPoint(d.tether));
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
