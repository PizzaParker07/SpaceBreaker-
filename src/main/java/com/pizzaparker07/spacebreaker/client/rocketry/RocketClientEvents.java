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
    private static final java.util.Map<Integer, EngineSound> SOUNDS = new java.util.HashMap<>();

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
        if (TransitView.active() && TransitView.holding() && !(mc.screen instanceof TransitScreen)) {
            TransitView.arrived(); // the new dimension is on screen: fade the view out into it
        } else if (ridden != null && lastDimension != null && lastDimension != level.dimension() && !TransitView.active()) {
            RocketHud.markArrival();
        }
        lastDimension = level.dimension();
        while (RocketryClient.LAUNCH.consumeClick()) {
            if (ridden != null) {
                PacketDistributor.sendToServer(new RocketActionPayload(ridden.getId(), RocketActionPayload.LAUNCH, 0));
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
            if (rocket.burning()) {
                EngineSound snd = SOUNDS.get(rocket.getId());
                if (snd == null || snd.isStopped()) {
                    snd = new EngineSound(rocket);
                    SOUNDS.put(rocket.getId(), snd);
                    mc.getSoundManager().play(snd);
                }
            }
            if (rocket.burning()) {
                boolean riding = rocket == ridden;
                double ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, rocket.getBlockX(), rocket.getBlockZ());
                boolean nearGround = rocket.getY() - ground < 60 && rocket.location() == Location.EARTH_SURFACE;
                float s = riding ? (rocket.state() == FlightState.ASCENT && rocket.timer() < 300 ? 1.2F : 0.5F) : 0;
                if (nearGround) {
                    s = Math.max(s, (float) Mth.clamp(1.4 - dist / 60.0, 0, 1.4));
                }
                if (rocket.state() == FlightState.COUNTDOWN) {
                    s *= 0.6F;
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
        shake = Mth.lerp(0.2F, shake, targetShake);
        SOUNDS.values().removeIf(EngineSound::isStopped);
    }

    private static void effects(ClientLevel level, RocketEntity rocket, int quality) {
        RandomSource rng = level.random;
        FlightState state = rocket.state();
        RocketGeometry geo = rocket.geometry();
        double x = rocket.getX(), y = rocket.getY(), z = rocket.getZ();
        boolean airless = rocket.location() != Location.EARTH_SURFACE;
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
        int n = quality * (airless ? 1 : 3);
        for (int i = 0; i < n; i++) {
            level.addParticle(ParticleTypes.FLAME, bx + rng.nextGaussian() * 0.25, by - rng.nextDouble() * 1.5, z + rng.nextGaussian() * 0.25,
                    rng.nextGaussian() * 0.02, -0.4 - rng.nextDouble() * 0.3, rng.nextGaussian() * 0.02);
        }
        if (airless) {
            if (state == FlightState.DESCENT && h < 12) {
                for (int i = 0; i < quality * 2; i++) {
                    double a = rng.nextDouble() * Math.PI * 2;
                    level.addParticle(ParticleTypes.WHITE_ASH, x + Math.cos(a) * 2, ground + 0.2, z + Math.sin(a) * 2,
                            Math.cos(a) * 0.6, 0.02, Math.sin(a) * 0.6);
                }
            }
            return;
        }
        // exhaust trail
        for (int i = 0; i < quality * 2; i++) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, bx + rng.nextGaussian() * 0.4, by - 2 - rng.nextDouble() * 3, z + rng.nextGaussian() * 0.4,
                    rng.nextGaussian() * 0.05, -0.2, rng.nextGaussian() * 0.05);
        }
        if (rng.nextInt(2) == 0) {
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, bx + rng.nextGaussian() * 0.5, by - 3,
                    z + rng.nextGaussian() * 0.5, rng.nextGaussian() * 0.02, 0.02, rng.nextGaussian() * 0.02);
        }
        // the launch cloud: exhaust deflected sideways along the flame trench
        if (h < 30) {
            float power = (float) (1 - h / 30.0);
            int clouds = (int) (quality * 4 * power) + 1;
            for (int i = 0; i < clouds; i++) {
                int side = rng.nextBoolean() ? 1 : -1;
                double speed = 0.35 + rng.nextDouble() * 0.45;
                level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x + side * (1.5 + rng.nextDouble()),
                        ground + 0.3 + rng.nextDouble() * 0.8, z + rng.nextGaussian() * 0.8,
                        side * speed, 0.02 + rng.nextDouble() * 0.05, rng.nextGaussian() * 0.08);
                if (rng.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.CLOUD, x + side * 2.5, ground + 0.5, z + rng.nextGaussian(),
                            side * speed * 1.4, 0.05, rng.nextGaussian() * 0.1);
                }
            }
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

    /** 0 at the surface, 1 where the sky has turned black (rocket ascent in the overworld). */
    static float spaceFactor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != Level.OVERWORLD) {
            return 0;
        }
        double y = mc.gameRenderer.getMainCamera().getPosition().y;
        return (float) Mth.clamp((y - 300) / 700.0, 0, 1);
    }

    @SubscribeEvent
    static void fog(ViewportEvent.ComputeFogColor event) {
        float k = spaceFactor();
        if (k > 0) {
            float keep = 1 - k;
            event.setRed(event.getRed() * keep);
            event.setGreen(event.getGreen() * keep);
            event.setBlue(event.getBlue() * keep);
        }
    }

    @SubscribeEvent
    static void sky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        float k = spaceFactor();
        if (k <= 0) {
            return;
        }
        int alpha = (int) (Mth.clamp(k * 1.1F, 0, 1) * 235);
        SkyDraw.blackout(event.getModelViewMatrix(), alpha << 24);
        SkyDraw.stars(event.getModelViewMatrix(), event.getProjectionMatrix(), k);
        // the sun stays bright in space
        Minecraft mc = Minecraft.getInstance();
        float theta = mc.level.getTimeOfDay(event.getPartialTick().getGameTimeDeltaPartialTick(false)) * Mth.TWO_PI;
        SkyDraw.billboard(event.getModelViewMatrix(), OrbitSkyEffects.SUN, new org.joml.Vector3f(-Mth.sin(theta), Mth.cos(theta), 0), 100F, 15F, k);
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
    }
}
