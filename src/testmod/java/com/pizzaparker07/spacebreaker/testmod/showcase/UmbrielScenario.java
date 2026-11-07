package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece;
import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.uranus.Umbriel;
import com.pizzaparker07.spacebreaker.content.uranus.entity.UranusEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Umbriel ("umbriel"): Uranus over the dark plains, the bright ring on Wunda's floor, gloam hoppers, a lander. */
final class UmbrielScenario extends OuterShowcase {
    @Override
    public ResourceKey<Level> dimension() {
        return Umbriel.UMBRIEL;
    }

    @Override
    protected void build(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_uranus_over_umbriel", 0, 0, 0.62, 16);
        var wunda = Umbriel.wunda();
        double wz = BasinFeature.centreZ(t, wunda);
        look(level, "02_wunda_floor", wunda.x() - wunda.radius() * 0.85, wz - 20, 8, wunda.x(), wz, at(0.1), 240, MoonScenario::farView);
        look(level, "03_dark_cratered_plains", -200, 120, 5, -500, 260, at(0.12), 240, MoonScenario::farView);
        int y0 = stage(level, Umbriel.REGOLITH.get().defaultBlockState(), Umbriel.ICE.base().get().defaultBlockState(),
                new Block[]{Umbriel.REGOLITH.get(), Umbriel.ICE.base().get(), Umbriel.SOOTY_ICE.get()},
                new Block[]{Umbriel.ICE.stairs().get(), Umbriel.ICE.slab().get(), Umbriel.ICE.wall().get()});
        mob(level, UranusEntities.GLOAM_HOPPER.get(), SX - 2, y0 + 1, SZ + 6, 150F);
        mob(level, UranusEntities.GLOAM_HOPPER.get(), SX + 3, y0 + 1, SZ + 7, 210F);
        shot("04_gloam_hopper", SX + 0.5, y0 + 2.4, SZ + 10.5, 180F, 20F, 60, at(0.12), null);
        lander(level, LanderPiece.Variant.UMBRIEL, "05_crashed_lander", 0.12);
    }
}
