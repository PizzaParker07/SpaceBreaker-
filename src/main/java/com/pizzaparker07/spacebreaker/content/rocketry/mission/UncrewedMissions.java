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
            }
        }
    }

    /** Releases the rocket's satellite (or an orbiting probe) where it is and removes the rocket. */
    public static void deploy(RocketEntity rocket, Location at) {
        if (!(rocket.level() instanceof ServerLevel level)) {
            return;
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
        level.playSound(null, rocket.blockPosition(), com.pizzaparker07.spacebreaker.content.audio.SbSounds.ROCKET_STAGE_SEPARATION.get(),
                SoundSource.NEUTRAL, 2F, 1.2F);
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
        if (!(rocket.level() instanceof ServerLevel level) || payload(rocket).map(p -> p.type() != PartType.PROBE).orElse(true)) {
            return;
        }
        MinecraftServer server = level.getServer();
        SatelliteData data = SatelliteData.get(server);
        UUID owner = rocket.owner();
        data.add(new SatelliteData.Craft(payloadId(rocket).orElseThrow(), at.body(), true, owner, level.getGameTime()));
        data.deliver(server, owner, Research.sample(at.body()).copyWithCount(2));
        data.deliver(server, owner, Research.experimentData(PROBE_LANDING));
        tell(server, owner, Component.translatable("message.spacebreaker.probe.landed", at.name()).withStyle(ChatFormatting.GREEN));
        CoveragePayload.sendToAll(server);
    }

    static void tell(MinecraftServer server, @Nullable UUID owner, Component message) {
        ServerPlayer p = owner == null ? null : server.getPlayerList().getPlayer(owner);
        if (p != null) {
            p.displayClientMessage(message, false);
        }
    }
}
