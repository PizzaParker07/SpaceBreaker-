package com.pizzaparker07.spacebreaker.content.rocketry.mission;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.research.Research;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.part.PartType;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketPart;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * What uncrewed rockets do. They are launched from the launch console with a mission target chosen on the
 * star map; in orbit they burn on to that target by themselves. A satellite is released when its rocket
 * reaches the target orbit; a probe lands on the target world and sends samples and data home.
 */
public final class UncrewedMissions {
    /** Experiment data a released observation satellite sends home (one per world). */
    public static final ResourceLocation ORBITAL_SURVEY = Spacebreaker.id("orbital_survey");
    /** Experiment data a landed probe sends home, with field samples of the world. */
    public static final ResourceLocation PROBE_LANDING = Spacebreaker.id("probe_landing");
    /** Ticks a rocket waits in orbit before burning on to its mission target. */
    public static final int COAST_TICKS = 40;

    private UncrewedMissions() {
    }

    /** The uncrewed payload of a rocket (satellite or probe), if it carries one. */
    public static Optional<RocketPart> payload(RocketEntity rocket) {
        var l = rocket.layout();
        if (!l.payload().present()) {
            return Optional.empty();
        }
        for (int i = l.payload().firstPart(); i <= l.payload().lastPart(); i++) {
            if (l.parts().get(i).type().uncrewed()) {
                return Optional.of(l.parts().get(i));
            }
        }
        return Optional.empty();
    }

    public static Optional<ResourceLocation> payloadId(RocketEntity rocket) {
        var l = rocket.layout();
        for (int i = l.payload().firstPart(); l.payload().present() && i <= l.payload().lastPart(); i++) {
            if (l.parts().get(i).type().uncrewed()) {
                return Optional.of(l.design().parts().get(i));
            }
        }
        return Optional.empty();
    }

    /** What to do on arriving somewhere: carry on to the target, release the satellite, or wait for landing. */
    public enum Next {
        CONTINUE, DEPLOY, NOTHING
    }

    /** Pure decision for an uncrewed rocket that just arrived at {@code at}. */
    public static Next onArrival(PartType payload, Location at, @Nullable Location target) {
        if (!at.orbit()) {
            return Next.NOTHING; // landing: handled at touchdown
        }
        if (target != null && !target.equals(at)) {
            return Next.CONTINUE;
        }
        return payload == PartType.SATELLITE || payload == PartType.PROBE ? Next.DEPLOY : Next.NOTHING;
    }

    /** Called by the rocket after it arrived (server side). */
    public static void arrived(RocketEntity rocket, Location at) {
        Optional<RocketPart> p = payload(rocket);
        if (p.isEmpty()) {
            return;
        }
        switch (onArrival(p.get().type(), at, rocket.missionTarget().orElse(null))) {
            case CONTINUE -> rocket.scheduleTravel(COAST_TICKS);
            case DEPLOY -> deploy(rocket, at);
            default -> {
                if (p.get().type() == PartType.CARGO && at.orbit() && !rocket.docking() && rocket.level() instanceof ServerLevel level) {
                    tell(level.getServer(), rocket.owner(), Component.translatable("message.spacebreaker.cargo.no_station", at.name()).withStyle(ChatFormatting.GOLD));
                }
            }
        }
    }

    /** Releases the rocket's satellite (or an orbiting probe) where it is and removes the rocket. */
    public static void deploy(RocketEntity rocket, Location at) {
        if (!(rocket.level() instanceof ServerLevel level) || payload(rocket).map(p -> p.type() == PartType.CARGO).orElse(false)) {
            return; // cargo is not released: it docks or lands and unloads
        }
        ResourceLocation part = payloadId(rocket).orElse(null);
        if (part == null) {
            return;
        }
        MinecraftServer server = level.getServer();
        SatelliteData data = SatelliteData.get(server);
        UUID owner = rocket.owner();
        data.add(new SatelliteData.Craft(part, at.body(), false, owner, level.getGameTime()));
        level.addFreshEntity(SatelliteEntity.create(level, part, rocket.position().add(0, 2, 0)));
        level.playSound(null, rocket.blockPosition(), com.pizzaparker07.spacebreaker.content.audio.SbSounds.ROCKET_SATELLITE_RELEASE.get(),
                SoundSource.NEUTRAL, 2F, 1F);
        if (part.getPath().equals("observation_satellite")) {
            data.deliver(server, owner, Research.experimentData(ORBITAL_SURVEY));
        }
        tell(server, owner, Component.translatable("message.spacebreaker.satellite.deployed",
                Component.translatable("item." + part.getNamespace() + "." + part.getPath()), at.name()).withStyle(ChatFormatting.GREEN));
        CoveragePayload.sendToAll(server);
        rocket.discard();
    }

    /** Called when an uncrewed rocket touches down: a probe starts its work and sends samples home. */
    public static void landed(RocketEntity rocket, Location at) {
        if (rocket.level() instanceof ServerLevel here && rocket.hasCargoHold()) {
            // unload next to the landing beacon it came down on (or where it stands)
            net.minecraft.core.BlockPos anchor = com.pizzaparker07.spacebreaker.content.rocketry.station.LandingBeacons.get(here)
                    .nearest(rocket.position()).filter(b -> b.closerToCenterThan(rocket.position(), UNLOAD_REACH + 4)).orElse(rocket.blockPosition());
            unloadCargo(rocket, anchor, at);
            return;
        }
        if (!(rocket.level() instanceof ServerLevel level) || payload(rocket).map(p -> p.type() != PartType.PROBE).orElse(true)) {
            return;
        }
        MinecraftServer server = level.getServer();
        SatelliteData data = SatelliteData.get(server);
        UUID owner = rocket.owner();
        data.add(new SatelliteData.Craft(payloadId(rocket).orElseThrow(), at.body(), true, owner, level.getGameTime()));
        level.playSound(null, rocket.blockPosition(), com.pizzaparker07.spacebreaker.content.audio.SbSounds.ROCKET_PROBE_LANDING.get(),
                SoundSource.NEUTRAL, 2F, 1F);
        data.deliver(server, owner, Research.sample(at.body()).copyWithCount(2));
        data.deliver(server, owner, Research.experimentData(PROBE_LANDING));
        tell(server, owner, Component.translatable("message.spacebreaker.probe.landed", at.name()).withStyle(ChatFormatting.GREEN));
        CoveragePayload.sendToAll(server);
    }

    /** How far from the port or beacon the cargo looks for chests, barrels and shulker boxes, blocks. */
    public static final int UNLOAD_REACH = 4;

    /**
     * Unloads a cargo rocket's hold into the chests, barrels and shulker boxes within {@link #UNLOAD_REACH}
     * blocks of {@code anchor} (its docking port or landing beacon). What does not fit stays in the hold. Returns
     * how many items moved.
     */
    /**
     * Storage within reach of {@code anchor}, nearest first: chests, barrels and shulker boxes only, so cargo never
     * ends up in a machine (coal in a generator, regolith in a heater).
     */
    public static java.util.List<net.minecraft.core.BlockPos> storageNear(ServerLevel level, net.minecraft.core.BlockPos anchor) {
        java.util.List<net.minecraft.core.BlockPos> out = new java.util.ArrayList<>();
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(anchor.offset(-UNLOAD_REACH, -UNLOAD_REACH, -UNLOAD_REACH),
                anchor.offset(UNLOAD_REACH, UNLOAD_REACH, UNLOAD_REACH))) {
            var state = level.getBlockState(pos);
            if (state.is(net.neoforged.neoforge.common.Tags.Blocks.CHESTS) || state.is(net.neoforged.neoforge.common.Tags.Blocks.BARRELS)
                    || state.is(net.minecraft.tags.BlockTags.SHULKER_BOXES)) {
                out.add(pos.immutable());
            }
        }
        out.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(anchor)));
        return out;
    }

    public static int unloadCargo(RocketEntity rocket, net.minecraft.core.BlockPos anchor, Location at) {
        if (!(rocket.level() instanceof ServerLevel level)) {
            return 0;
        }
        var hold = rocket.cargo();
        int moved = 0;
        for (net.minecraft.core.BlockPos pos : storageNear(level, anchor)) {
            if (hold.isEmpty()) {
                break;
            }
            var target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, null);
            if (target == null) {
                continue;
            }
            for (int i = 0; i < hold.getContainerSize(); i++) {
                var stack = hold.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                var rest = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
                moved += stack.getCount() - rest.getCount();
                hold.setItem(i, rest);
            }
        }
        int left = 0;
        for (int i = 0; i < hold.getContainerSize(); i++) {
            left += hold.getItem(i).getCount();
        }
        MinecraftServer server = level.getServer();
        if (moved > 0) {
            tell(server, rocket.owner(), Component.translatable("message.spacebreaker.cargo.delivered", at.name(), moved).withStyle(ChatFormatting.GREEN));
        }
        if (left > 0) {
            tell(server, rocket.owner(), Component.translatable("message.spacebreaker.cargo.waiting", at.name(), left).withStyle(ChatFormatting.GOLD));
        }
        return moved;
    }

    static void tell(MinecraftServer server, @Nullable UUID owner, Component message) {
        ServerPlayer p = owner == null ? null : server.getPlayerList().getPlayer(owner);
        if (p != null) {
            p.displayClientMessage(message, false);
        }
    }
}
