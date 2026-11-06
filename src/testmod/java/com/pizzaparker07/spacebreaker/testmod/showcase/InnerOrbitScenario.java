package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.client.CameraType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** The inner planets from orbit: Venus with its thick haze, airless cratered Mercury, Mars with Phobos and Deimos. */
final class InnerOrbitScenario implements ShowcaseScenario {
    private static final double Y = 200;

    private static ResourceKey<Level> orbit(String body) {
        return ResourceKey.create(Registries.DIMENSION, Spacebreaker.id(body + "_orbit"));
    }

    @Override
    public ResourceKey<Level> dimension() {
        return orbit("mars");
    }

    @Override
    public void build(ServerLevel level) {
    }

    /** Moves to another orbit and fixes the orbital phase (the sun's place in the sky follows game time). */
    private static void go(ServerPlayer p, String body, long phase, float yaw, float pitch) {
        p.server.getWorldData().overworldData().setGameTime(12000L * 500 + phase);
        ServerLevel level = p.server.getLevel(orbit(body));
        if (level != null) {
            p.teleportTo(level, 0.5, Y - p.getEyeHeight(), 0.5, yaw, pitch);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        CameraType fp = CameraType.FIRST_PERSON;
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        String[] bodies = {"venus", "mercury", "mars"};
        for (int i = 0; i < bodies.length; i++) {
            String b = bodies[i];
            s.add(new ShowcaseDriver.Shot(String.format("%02d_%s_orbit", i * 2 + 1, b), 6000, 0.5, Y, 0.5, 0F, 0F, true, fp, 100,
                    p -> go(p, b, 1500, 200F, 78F), null));
            s.add(new ShowcaseDriver.Shot(String.format("%02d_%s_limb", i * 2 + 2, b), 6000, 0.5, Y, 0.5, 0F, 0F, true, fp, 100,
                    p -> go(p, b, 3000, 20F, 12F), null));
        }
        return s;
    }
}
