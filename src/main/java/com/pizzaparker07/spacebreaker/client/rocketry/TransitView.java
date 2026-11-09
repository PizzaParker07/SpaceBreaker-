package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketGeometry;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketLayout;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Transit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The jump between dimensions, kept out of sight. The views either side of it are built to match ({@link SpaceView}:
 * the top of the climb already looks like the orbit, the start of a descent still does), so the jump itself only has
 * to keep the picture going: no loading screen ({@link TransitScreen} draws nothing, the new world shows through as
 * soon as it exists), the crew arrive straight in their seats, and until the rocket itself has reached the client the
 * view draws a stand-in of it where it will be (the "ghost"), with the crew hidden as they are in the closed capsule.
 */
public final class TransitView {
    /** Give up on the stand-in after this long (the rocket never came: a dead rocket, a refused landing). */
    private static final long GHOST_LIMIT_NANOS = 8_000_000_000L;

    @Nullable
    private static Transit.Kind kind;
    @Nullable
    private static Location to;
    private static float lead;
    private static boolean holding;
    private static long holdSince;
    private static boolean pinned;
    private static float pinnedAlpha;

    // the rocket as last seen before the jump, for the stand-in
    @Nullable
    private static RocketLayout ghostLayout;
    private static int ghostStage;
    private static float ghostYaw;
    private static Vec3 seatOffset = Vec3.ZERO;
    /** Another kind of vehicle (not a rocket) to stand in for, drawn through its own renderer. */
    @Nullable
    private static net.minecraft.world.entity.Entity ghostVehicle;

    /** See {@link SeamlessHandOff#expect}. */
    static void expectVehicle(Transit.Kind k, @Nullable Location from, @Nullable Location dest, @Nullable net.minecraft.world.entity.Entity vehicle) {
        begin(k, from, dest);
        var player = Minecraft.getInstance().player;
        ghostVehicle = vehicle instanceof RocketEntity ? null : vehicle;
        if (vehicle instanceof RocketEntity r) {
            ghostLayout = r.layout();
            ghostStage = r.activeStage();
            ghostYaw = r.getYRot();
        } else if (vehicle == null) {
            ghostLayout = null;
        }
        if (player != null && vehicle != null) {
            seatOffset = player.position().subtract(vehicle.position());
        }
    }

    private TransitView() {
    }

    /** Called every client tick while the player rides a rocket. */
    static void update(RocketEntity rocket) {
        if (pinned) {
            return;
        }
        FlightState s = rocket.state();
        // remember the rocket and where the crew sit in it, for the stand-in after the jump
        var player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() == rocket && !holding) {
            ghostLayout = rocket.layout();
            // coming home to the Earth only the capsule comes down (the stages stay in orbit)
            Location dest = rocket.destination();
            ghostStage = s == FlightState.TRANSFER && dest != null && dest.isEarth() && !dest.orbit()
                    ? rocket.layout().stages().size() : rocket.activeStage();
            ghostYaw = rocket.getYRot();
            seatOffset = player.position().subtract(rocket.position());
        }
        float l = Transit.leadIn(s, rocket.timer(), rocket.ascentProgress());
        if (l > 0 && kind == null) {
            Location here = rocket.location();
            Location dest = s == FlightState.ASCENT ? RocketEntity.launchTarget(here) : rocket.destination();
            if (dest != null) {
                begin(Transit.kind(here, dest), here, dest);
            }
        }
        lead = l;
        if (l <= 0 && !holding && kind != null) {
            reset(); // aborted (failed ascent, cancelled travel)
        }
    }

    static void begin(Transit.Kind k, @Nullable Location here, @Nullable Location dest) {
        kind = k;
        to = dest;
    }

    /** The dimension is changing ({@link TransitScreen} is up): from now the stand-in takes the rocket's place. */
    static void hold() {
        holding = true;
        holdSince = System.nanoTime();
    }

    /** The rocket has reached the client and the crew are in their seats again: the real one takes over. */
    static void arrived() {
        reset();
        handedOver = System.nanoTime();
    }

    private static long handedOver = Long.MIN_VALUE / 2;

    /** True during a hand-over and for a moment after it: the camera keeps its settings across the change of rocket. */
    static boolean handingOver() {
        return holding || System.nanoTime() - handedOver < 2_000_000_000L;
    }

    static boolean holding() {
        return holding;
    }

    static boolean active() {
        return kind != null || holding;
    }

    /** Whether the stand-in is drawn now. */
    static boolean ghost() {
        if (!holding || ghostLayout == null && ghostVehicle == null) {
            return false;
        }
        if (System.nanoTime() - holdSince > GHOST_LIMIT_NANOS) {
            reset();
            return false;
        }
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        // the game draws an entity only once the terrain section it is in has been built: until then the stand-in
        // stays, even with the crew back in their seats
        var vehicle = player.getVehicle();
        return vehicle == null || !drawable(vehicle);
    }

    /** Whether the game draws this entity yet (its section built, or it is outside the build height). */
    static boolean drawable(net.minecraft.world.entity.Entity e) {
        var mc = Minecraft.getInstance();
        var at = e.blockPosition();
        return mc.level == null || mc.level.isOutsideBuildHeight(at) || mc.levelRenderer.isSectionCompiled(at);
    }

    /**
     * Draws the stand-in rocket where the real one will appear: under the seated player. The pose is camera-relative
     * (as in an after-entities render stage).
     */
    static void renderGhost(PoseStack pose, MultiBufferSource buffers, Vec3 camera, boolean firstPerson) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Vec3 at = player.position().subtract(seatOffset);
        if (ghostVehicle != null) {
            // someone else's vehicle: its own renderer, at its last pose
            var mc = Minecraft.getInstance();
            mc.getEntityRenderDispatcher().render(ghostVehicle, at.x - camera.x, at.y - camera.y, at.z - camera.z, ghostVehicle.getYRot(),
                    mc.getTimer().getGameTimeDeltaPartialTick(false), pose, buffers, LightTexture.FULL_BRIGHT);
            return;
        }
        if (ghostLayout == null) {
            return;
        }
        RocketGeometry geo = RocketGeometry.ofActive(ghostLayout, ghostStage);
        pose.pushPose();
        pose.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
        pose.mulPose(Axis.YP.rotationDegrees(-ghostYaw));
        int skipFrom = firstPerson && ghostLayout.payload().present() ? ghostLayout.payload().firstPart() : Integer.MAX_VALUE;
        RocketStackRenderer.render(ghostLayout, geo, pose, buffers, LightTexture.FULL_BRIGHT, 0F, skipFrom);
        pose.popPose();
    }

    /**
     * For screenshots: shows (or with a null kind, clears) a dip at a fixed strength until cleared.
     *
     * @param alpha how dark (0 = clear, 1 = black)
     */
    public static void showcase(@Nullable Transit.Kind k, @Nullable Location here, @Nullable Location dest, float alpha) {
        reset();
        if (k != null) {
            begin(k, here, dest);
            pinned = true;
            pinnedAlpha = alpha;
        }
    }

    private static void reset() {
        ghostVehicle = null;
        pinned = false;
        kind = null;
        to = null;
        lead = 0;
        holding = false;
    }

    /** How strongly a dip covers the world right now: only the showcase's pinned dip (the flight never dims). */
    static float alpha() {
        return pinned ? pinnedAlpha : 0F;
    }

    @Nullable
    static Location destination() {
        return to;
    }

    /** The showcase's pinned dip. */
    static void render(GuiGraphics g, int w, int h, float alpha) {
        if (!pinned || alpha <= 0.003F) {
            return;
        }
        float a = Mth.clamp(alpha, 0, 1);
        g.fill(0, 0, w, h, (int) (a * a * (3 - 2 * a) * 255) << 24);
    }
}
