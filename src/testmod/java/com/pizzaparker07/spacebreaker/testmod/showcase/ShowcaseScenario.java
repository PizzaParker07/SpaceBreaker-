package com.pizzaparker07.spacebreaker.testmod.showcase;

import net.minecraft.server.level.ServerLevel;

import java.util.List;

/** A scripted showcase: builds a scene on the server, then a list of camera shots. */
public interface ShowcaseScenario {
    /** Dimension to build and shoot in (default: overworld). */
    default net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension() {
        return net.minecraft.world.level.Level.OVERWORLD;
    }

    void build(ServerLevel level);

    List<ShowcaseDriver.Shot> shots();
}
