package com.pizzaparker07.spacebreaker.testmod.showcase;

import net.minecraft.server.level.ServerLevel;

import java.util.List;

/** The first style sample: ores, Moon rocks, solar trackers, helmets, oxygen tank. */
final class StyleSampleScenario implements ShowcaseScenario {
    @Override
    public void build(ServerLevel level) {
        ShowcaseBuilder.build(level);
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        return ShowcaseShots.forScenario("style_sample");
    }
}
