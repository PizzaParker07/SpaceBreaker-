package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece;
import com.pizzaparker07.spacebreaker.content.uranus.Oberon;
import com.pizzaparker07.spacebreaker.content.uranus.entity.UranusEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Oberon ("oberon"): Uranus low in the sky, the great mountain over the plains, dark-floored craters, the chasm, rim burrowers, a lander. */
final class OberonScenario extends OuterShowcase {
    @Override
    public ResourceKey<Level> dimension() {
        return Oberon.OBERON;
    }

    @Override
    protected void build(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_uranus_over_oberon", 0, 0, 0.62, 12);
        look(level, "02_great_mountain", Oberon.MOUNTAIN_X + 260, Oberon.MOUNTAIN_Z + 40, 6, Oberon.MOUNTAIN_X, Oberon.MOUNTAIN_Z, at(0.1), 240,
                MoonScenario::farView);
        look(level, "03_dark_floored_craters", 60, -60, 70, 220, -220, at(0.12), 240, MoonScenario::farView);
        var g = Oberon.chasma().grabens().getFirst();
        double cz = -g.latitude() / 90.0 * t.profile().poleDistance();
        double a = Math.toRadians(g.angleDeg());
        look(level, "04_mommur_chasma", g.x() - Math.cos(a) * 220, cz - Math.sin(a) * 220, 4, g.x() + Math.cos(a) * 120, cz + Math.sin(a) * 120,
                at(0.12), 240, MoonScenario::farView);
        int y0 = stage(level, Oberon.REGOLITH.get().defaultBlockState(), Oberon.ICE.base().get().defaultBlockState(),
                new Block[]{Oberon.REGOLITH.get(), Oberon.ICE.base().get(), Oberon.DARK_FLOOR.get()},
                new Block[]{Oberon.ICE.stairs().get(), Oberon.ICE.slab().get(), Oberon.ICE.wall().get()});
        mob(level, UranusEntities.RIM_BURROWER.get(), SX - 2, y0 + 1, SZ + 6, 150F);
        mob(level, UranusEntities.RIM_BURROWER.get(), SX + 3, y0 + 1, SZ + 7, 210F);
        shot("05_rim_burrower", SX + 0.5, y0 + 2.4, SZ + 10.5, 180F, 20F, 60, at(0.12), null);
        lander(level, LanderPiece.Variant.OBERON, "06_crashed_lander", 0.12);
    }
}
