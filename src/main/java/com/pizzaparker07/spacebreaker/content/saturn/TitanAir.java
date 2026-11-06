package com.pizzaparker07.spacebreaker.content.saturn;

import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Titan's thick, cold air: four times the density of Earth's under a seventh of its gravity. Falling bodies quickly
 * reach a gentle terminal speed, so long falls do no harm, and wings work far better than at home - an elytra glides for
 * a long way and even gains a little height from speed. Players' movement is simulated on their own client (where this
 * runs for them); every other creature is handled on the server.
 */
public final class TitanAir {
    /** Terminal falling speed in blocks per tick. */
    public static final double TERMINAL = 0.32;
    /** Drag per tick on the falling speed. */
    public static final double DRAG = 0.9;
    /** Upward lift per tick per block-per-tick of horizontal speed while gliding. */
    public static final double LIFT = 0.045;

    private TitanAir() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(EntityTickEvent.Post.class, e -> apply(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(LivingFallEvent.class, e -> {
            if (e.getEntity().level().dimension() == SaturnModule.TITAN) {
                // a terminal-speed landing in thick air: like stepping off a block or two
                e.setDistance(Math.min(e.getDistance(), 2.5F));
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p && e.getTo() == SaturnModule.TITAN) {
                p.displayClientMessage(Component.translatable("message.spacebreaker.titan_air").withStyle(ChatFormatting.GOLD), false);
            }
        });
    }

    /** Applies Titan's air drag and wing lift to one entity for one tick. */
    public static void apply(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity.level().dimension() != SaturnModule.TITAN) {
            return;
        }
        boolean player = entity instanceof Player;
        if (player != entity.level().isClientSide) {
            return;
        }
        if (living.onGround() || living.isInWater() || living.isInLava() || living.isNoGravity()
                || living instanceof Player p && p.getAbilities().flying) {
            return;
        }
        Vec3 v = living.getDeltaMovement();
        if (living.isFallFlying()) {
            double speed = Math.sqrt(v.x * v.x + v.z * v.z);
            living.setDeltaMovement(v.x, v.y + speed * LIFT, v.z);
        } else if (v.y < 0) {
            living.setDeltaMovement(v.x * 0.98, Math.max(v.y * DRAG, -TERMINAL), v.z * 0.98);
        }
    }

    static void translations(Lang lang) {
        lang.add("message.spacebreaker.titan_air",
                "Titan: 1.5 bar of cold nitrogen and no oxygen. Keep your helmet on - but in air this thick, falls are gentle and wings carry you far.",
                "Titan: 1,5 bar soğuk azot, hiç oksijen yok. Kaskını çıkarma - ama bu kadar yoğun havada düşüşler yumuşaktır ve kanatlar seni uzağa taşır.");
    }
}
