package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.belt.TinyWorld;
import com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Locale;

/** A tiny moon ("nix", "hydra", "proteus"): its parent in the sky, the lumpy ground near the landing site, the lander. */
final class TinyMoonScenario extends OuterShowcase {
    private final TinyWorld world;
    private final double extraX;
    private final double extraLatitude;
    private final String extra;

    /** @param extra name of an extra view toward (extraX, extraLatitude), or null */
    TinyMoonScenario(TinyWorld world, String extra, double extraX, double extraLatitude) {
        this.world = world;
        this.extra = extra;
        this.extraX = extraX;
        this.extraLatitude = extraLatitude;
    }

    @Override
    public ResourceKey<Level> dimension() {
        return world.level;
    }

    @Override
    protected void build(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_sky_over_" + world.name, 0, 0, 0.62, 14);
        look(level, "02_" + world.name + "_surface", 40, 30, 6, -120, -60, at(0.12), 220, MoonScenario::farView);
        if (extra != null) {
            double z = -extraLatitude / 90.0 * t.profile().poleDistance();
            look(level, "03_" + extra, extraX + 200, z + 60, 20, extraX, z, at(0.12), 240, MoonScenario::farView);
        }
        lander(level, LanderPiece.Variant.valueOf(world.name.toUpperCase(Locale.ROOT)), "04_crashed_lander", 0.12);
    }
}
