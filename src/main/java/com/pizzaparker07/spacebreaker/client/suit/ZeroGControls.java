package com.pizzaparker07.spacebreaker.client.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.hud.ClientSuitState;
import com.pizzaparker07.spacebreaker.content.suit.Gravity;
import com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload;
import com.pizzaparker07.spacebreaker.content.suit.net.ThrustPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Movement in free fall and with the Maneuvering Unit, for the local player.
 * Free fall: jump/sneak move up/down and walking keys push along the view direction; without a
 * Maneuvering Unit the push is tiny (like pushing off a wall), with one it is a real 6-axis control
 * with attitude hold. In gravity the unit gives lift while jump is held (enough to hover on the Moon,
 * only a softer fall on Earth).
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class ZeroGControls {
    private static boolean thrusting;

    private ZeroGControls() {
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.isPaused()) {
            return;
        }
        boolean thrust = false;
        if (!p.isSpectator() && !p.getAbilities().flying && !p.isPassenger()) {
            SuitStatusPayload status = ClientSuitState.get(mc.level.getGameTime());
            boolean mmu = status != null && status.has(SuitStatusPayload.MMU) && status.mmuFuel() > 0;
            double g = Gravity.factorFor(p);
            boolean up = p.input.jumping;
            boolean down = p.input.shiftKeyDown;
            float fwd = p.input.forwardImpulse;
            float side = p.input.leftImpulse;
            if (g < 0.01 && !p.onGround()) {
                double a = mmu ? 0.03 : 0.004;
                Vec3 look = p.getLookAngle();
                float yaw = p.getYRot() * Mth.DEG_TO_RAD;
                Vec3 left = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
                Vec3 push = look.scale(fwd).add(left.scale(side)).add(0, (up ? 1 : 0) - (down ? 1 : 0), 0);
                if (push.lengthSqr() > 1e-4) {
                    p.setDeltaMovement(p.getDeltaMovement().add(push.normalize().scale(a)));
                    thrust = mmu;
                } else if (mmu) {
                    p.setDeltaMovement(p.getDeltaMovement().scale(0.9)); // attitude/position hold
                }
            } else if (mmu && up && !p.onGround() && p.getDeltaMovement().y < 0.35) {
                p.setDeltaMovement(p.getDeltaMovement().add(0, 0.05, 0));
                p.resetFallDistance();
                thrust = true;
            }
            if (thrust && p.tickCount % 2 == 0) {
                Vec3 back = Vec3.directionFromRotation(0, p.getYRot()).scale(-0.45);
                mc.level.addParticle(ParticleTypes.CLOUD, p.getX() + back.x, p.getY() + 0.9, p.getZ() + back.z, 0, -0.05, 0);
            }
        }
        if (thrust != thrusting) {
            thrusting = thrust;
            PacketDistributor.sendToServer(new ThrustPayload(thrust));
        }
    }
}
