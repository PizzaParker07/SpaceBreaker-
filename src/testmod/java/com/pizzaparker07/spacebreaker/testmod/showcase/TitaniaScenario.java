package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece;
import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature;
import com.pizzaparker07.spacebreaker.content.uranus.UranusData;
import com.pizzaparker07.spacebreaker.content.uranus.UranusModule;
import com.pizzaparker07.spacebreaker.content.uranus.entity.UranusEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Titania ("titania"): Uranus in the sky, a great canyon from its rim and its floor, the crater basin, a scree roller, a lander. */
final class TitaniaScenario extends OuterShowcase {
    @Override
    public ResourceKey<Level> dimension() {
        return UranusModule.TITANIA;
    }

    @Override
    protected void build(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_uranus_over_titania", 0, 0, 0.62, 16);
        GrabenFeature.Graben g = UranusData.canyons().grabens().getFirst();
        double cz = -g.latitude() / 90.0 * t.profile().poleDistance();
        double a = Math.toRadians(g.angleDeg());
        double nx = -Math.sin(a);
        double nz = Math.cos(a);
        double ux = Math.cos(a);
        double uz = Math.sin(a);
        look(level, "02_canyon_rim", g.x() + nx * (g.width() / 2 + 18) - ux * 160, cz + nz * (g.width() / 2 + 18) - uz * 160, 6,
                g.x() + ux * 80, cz + uz * 80, at(0.1), 240, MoonScenario::farView);
        look(level, "03_canyon_floor", g.x() - ux * 220, cz - uz * 220, 3, g.x() + ux * 120, cz + uz * 120, at(0.12), 240, MoonScenario::farView);
        var basin = UranusData.greatCrater();
        double bz = BasinFeature.centreZ(t, basin);
        look(level, "04_great_crater", basin.x() + basin.radius() * 0.6, bz + basin.radius() * 0.4, 8, basin.x(), bz, at(0.1), 240,
                MoonScenario::farView);
        int y0 = stage(level, UranusModule.TITANIA_REGOLITH.get().defaultBlockState(), UranusModule.TITANIA_ICE.base().get().defaultBlockState(),
                new Block[]{UranusModule.TITANIA_REGOLITH.get(), UranusModule.TITANIA_ICE.base().get(), UranusModule.CANYON_WALL_ICE.get(),
                        UranusModule.TITANIA_CARBON_ORE.get()},
                new Block[]{UranusModule.TITANIA_ICE.stairs().get(), UranusModule.TITANIA_ICE.slab().get(), UranusModule.TITANIA_ICE.wall().get()});
        mob(level, UranusEntities.SCREE_ROLLER.get(), SX - 2, y0 + 1, SZ + 6, 150F);
        mob(level, UranusEntities.SCREE_ROLLER.get(), SX + 3, y0 + 1, SZ + 7, 210F);
        shot("05_scree_roller", SX + 0.5, y0 + 2.4, SZ + 10.5, 180F, 20F, 60, at(0.12), null);
        lander(level, LanderPiece.Variant.TITANIA, "06_crashed_lander", 0.12);
    }
}
