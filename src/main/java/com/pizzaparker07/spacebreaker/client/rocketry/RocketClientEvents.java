package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketGeometry;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.net.RocketActionPayload;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client game-bus hooks: crew keys, exhaust/smoke particles, camera shake and the sky darkening with altitude. */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class RocketClientEvents {
    private static ResourceKey<Level> lastDimension;
    private static float shake;
    private static int unseated;
    private static final java.util.Map<Integer, EngineSound> SOUNDS = new java.util.HashMap<>();
    private static final java.util.Map<Integer, EngineSound> ENTRY_SOUNDS = new java.util.HashMap<>();

    private RocketClientEvents() {
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        RocketEntity ridden = RocketHud.ridden();
        if (ridden != null) {
            TransitView.update(ridden);
        }
        SpaceView.tickLods();
        if (TransitView.active() && TransitView.holding() && !(mc.screen instanceof TransitScreen)) {
            // the new dimension is on screen: fade the view out into it once the crew is back in the seat
            // (the passenger follows the rocket a few ticks later), or after two seconds in any case
            if (ridden != null && TransitView.drawable(ridden) || ++unseated > 60) {
                unseated = 0;
                TransitView.arrived();
            }
        } else if (ridden != null && lastDimension != null && lastDimension != level.dimension() && !TransitView.active()) {
            RocketHud.markArrival();
        }
        lastDimension = level.dimension();
        while (RocketryClient.LAUNCH.consumeClick()) {
            if (ridden != null) {
                PacketDistributor.sendToServer(new RocketActionPayload(ridden.getId(), RocketActionPayload.LAUNCH, 0));
            }
        }
        while (RocketryClient.FLIGHT_DETAILS.consumeClick()) {
            if (ridden != null) {
                RocketHud.toggleDetails();
            }
        }
        while (RocketryClient.STAR_MAP.consumeClick()) {
            if (ridden != null && mc.screen == null) {
                mc.setScreen(new StarMapScreen(ridden));
            }
        }
        float targetShake = 0;
        int quality = SbConfig.CLIENT_SPEC.isLoaded() ? SbConfig.EFFECTS_QUALITY.get().ordinal() + 1 : 3;
        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof RocketEntity rocket) || rocket.design().isEmpty()) {
                continue;
            }
            double dist = rocket.distanceTo(mc.player);
            if (dist > 200) {
                continue;
            }
            effects(level, rocket, quality);
            staging(level, rocket, rocket == ridden, quality);
            loop(mc, rocket, SOUNDS, EngineSound.Kind.ENGINE);
            loop(mc, rocket, ENTRY_SOUNDS, EngineSound.Kind.REENTRY);
            if (rocket.burning()) {
                boolean riding = rocket == ridden;
                double ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, rocket.getBlockX(), rocket.getBlockZ());
                boolean nearGround = rocket.getY() - ground < 60 && Location.EARTH_SURFACE.equals(rocket.location());
                float s = riding ? rideShake(rocket) : 0;
                if (nearGround && !riding) {
                    // the ground shakes for everyone near the pad
                    s = Math.max(s, (float) Mth.clamp(1.4 - dist / 60.0, 0, 1.4) * (rocket.state() == FlightState.COUNTDOWN ? 0.6F : 1F));
                }
                if (!rocket.location().equals(Location.EARTH_SURFACE) && !riding) {
                    s = 0;
                }
                targetShake = Math.max(targetShake, s);
            }
            if (rocket == ridden && rocket.state() == FlightState.REENTRY) {
                targetShake = Math.max(targetShake, 1.0F);
            }
        }
        if (jolt > 0) {
            targetShake = Math.max(targetShake, 1.3F * jolt / JOLT_TICKS);
            jolt--;
        }
        shake = Mth.lerp(0.25F, shake, targetShake);
        SOUNDS.values().removeIf(EngineSound::isStopped);
        ENTRY_SOUNDS.values().removeIf(EngineSound::isStopped);
        MissionVoice.tick(ridden);
    }

    /** Ticks a staging jolt lasts. */
    private static final int JOLT_TICKS = 10;
    private static int jolt;
    private static final java.util.Map<Integer, Integer> STAGES = new java.util.HashMap<>();

    /**
     * How hard the ride shakes the crew: the engines spooling up on the pad, a hard kick at liftoff, a rumble that
     * follows the thrust and fades as the air thins, and a short buffet through maximum dynamic pressure.
     */
    static float rideShake(RocketEntity rocket) {
        FlightState st = rocket.state();
        float air = Location.EARTH_SURFACE.equals(rocket.location()) || rocket.location().atmosphere() && !rocket.location().orbit()
                ? 1F - spaceFactor() : 0F;
        if (st == FlightState.COUNTDOWN) {
            float spool = Mth.clamp((RocketEntity.IGNITION_TICKS - rocket.timer()) / (float) RocketEntity.IGNITION_TICKS, 0, 1);
            return 0.15F + 0.55F * spool;
        }
        if (st == FlightState.ASCENT) {
            float t = rocket.timer();
            float liftoff = 1.5F * (float) Math.exp(-t / 40.0);
            float rumble = 0.15F + 0.45F * air;
            float p = rocket.ascentProgress();
            float maxQ = air > 0 ? 0.9F * (float) Math.exp(-Math.pow((p - 0.3) / 0.05, 2)) : 0F;
            return Math.max(liftoff, rumble + maxQ);
        }
        return 0.3F; // other burns: a steady push
    }

    /** A spent stage falls away: a ring of smoke where it separated, and a jolt for the crew. */
    private static void staging(ClientLevel level, RocketEntity rocket, boolean ridden, int quality) {
        int stage = rocket.activeStage();
        Integer before = STAGES.put(rocket.getId(), stage);
        if (before == null || stage <= before || rocket.state() != FlightState.ASCENT && rocket.state() != FlightState.ORBIT) {
            return;
        }
        if (ridden) {
            jolt = JOLT_TICKS;
        }
        RandomSource rng = level.random;
        for (int i = 0; i < 18 * quality; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double v = 0.15 + rng.nextDouble() * 0.2;
            level.addParticle(ParticleTypes.CLOUD, rocket.getX() + Math.cos(a) * 1.1, rocket.getY() - 0.3, rocket.getZ() + Math.sin(a) * 1.1,
                    Math.cos(a) * v, -0.05, Math.sin(a) * v);
        }
    }

    private static void loop(Minecraft mc, RocketEntity rocket, java.util.Map<Integer, EngineSound> playing, EngineSound.Kind kind) {
        if (!EngineSound.wanted(rocket, kind)) {
            return;
        }
        EngineSound snd = playing.get(rocket.getId());
        if (snd == null || snd.isStopped()) {
            snd = new EngineSound(rocket, kind);
            playing.put(rocket.getId(), snd);
            mc.getSoundManager().play(snd);
        }
    }

    private static void effects(ClientLevel level, RocketEntity rocket, int quality) {
        RandomSource rng = level.random;
        FlightState state = rocket.state();
        RocketGeometry geo = rocket.geometry();
        double x = rocket.getX(), y = rocket.getY(), z = rocket.getZ();
        boolean airless = !Location.EARTH_SURFACE.equals(rocket.location());
        // LOX boil-off from a fuelled rocket waiting on Earth
        if ((state == FlightState.IDLE || state == FlightState.COUNTDOWN) && !airless && rocket.fuelFraction() > 0.05 && rng.nextInt(4) == 0) {
            double h = rng.nextDouble() * geo.payloadBottom() / 16.0;
            double a = rng.nextDouble() * Math.PI * 2;
            level.addParticle(ParticleTypes.CLOUD, x + Math.cos(a) * 1.05, y + h, z + Math.sin(a) * 1.05,
                    Math.cos(a) * 0.03, -0.01, Math.sin(a) * 0.03);
        }
        if (!rocket.burning()) {
            return;
        }
        double tilt = Math.toRadians(rocket.tilt());
        double bx = x - Math.sin(tilt) * 0.2, by = y - 0.2;
        double ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, rocket.getBlockX(), rocket.getBlockZ());
        double h = y - ground;
        // a nuclear thermal stage throws out hot hydrogen: no flame, no smoke
        if (com.pizzaparker07.spacebreaker.content.rocketry.entity.NuclearExhaust.reactorHot(rocket)) {
            return;
        }
        if (airless) {
            int n = quality;
            for (int i = 0; i < n; i++) {
                level.addParticle(ParticleTypes.FLAME, bx + rng.nextGaussian() * 0.25, by - rng.nextDouble() * 1.5, z + rng.nextGaussian() * 0.25,
                        rng.nextGaussian() * 0.02, -0.4 - rng.nextDouble() * 0.3, rng.nextGaussian() * 0.02);
            }
            if (state == FlightState.DESCENT && h < 12) {
                for (int i = 0; i < quality * 2; i++) {
                    double a = rng.nextDouble() * Math.PI * 2;
                    level.addParticle(ParticleTypes.WHITE_ASH, x + Math.cos(a) * 2, ground + 0.2, z + Math.sin(a) * 2,
                            Math.cos(a) * 0.6, 0.02, Math.sin(a) * 0.6);
                }
            }
            return;
        }
        // In air the flame cone, the glow on the pad and the smoke column are drawn by the renderer. Here: the ground
        // cloud, a few big billows of steam and exhaust thrown sideways out of the flame trench, low over the pad.
        float power;
        if (state == FlightState.COUNTDOWN) {
            power = 0.35F + 0.65F * Mth.clamp((RocketEntity.IGNITION_TICKS - rocket.timer()) / 30F, 0, 1);
        } else if (h < 40) {
            power = (float) (1 - h / 40.0);
        } else {
            return;
        }
        float count = quality * 0.55F * power;
        int n = (int) count + (rng.nextFloat() < count - (int) count ? 1 : 0);
        for (int i = 0; i < n; i++) {
            int side = rng.nextBoolean() ? 1 : -1;
            double speed = 0.3 + rng.nextDouble() * 0.45 * power;
            boolean along = rng.nextInt(4) == 0; // some roll out across the trench too, so it reads as one cloud
            double vx = along ? rng.nextGaussian() * 0.06 : side * speed;
            double vz = along ? side * speed * 0.55 : rng.nextGaussian() * 0.07;
            level.addAlwaysVisibleParticle(com.pizzaparker07.spacebreaker.content.rocketry.RocketryParticles.STEAM.get(), true,
                    x + vx * 4, ground + 1.0 + rng.nextDouble() * 0.8, z + vz * 4, vx, 0.004 + rng.nextDouble() * 0.01, vz);
        }
    }

    /**
     * The crew sit inside the closed capsule: like on a real capsule they are not seen from outside (their model would
     * otherwise poke through the capsule walls in third person, on every phase of the flight).
     */
    /** Between the jump and the rocket's arrival on the client: the stand-in rocket, and the crew hidden in it. */
    @SubscribeEvent
    static void ghost(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !TransitView.ghost()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        var buffers = mc.renderBuffers().bufferSource();
        TransitView.renderGhost(event.getPoseStack(), buffers, event.getCamera().getPosition(), mc.options.getCameraType().isFirstPerson());
        buffers.endBatch();
    }

    @SubscribeEvent
    static void hideCrew(net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity().getVehicle() instanceof com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity rocket
                && rocket.layout().payload().present()
                || event.getEntity() == Minecraft.getInstance().player && TransitView.ghost()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        var player = event.getEntity();
        if (player == null || !(event.getItemStack().getItem() instanceof com.pizzaparker07.spacebreaker.content.rocketry.item.RocketPartItem)) {
            return;
        }
        if (!com.pizzaparker07.spacebreaker.content.rocketry.part.RocketTech.unlocked(player, event.getItemStack())) {
            var tech = com.pizzaparker07.spacebreaker.content.rocketry.part.RocketTech.techFor(player, event.getItemStack());
            if (tech != null) {
                event.getToolTip().add(com.pizzaparker07.spacebreaker.content.rocketry.part.RocketTech.requires(tech)
                        .copy().withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
    }

    @SubscribeEvent
    static void login(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        com.pizzaparker07.spacebreaker.content.rocketry.nav.AstroTransferModel.install(event.getPlayer().level().registryAccess());
    }

    @SubscribeEvent
    static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (shake < 0.01F || !SbConfig.CLIENT_SPEC.isLoaded() || !SbConfig.CINEMATIC_CAMERA.get()) {
            return;
        }
        float t = (float) (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime() + event.getPartialTick());
        float a = shake * 0.9F;
        event.setPitch(event.getPitch() + Mth.sin(t * 1.9F) * a * 0.6F + Mth.sin(t * 5.3F) * a * 0.3F);
        event.setYaw(event.getYaw() + Mth.sin(t * 2.7F + 1F) * a * 0.4F);
        event.setRoll(event.getRoll() + Mth.sin(t * 3.1F + 2F) * a * 0.5F);
    }

    /** 0 at the surface, 1 where the view is the orbit's (see {@link SpaceView#factor}). */
    static float spaceFactor() {
        return SpaceView.factor();
    }

    /** True for the overworld's sky effects while the camera is high enough that its clouds are left out. */
    public static boolean cloudsBelowView(net.minecraft.client.renderer.DimensionSpecialEffects effects) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.effects() == effects && spaceFactor() > CLOUDS_GONE;
    }

    /** Above this (camera ~y 325) the vanilla clouds go; the Earth's own cloud layer is in by then. */
    private static final float CLOUDS_GONE = 0.1F;

    /** The air thins: the fog draws back with altitude and is gone well before the sky turns black. */
    // last, and also after a world's own fog handler has claimed the event: high up the far ground is the ball, with
    // no band of fog between it and the terrain
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST, receiveCanceled = true)
    static void fogDistance(ViewportEvent.RenderFog event) {
        if (event.getMode() == net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN) {
            SpaceView.terrainFog(event.getFarPlaneDistance(), event.getFogShape() == com.mojang.blaze3d.shaders.FogShape.SPHERE);
        }
        float k = spaceFactor();
        float clear = SpaceView.clearAir(k);
        if (clear <= 0) {
            return;
        }
        event.setFarPlaneDistance(event.getFarPlaneDistance() * (1 + 30 * clear));
        event.setNearPlaneDistance(event.getNearPlaneDistance() * (1 + 30 * clear));
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void fog(ViewportEvent.ComputeFogColor event) {
        // the horizon turns a clear, saturated blue as the air thins (never grey), then fades to black at the top
        float f = spaceFactor();
        if (f <= 0) {
            return;
        }
        var here = SpaceView.surface(Minecraft.getInstance().level);
        if (here == null || !here.atmosphere()) {
            return; // no air, no blue: the world's own (black) sky all the way up
        }
        float clear = Mth.clamp(f * 3F, 0, 1);
        float keep = 1 - Mth.clamp((f - 0.45F) / 0.55F, 0, 1);
        event.setRed(Mth.lerp(clear, event.getRed(), 0.38F) * keep);
        event.setGreen(Mth.lerp(clear, event.getGreen(), 0.62F) * keep);
        event.setBlue(Mth.lerp(clear, event.getBlue(), 1.0F) * keep);
    }

    @SubscribeEvent
    static void sky(RenderLevelStageEvent event) {
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            SpaceView.sky(event.getModelViewMatrix(), event.getProjectionMatrix(), partial);
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            SpaceView.overTerrain(event.getModelViewMatrix(), partial);
        }
    }
}
