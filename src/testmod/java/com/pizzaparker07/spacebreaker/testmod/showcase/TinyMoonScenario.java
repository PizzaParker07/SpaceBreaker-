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
        if (world == com.pizzaparker07.spacebreaker.content.belt.Comets.CHURYUMOV) {
            // a cluster of jets in noon sunlight, seen from a few blocks away
            int gx = 60;
            int gz = -40;
            for (int[] o : new int[][]{{0, 0}, {3, 1}, {-2, 3}}) {
                int y = (int) ground(level, gx + o[0], gz + o[1]);
                level.setBlock(new net.minecraft.core.BlockPos(gx + o[0], y, gz + o[1]),
                        com.pizzaparker07.spacebreaker.content.belt.Comets.JET.get().defaultBlockState(), 3);
            }
            double y = ground(level, gx + 7, gz - 7) + 2.6;
            shot("03_comet_jets", gx + 7.5, y, gz - 6.5, MoonScenario.yaw(-7, 7), 10F, 160, at(0.25), null);
        }
        lander(level, LanderPiece.Variant.valueOf(world.name.toUpperCase(Locale.ROOT)), "04_crashed_lander", 0.12);
    }
}
