package com.pizzaparker07.spacebreaker.core.tech;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

import java.util.Set;

/** Posted on the NeoForge event bus (server side) whenever a player's known technologies change. */
public class TechChangedEvent extends Event {
    private final Player player;
    private final Set<ResourceLocation> learned;
    private final Set<ResourceLocation> forgotten;

    public TechChangedEvent(Player player, Set<ResourceLocation> learned, Set<ResourceLocation> forgotten) {
        this.player = player;
        this.learned = learned;
        this.forgotten = forgotten;
    }

    public Player getPlayer() {
        return player;
    }

    public Set<ResourceLocation> getLearned() {
        return learned;
    }

    public Set<ResourceLocation> getForgotten() {
        return forgotten;
    }
}
