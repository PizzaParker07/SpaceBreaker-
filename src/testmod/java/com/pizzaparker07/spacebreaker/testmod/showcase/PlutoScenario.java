package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece;
import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.pluto.PlutoData;
import com.pizzaparker07.spacebreaker.content.pluto.PlutoModule;
import com.pizzaparker07.spacebreaker.content.pluto.entity.PlutoEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Pluto ("pluto"): Charon still in the black sky, the heart's nitrogen plain and its convection cells, the water-ice
 * mountains on its shore, the blue haze layers at sunrise, the blocks with nitrogen sliders and a crashed lander.
 * Charon ("charon"): Pluto over the grey plains, the chasm belt, the red polar cap, the blocks with frost tortoises and
 * a crashed lander.
 */
final class PlutoScenario extends OuterShowcase {
    private final boolean charon;

    PlutoScenario(boolean charon) {
        this.charon = charon;
    }

    @Override
    public ResourceKey<Level> dimension() {
        return charon ? PlutoModule.CHARON : PlutoModule.PLUTO;
    }

    @Override
    protected void build(ServerLevel level, PlanetTerrain t) {
        if (charon) {
            charon(level, t);
        } else {
            pluto(level, t);
        }
    }

    private void pluto(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_charon_over_pluto", 0, 0, 0.62, 22);
        double hx = PlutoData.HEART_X;
        double hz = BasinFeature.centreZ(t, PlutoData.heart());
        double r = PlutoData.HEART_RADIUS;
        look(level, "02_heart_plain", hx - r * 0.55, hz + 20, 14, hx, hz - 40, at(0.12), 240, MoonScenario::farView);
        look(level, "03_convection_cells", hx + 30, hz + 26, 22, hx, hz, at(0.14), 200, MoonScenario::farView);
        double mx = hx - r - 40;
        double mz = hz - 170;
        look(level, "04_ice_mountains", mx + 85, mz + 45, 3, mx, mz, at(0.1), 240, MoonScenario::farView);
        // the haze layers stand out low over the horizon with the sun just up
        double gy = ground(level, 0, 0);
        shot("05_blue_haze", 0.5, gy + 3, 0.5, -90F, -4F, 160, at(0.27), MoonScenario::farView);
        int y0 = stage(level, PlutoModule.PLUTONIAN_FROST.get().defaultBlockState(), PlutoModule.PLUTONIAN_ICE.base().get().defaultBlockState(),
                new Block[]{PlutoModule.PLUTONIAN_FROST.get(), PlutoModule.THOLIN_REGOLITH.get(), PlutoModule.NITROGEN_ICE.get(),
                        PlutoModule.METHANE_FROST.get(), PlutoModule.PLUTONIAN_ICE.base().get(), PlutoModule.PLUTONIAN_THOLIN_ORE.get()},
                new Block[]{PlutoModule.PLUTONIAN_ICE.stairs().get(), PlutoModule.PLUTONIAN_ICE.slab().get(), PlutoModule.PLUTONIAN_ICE.wall().get()});
        for (int x = SX - 6; x <= SX + 6; x++) {
            for (int z = SZ + 3; z <= SZ + 7; z++) {
                level.setBlock(new BlockPos(x, y0, z), PlutoModule.NITROGEN_ICE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        mob(level, PlutoEntities.NITROGEN_SLIDER.get(), SX - 2, y0 + 1, SZ + 5, 120F);
        mob(level, PlutoEntities.NITROGEN_SLIDER.get(), SX + 3, y0 + 1, SZ + 4, 230F);
        shot("06_pluto_blocks", SX + 0.5, y0 + 3.4, SZ + 12.5, 180F, 18F, 120, at(0.12), null);
        shot("07_nitrogen_slider", SX + 0.5, y0 + 2.2, SZ + 9.5, 180F, 24F, 60, at(0.12), null);
        lander(level, LanderPiece.Variant.PLUTO, "08_crashed_lander", 0.12);
    }

    private void charon(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_pluto_over_charon", 0, 0, 0.62, 20);
        double rz = t.riftCentre(200);
        if (!Double.isNaN(rz)) {
            look(level, "02_chasm_belt", 200, rz + 72, 6, 480, rz, at(0.1), 240, MoonScenario::farView);
        }
        double pole = t.profile().poleDistance();
        double capZ = -pole * 0.8;
        look(level, "03_red_polar_cap", 60, capZ + 90, 8, 0, capZ, at(0.1), 220, MoonScenario::farView);
        int y0 = stage(level, PlutoModule.CHARON_REGOLITH.get().defaultBlockState(), PlutoModule.CHARON_ICE.get().defaultBlockState(),
                new Block[]{PlutoModule.CHARON_REGOLITH.get(), PlutoModule.CHARON_ICE.get(), PlutoModule.RED_POLAR_FROST.get(),
                        PlutoModule.AMMONIA_HYDRATE_ORE.get()}, new Block[0]);
        mob(level, PlutoEntities.FROST_TORTOISE.get(), SX - 2, y0 + 1, SZ + 5, 160F);
        mob(level, PlutoEntities.FROST_TORTOISE.get(), SX + 3, y0 + 1, SZ + 6, 210F);
        shot("04_charon_blocks", SX + 0.5, y0 + 3.4, SZ + 12.5, 180F, 18F, 120, at(0.12), null);
        shot("05_frost_tortoise", SX + 0.5, y0 + 2.4, SZ + 10.5, 180F, 24F, 60, at(0.12), null);
        lander(level, LanderPiece.Variant.CHARON, "06_crashed_lander", 0.12);
    }
}
