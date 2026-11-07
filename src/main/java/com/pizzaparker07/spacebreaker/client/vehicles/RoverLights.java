package com.pizzaparker07.spacebreaker.client.vehicles;

import com.pizzaparker07.spacebreaker.client.light.LightBeams;
import com.pizzaparker07.spacebreaker.content.vehicles.RoverEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Rover headlights as real moving lights: each lit rover gives two beams from the lamps on its nose, pointing where
 * the body points (it leans with the ground), dipped a little towards the road. The shared light helper turns them
 * into dynamic light with a lights mod, or soft pools of light without one. The visible cones stay in the renderer.
 */
final class RoverLights {
    /** How far the headlights reach, blocks. */
    static final double RANGE = 14;
    static final int LUMINANCE = 13;
    private static final double NEAR_GLOW = 0.12;
    /** Lamps on the nose, model space (pixels): either side of the middle, up from the ground, ahead of the centre. */
    private static final double LAMP_X = 6.5 / 16, LAMP_Y = 10 / 16.0, LAMP_FORWARD = 21.6 / 16;
    /** The beams aim slightly down so they light the ground ahead. */
    private static final double DIP = 0.08;

    private RoverLights() {
    }

    static void collect(Minecraft mc, float pt, Consumer<LightBeams.Beam> out) {
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        for (RoverEntity rover : mc.level.getEntitiesOfClass(RoverEntity.class, new AABB(cam, cam).inflate(64))) {
            if (!RoverRenderer.lightsOn(rover)) {
                continue;
            }
            float yaw = Mth.lerp(pt, rover.yRotO, rover.getYRot()) * Mth.DEG_TO_RAD;
            float pitch = Mth.lerp(pt, rover.bodyPitchO, rover.bodyPitch);
            Vec3 flat = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
            Vec3 right = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
            Vec3 aim = flat.scale(Mth.cos(pitch)).add(0, Mth.sin(pitch) - DIP, 0).normalize();
            Vec3 base = rover.getPosition(pt).add(0, LAMP_Y + Mth.lerp(pt, rover.bodyLiftO, rover.bodyLift), 0)
                    .add(flat.scale(LAMP_FORWARD));
            for (int side = -1; side <= 1; side += 2) {
                Vec3 from = base.add(right.scale(side * LAMP_X));
                Vec3 end = from.add(aim.scale(RANGE));
                BlockHitResult hit = mc.level.clip(new ClipContext(from, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, rover));
                boolean onSurface = hit.getType() == HitResult.Type.BLOCK;
                Vec3 to = onSurface ? hit.getLocation() : end;
                double d = to.distanceTo(from);
                out.accept(new LightBeams.Beam(rover.getUUID() + (side < 0 ? "L" : "R"), from, to, onSurface, hit.getDirection(),
                        LUMINANCE, 1.8 + 0.3 * d, Math.max(0, 1 - d / RANGE), NEAR_GLOW));
            }
        }
    }
}
