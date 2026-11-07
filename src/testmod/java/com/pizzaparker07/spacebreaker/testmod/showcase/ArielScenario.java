package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece;
import com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature;
import com.pizzaparker07.spacebreaker.content.uranus.UranusData;
import com.pizzaparker07.spacebreaker.content.uranus.UranusModule;
import com.pizzaparker07.spacebreaker.content.uranus.entity.UranusEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Ariel ("ariel"): Uranus in the sky, a rift valley from its rim and along its floor, the blocks with frost moths, a lander. */
final class ArielScenario extends OuterShowcase {
    @Override
    public ResourceKey<Level> dimension() {
        return UranusModule.ARIEL;
    }

    @Override
    protected void build(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_uranus_over_ariel", 0, 0, 0.62, 20);
        GrabenFeature.Graben g = UranusData.chasmata().grabens().getFirst();
        double cz = -g.latitude() / 90.0 * t.profile().poleDistance();
        double a = Math.toRadians(g.angleDeg());
        double nx = -Math.sin(a);
        double nz = Math.cos(a);
        double ux = Math.cos(a);
        double uz = Math.sin(a);
        // from the rim, looking along and down into the valley
        look(level, "02_rift_valley", g.x() + nx * (g.width() / 2 + 14) - ux * 120, cz + nz * (g.width() / 2 + 14) - uz * 120, 6,
                g.x() + ux * 60, cz + uz * 60, at(0.1), 240, MoonScenario::farView);
        // on the floor, looking along it between the walls
        look(level, "03_valley_floor", g.x() - ux * 200 + nx * 8, cz - uz * 200 + nz * 8, 3, g.x() + ux * 100, cz + uz * 100, at(0.12), 240,
                MoonScenario::farView);
        int y0 = stage(level, UranusModule.ARIEL_REGOLITH.get().defaultBlockState(), UranusModule.ARIEL_ICE.base().get().defaultBlockState(),
                new Block[]{UranusModule.ARIEL_REGOLITH.get(), UranusModule.ARIEL_ICE.base().get(), UranusModule.GRABEN_FLOOR_ICE.get(),
                        com.pizzaparker07.spacebreaker.content.mars.MarsModule.CO2_FROST.get(), UranusModule.ARIEL_AMMONIA_ORE.get()},
                new Block[]{UranusModule.ARIEL_ICE.stairs().get(), UranusModule.ARIEL_ICE.slab().get(), UranusModule.ARIEL_ICE.wall().get()});
        var moth = mob(level, UranusEntities.FROST_MOTH.get(), SX + 1, y0 + 2.5, SZ + 6, 160F);
        if (moth != null) {
            moth.setNoGravity(true);
            moth.setResting(false);
        }
        shot("04_ariel_blocks", SX + 0.5, y0 + 3.4, SZ + 12.5, 180F, 18F, 120, at(0.12), null);
        shot("05_frost_moth", SX + 1.5, y0 + 2.6, SZ + 9.5, 180F, 4F, 60, at(0.12), null);
        lander(level, LanderPiece.Variant.ARIEL, "06_crashed_lander", 0.12);
    }
}
