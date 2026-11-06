package com.pizzaparker07.spacebreaker.client.rocketry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The rocket camera. Boarding a rocket switches to an orbiting third-person view far enough out to see the
 * whole stack (move the mouse to fly round it, scroll to zoom); F5 still cycles to the cockpit view, where
 * everything below the capsule is drawn and the window frame is a GUI layer. Leaving the rocket restores the
 * camera the player had. Off when the {@code cinematicCamera} client option is off.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class RocketCamera {
    public static final float MIN_DISTANCE = 4F;
    public static final float MAX_DISTANCE = 64F;

    @Nullable
    private static CameraType before;
    private static int riddenId = -1;
    private static float zoom = -1;

    private RocketCamera() {
    }

    static boolean enabled() {
        return !SbConfig.CLIENT_SPEC.isLoaded() || SbConfig.CINEMATIC_CAMERA.get();
    }

    /** A camera distance that shows a rocket of this many blocks whole. */
    public static float distanceFor(double heightBlocks) {
        return Mth.clamp((float) heightBlocks * 1.1F + 4F, 8F, 40F);
    }

    /** The zoom after one wheel notch (up = closer). */
    public static float zoomed(float current, double scroll) {
        return Mth.clamp(current * (float) Math.pow(0.88, scroll), MIN_DISTANCE, MAX_DISTANCE);
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        RocketEntity rocket = RocketHud.ridden();
        if (rocket != null && rocket.getId() != riddenId) {
            riddenId = rocket.getId();
            zoom = distanceFor(rocket.geometry().height() / 16.0);
            if (enabled() && before == null) {
                before = mc.options.getCameraType();
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        } else if (rocket == null && riddenId != -1) {
            riddenId = -1;
            if (before != null && mc.player != null && !mc.player.isPassenger()) {
                mc.options.setCameraType(before);
            }
            before = null;
        }
    }

    @SubscribeEvent
    static void distance(CalculateDetachedCameraDistanceEvent event) {
        if (RocketHud.ridden() != null && zoom > 0) {
            event.setDistance(zoom);
        }
    }

    @SubscribeEvent
    static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null && RocketHud.ridden() != null && !mc.options.getCameraType().isFirstPerson() && zoom > 0) {
            zoom = zoomed(zoom, event.getScrollDeltaY());
            event.setCanceled(true);
        }
    }

    /** No arm and held item in front of the cockpit window. */
    @SubscribeEvent
    static void hand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        if (inCockpit()) {
            event.setCanceled(true);
        }
    }

    /** While the transit view covers the screen, vanilla HUD pieces (crosshair, hotbar...) are hidden. */
    @SubscribeEvent
    static void layers(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre event) {
        if (TransitView.alpha() > 0.9F && !event.getName().equals(Spacebreaker.id("rocket_overlay"))) {
            event.setCanceled(true);
        }
    }

    /** Whether the camera sits in the capsule of the rocket being ridden. */
    public static boolean inCockpit() {
        Minecraft mc = Minecraft.getInstance();
        return RocketHud.ridden() != null && mc.options.getCameraType().isFirstPerson();
    }
}
