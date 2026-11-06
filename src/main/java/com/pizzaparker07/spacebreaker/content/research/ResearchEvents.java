package com.pizzaparker07.spacebreaker.content.research;

import com.pizzaparker07.spacebreaker.content.research.net.ResearchNet;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.core.tech.TechChangedEvent;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Game-bus wiring: client sync, advancements, team and offline research hand-over. */
final class ResearchEvents {
    static void register() {
        NeoForge.EVENT_BUS.addListener(ResearchEvents::onTechChanged);
        NeoForge.EVENT_BUS.addListener(ResearchEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(ResearchEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(ResearchEvents::onDimension);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> ResearchCommands.register(e.getDispatcher()));
    }

    private static void onTechChanged(TechChangedEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        List<ResourceLocation> learned = new ArrayList<>(event.getLearned());
        ResearchNet.sync(player, learned);
        int count = Tech.known(player).size();
        for (ResourceLocation tech : learned) {
            String category = Tech.get(player.level().registryAccess(), tech).map(TechNode::category).orElse("");
            ResearchModule.TECH_LEARNED.get().trigger(player, tech, category, count);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Set<ResourceLocation> catchUp = new HashSet<>(ResearchSavedData.get(player.server).takePending(player.getUUID()));
        PlayerTeam team = player.getTeam();
        if (team != null) {
            catchUp.addAll(ResearchSavedData.get(player.server).teamTech(team.getName()));
        }
        catchUp.removeIf(t -> Tech.knows(player, t));
        for (ResourceLocation tech : catchUp) {
            Tech.learn(player, tech); // posts TechChangedEvent -> sync
        }
        ResearchNet.sync(player, List.of());
        // every new player gets the field guide once
        var tag = player.getPersistentData();
        if (!tag.getBoolean(GOT_GUIDE)) {
            tag.putBoolean(GOT_GUIDE, true);
            net.minecraft.world.item.ItemStack book = new net.minecraft.world.item.ItemStack(ResearchModule.GUIDE_BOOK.get());
            if (!player.getInventory().add(book)) {
                player.drop(book, false);
            }
        }
    }

    private static final String GOT_GUIDE = "spacebreaker:received_guide";

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResearchNet.sync(player, List.of());
        }
    }

    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResearchNet.sync(player, List.of());
        }
    }

    private ResearchEvents() {
    }
}
