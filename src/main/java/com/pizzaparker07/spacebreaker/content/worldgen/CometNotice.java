package com.pizzaparker07.spacebreaker.content.worldgen;

import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * A great comet is a rare event (Halley returns every 76 years): when one shows in a player's sky, mission control
 * tells them once per return where to look. Checked every couple of minutes on the server.
 */
public final class CometNotice {
    private static final int EVERY = 2400;

    private CometNotice() {
    }

    static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % EVERY == 20) {
                check(p);
            }
        });
    }

    static void check(ServerPlayer player) {
        var level = player.level();
        if (Bodies.isOrbit(level)) {
            return;
        }
        var access = level.registryAccess();
        var observer = Environment.body(level).flatMap(b -> Bodies.registry(access).map(r -> r.getKey(b)));
        var sys = BodyAdapter.system(access);
        if (observer.isEmpty() || sys.isEmpty()) {
            return;
        }
        double t = BodyAdapter.simTime(level);
        for (CometPoints.Sighting s : CometPoints.compute(sys.get(), observer.get().toString(), t)) {
            if (!s.great()) {
                continue;
            }
            Comet c = Comet.byId(s.id()).orElse(null);
            long passage = c == null ? 0 : Math.round(c.nextPerihelion(t - c.periodSeconds() / 2) / 86400.0);
            String key = "spacebreaker_comet_" + s.id();
            if (player.getPersistentData().getLong(key) == passage + 1) {
                continue;
            }
            player.getPersistentData().putLong(key, passage + 1);
            player.sendSystemMessage(Component.translatable("message.spacebreaker.great_comet", Component.translatable("body.spacebreaker." + s.id())).withStyle(ChatFormatting.AQUA));
        }
    }
}
