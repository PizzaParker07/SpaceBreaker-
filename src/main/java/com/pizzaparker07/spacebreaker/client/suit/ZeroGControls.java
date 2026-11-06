package com.pizzaparker07.spacebreaker.client.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.hud.ClientSuitState;
import com.pizzaparker07.spacebreaker.content.suit.Gravity;
import com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload;
import com.pizzaparker07.spacebreaker.content.suit.net.ThrustPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
    private static Vec3 sentDir = Vec3.ZERO;

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
        Vec3 dir = Vec3.ZERO;
        if (!p.isSpectator() && !p.getAbilities().flying && !p.isPassenger()) {
            SuitStatusPayload status = ClientSuitState.get(mc.level.getGameTime());
            boolean mmu = status != null && status.has(SuitStatusPayload.MMU) && status.mmuFuel() > 0;
            double boost = status != null && status.has(SuitStatusPayload.BOOSTER) ? 1.5 : 1.0;
            double g = Gravity.factorFor(p);
            boolean up = p.input.jumping;
            boolean down = p.input.shiftKeyDown;
            float fwd = p.input.forwardImpulse;
            float side = p.input.leftImpulse;
            if (g < 0.01 && !p.onGround()) {
                double a = mmu ? 0.03 * boost : 0.004;
                Vec3 look = p.getLookAngle();
                float yaw = p.getYRot() * Mth.DEG_TO_RAD;
                Vec3 left = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
                Vec3 push = look.scale(fwd).add(left.scale(side)).add(0, (up ? 1 : 0) - (down ? 1 : 0), 0);
                if (push.lengthSqr() > 1e-4) {
                    p.setDeltaMovement(p.getDeltaMovement().add(push.normalize().scale(a)));
                    thrust = mmu;
                    dir = push.normalize();
                } else if (mmu) {
                    p.setDeltaMovement(p.getDeltaMovement().scale(0.9)); // attitude/position hold
                }
            } else if (mmu && up && !p.onGround() && p.getDeltaMovement().y < 0.35) {
                p.setDeltaMovement(p.getDeltaMovement().add(0, 0.05 * boost, 0));
                p.resetFallDistance();
                thrust = true;
                dir = new Vec3(0, 1, 0);
            }
        }
        // the server burns the gas and shows the puffs; tell it when the thrust starts, stops or turns
        if (thrust != thrusting || (thrust && dir.dot(sentDir) < 0.9)) {
            thrusting = thrust;
            sentDir = dir;
            PacketDistributor.sendToServer(new ThrustPayload(thrust, (float) dir.x, (float) dir.y, (float) dir.z));
        }
    }
}
