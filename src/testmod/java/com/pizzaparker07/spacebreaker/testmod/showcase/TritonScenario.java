package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece;
import com.pizzaparker07.spacebreaker.content.belt.world.VentFeature;
import com.pizzaparker07.spacebreaker.content.neptune.NeptuneData;
import com.pizzaparker07.spacebreaker.content.neptune.NeptuneModule;
import com.pizzaparker07.spacebreaker.content.neptune.entity.NeptuneEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Triton ("triton"): Neptune in the black sky, the cantaloupe terrain, a nitrogen geyser erupting in the pink polar cap,
 * the blocks with plume beetles and a crashed lander.
 */
final class TritonScenario extends OuterShowcase {
    @Override
    public ResourceKey<Level> dimension() {
        return NeptuneModule.TRITON;
    }

    @Override
    protected void build(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_neptune_over_triton", 0, 0, 0.62, 22);
        look(level, "02_cantaloupe_terrain", 330, 230, 16, 280, 190, at(0.12), 220, MoonScenario::farView);
        double pole = t.profile().poleDistance();
        VentFeature.Config geysers = NeptuneData.geysers();
        BlockPos vent = null;
        int gz0 = (int) (pole * 0.75 / geysers.cell());
        for (int[] g : grid(-3, 3)) {
            int[] v = VentFeature.vent(t, geysers, g[0], gz0 + g[1]);
            if (v != null) {
                vent = findBlock(level, v[0], v[1], 3, NeptuneModule.NITROGEN_GEYSER.get());
                if (vent != null) {
                    break;
                }
            }
        }
        if (vent != null) {
            BlockPos v = vent;
            double y = Math.max(ground(level, v.getX() + 18, v.getZ() + 12), v.getY() + 1) + 5;
            shot("03_nitrogen_geyser", v.getX() + 18.5, y, v.getZ() + 12.5, MoonScenario.yaw(-18, -12), -20F, 140, erupting(v, 0.15, false),
                    MoonScenario::farView);
        }
        look(level, "04_pink_polar_cap", 60, pole * 0.7 + 40, 10, 0, pole * 0.7, at(0.12), 220, MoonScenario::farView);
        int y0 = stage(level, NeptuneModule.TRITON_FROST.get().defaultBlockState(), NeptuneModule.TRITON_ICE.base().get().defaultBlockState(),
                new Block[]{NeptuneModule.TRITON_FROST.get(), NeptuneModule.PINK_NITROGEN_FROST.get(), NeptuneModule.PLUME_DUST.get(),
                        NeptuneModule.TRITON_ICE.base().get(), NeptuneModule.CLATHRATE_ORE.get(), NeptuneModule.NITROGEN_GEYSER.get()},
                new Block[]{NeptuneModule.TRITON_ICE.stairs().get(), NeptuneModule.TRITON_ICE.slab().get(), NeptuneModule.TRITON_ICE.wall().get()});
        mob(level, NeptuneEntities.PLUME_BEETLE.get(), SX - 2, y0 + 1, SZ + 5, 150F);
        var flying = mob(level, NeptuneEntities.PLUME_BEETLE.get(), SX + 3, y0 + 2.5, SZ + 6, 220F);
        if (flying != null) {
            flying.setNoGravity(true);
        }
        shot("05_triton_blocks", SX + 0.5, y0 + 3.4, SZ + 12.5, 180F, 18F, 120, at(0.12), null);
        shot("06_plume_beetle", SX + 0.5, y0 + 2.4, SZ + 10.5, 180F, 20F, 60, at(0.12), null);
        // on the flat polar cap, away from the pits of the cantaloupe terrain
        lander(level, LanderPiece.Variant.TRITON, "07_crashed_lander", 0.12, 120, (int) (pole * 0.66));
        // the geyser watch station, also on the cap
        int gx = -140;
        int gz = (int) (pole * 0.66);
        int gy = (int) ground(level, gx + 7, gz + 7);
        var station = new com.pizzaparker07.spacebreaker.content.neptune.dungeon.GeyserStationPiece(71L, gx, gy, gz, net.minecraft.core.Direction.SOUTH);
        MarsScenario.placePiece(level, station);
        BlockPos centre = station.world(7, 3, 7);
        BlockPos out = station.world(7, 3, -9);
        shot("08_geyser_station", out.getX() + 3.5, out.getY() + 5, out.getZ() + 0.5,
                MoonScenario.yaw(centre.getX() - out.getX() - 3, centre.getZ() - out.getZ()), 12F, 140, at(0.12), MoonScenario::farView);
        BlockPos door = station.world(7, 3, 2);
        shot("09_geyser_station_inside", door.getX() + 0.5, door.getY() + 1.6, door.getZ() + 0.5,
                MoonScenario.yaw(centre.getX() - door.getX(), centre.getZ() - door.getZ()), 15F, 100, erupting(station.world(7, 2, 7), 0.12, false), null);
        BlockPos high = station.world(7, 8, 3);
        shot("09b_geyser_station_floor", high.getX() + 0.5, high.getY(), high.getZ() + 0.5,
                MoonScenario.yaw(centre.getX() - high.getX(), centre.getZ() - high.getZ()), 52F, 100, erupting(station.world(7, 2, 7), 0.12, false), null);
    }
}
