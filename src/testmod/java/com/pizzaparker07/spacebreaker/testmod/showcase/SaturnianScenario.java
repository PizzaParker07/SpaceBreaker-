package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.belt.world.LakeFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.LineaeFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.VentFeature;
import com.pizzaparker07.spacebreaker.content.jupiter.JupiterModule;
import com.pizzaparker07.spacebreaker.content.saturn.SaturnData;
import com.pizzaparker07.spacebreaker.content.saturn.SaturnModule;
import com.pizzaparker07.spacebreaker.content.saturn.dungeon.HangarPiece;
import com.pizzaparker07.spacebreaker.content.saturn.entity.SaturnEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Saturn's moons. Titan ("saturnian"): the orange haze over the dune sea, a methane lake with skaters on its surface,
 * the blocks and the rotorcraft hangar. Enceladus: Saturn 28° wide with its rings nearly edge-on over the bright
 * snow, the tiger stripes and their geysers, a geyser drifter.
 */
final class SaturnianScenario extends OuterShowcase {
    private final boolean enceladus;

    SaturnianScenario(boolean enceladus) {
        this.enceladus = enceladus;
    }

    @Override
    public ResourceKey<Level> dimension() {
        return enceladus ? SaturnModule.ENCELADUS : SaturnModule.TITAN;
    }

    @Override
    protected void build(ServerLevel level, PlanetTerrain t) {
        if (enceladus) {
            enceladus(level, t);
        } else {
            titan(level, t);
        }
    }

    private void titan(ServerLevel level, PlanetTerrain t) {
        double[] dunes = MoonScenario.find(t, (x, z) -> t.column(x, z).dune > 0.8, 4000);
        if (dunes != null) {
            look(level, "01_titan_dunes", dunes[0], dunes[1], 6, dunes[0] + 80, dunes[1] + 10, at(0.2), 220, MoonScenario::farView);
        }
        LakeFeature.Config lakes = SaturnData.titanLakes();
        double pole = t.profile().poleDistance();
        int row = (int) (pole * 0.75 / lakes.cell());
        LakeFeature.Lake lake = null;
        for (int[] g : grid(-4, 4)) {
            LakeFeature.Lake l = LakeFeature.lake(t, lakes, g[0], row + g[1]);
            if (l != null && l.radius() > 20) {
                lake = l;
                break;
            }
        }
        if (lake != null) {
            double sx = lake.x() + lake.radius() * 1.3;
            double sz = lake.z() + lake.radius() * 0.5;
            for (int i = 0; i < 3; i++) {
                double mx = lake.x() + (i - 1) * 4 + lake.radius() * 0.6;
                double mz = lake.z() + i * 3;
                ground(level, mx, mz);
                mob(level, SaturnEntities.METHANE_SKATER.get(), mx, lake.level() + 1, mz, 120F + i * 40);
            }
            look(level, "02_methane_lake", sx, sz, 4, lake.x(), lake.z(), at(0.22), 220, MoonScenario::farView);
            shot("03_methane_rain", sx + 0.5, ground(level, sx, sz) + 2, sz + 0.5, MoonScenario.yaw(lake.x() - sx, lake.z() - sz), 5F, 160, p -> {
                at(0.2).accept(p);
                long now = p.serverLevel().getGameTime();
                long w = com.pizzaparker07.spacebreaker.content.saturn.TitanWeather.WINDOW;
                for (long k = now / w; k < now / w + 400; k++) {
                    if (com.pizzaparker07.spacebreaker.content.saturn.TitanWeather.rain(k * w + w / 2) > 0.9F) {
                        p.getServer().getWorldData().overworldData().setGameTime(k * w + w / 2);
                        break;
                    }
                }
            }, null);
        }
        int y0 = stage(level, SaturnModule.THOLIN_MUD.get().defaultBlockState(), SaturnModule.TITAN_ICE.base().get().defaultBlockState(),
                new Block[]{SaturnModule.THOLIN_SAND.get(), SaturnModule.THOLIN_MUD.get(), SaturnModule.TITAN_ICE.base().get(),
                        SaturnModule.ICE_COBBLES.get(), SaturnModule.THOLIN_ORE.get()},
                new Block[]{SaturnModule.TITAN_ICE.stairs().get(), SaturnModule.TITAN_ICE.slab().get(), SaturnModule.TITAN_ICE.wall().get()});
        mob(level, SaturnEntities.METHANE_SKATER.get(), SX + 1, y0 + 1, SZ + 6, 200F);
        shot("04_titan_blocks", SX + 0.5, y0 + 3.4, SZ + 12.5, 180F, 18F, 120, at(0.2), null);
        shot("05_methane_skater", SX + 1.5, y0 + 2.6, SZ + 10.5, 180F, 26F, 60, at(0.2), null);
        // two tholin worms on a patch of dune sand at the back of the stage, one burrowed
        for (int x = SX + 3; x <= SX + 8; x++) {
            for (int z = SZ - 4; z <= SZ + 1; z++) {
                level.setBlock(new BlockPos(x, y0, z), SaturnModule.THOLIN_SAND.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        mob(level, SaturnEntities.THOLIN_WORM.get(), SX + 5, y0 + 1, SZ - 2, 150F);
        var hidden = mob(level, SaturnEntities.THOLIN_WORM.get(), SX + 7, y0 + 1, SZ, 40F);
        if (hidden != null) {
            hidden.setBurrowed(true);
        }
        shot("05b_tholin_worm", SX + 6.2, y0 + 1.9, SZ + 2.2, 170F, 30F, 80, at(0.2), null);
        int hx = SX + 40;
        int hz = SZ - 40;
        int hy = (int) ground(level, hx + 8, hz + 12);
        HangarPiece hangar = new HangarPiece(61L, hx, hy, hz, Direction.NORTH);
        MarsScenario.placePiece(level, hangar);
        BlockPos front = hangar.world(8, 4, 0);
        BlockPos inside = hangar.world(8, 4, 12);
        shot("06_rotorcraft_hangar", front.getX() + 0.5, front.getY() + 2.6, front.getZ() + 0.5,
                MoonScenario.yaw(inside.getX() - front.getX(), inside.getZ() - front.getZ()), 12F, 160, at(0.22), null);
        // the outside: apron, floodlights, buttresses, the roof walkway and mast, the tanks behind
        BlockPos away = hangar.world(18, 12, -12);
        BlockPos mid = hangar.world(8, 6, 11);
        shot("06b_hangar_outside", away.getX() + 0.5, away.getY(), away.getZ() + 0.5,
                MoonScenario.yaw(mid.getX() - away.getX(), mid.getZ() - away.getZ()), 18F, 160, at(0.22), null);
        BlockPos back = hangar.world(-4, 11, 30);
        shot("06c_hangar_back", back.getX() + 0.5, back.getY(), back.getZ() + 0.5,
                MoonScenario.yaw(mid.getX() - back.getX(), mid.getZ() - back.getZ()), 22F, 160, at(0.22), null);
    }

    private void enceladus(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_saturn_over_enceladus", 0, 0, 0.62, 20);
        parent(level, t, "02_saturn_crescent", 0, 0, 0.12, 20);
        double pole = t.profile().poleDistance();
        LineaeFeature.Config stripes = SaturnData.tigerStripes();
        double[] stripe = null;
        for (int i = 0; i < 400 && stripe == null; i++) {
            double x = (i % 20) * 23 - 230;
            double z = pole * 0.9 + (i / 20) * 11 - 100;
            if (LineaeFeature.crack(t, stripes, x, z) < 0.3) {
                stripe = new double[]{x, z};
            }
        }
        if (stripe != null) {
            look(level, "03_tiger_stripe", stripe[0] + 26, stripe[1] - 20, 12, stripe[0], stripe[1], at(0.15), 200, MoonScenario::farView);
        }
        VentFeature.Config geysers = SaturnData.southGeysers();
        BlockPos vent = null;
        int gz0 = (int) (pole * 0.9 / geysers.cell());
        for (int[] g : grid(-3, 3)) {
            int[] v = VentFeature.vent(t, geysers, g[0], gz0 + g[1]);
            if (v != null) {
                vent = findBlock(level, v[0], v[1], 3, JupiterModule.ICE_GEYSER.get());
                if (vent != null) {
                    break;
                }
            }
        }
        if (vent != null) {
            BlockPos v = vent;
            double y = Math.max(ground(level, v.getX() + 14, v.getZ() + 9), v.getY() + 1) + 5;
            for (int i = 0; i < 3; i++) {
                mob(level, SaturnEntities.GEYSER_DRIFTER.get(), v.getX() + 3 + i * 2, ground(level, v.getX() + 3 + i * 2, v.getZ() + 2) + 3 + i, v.getZ() + 2,
                        i * 90F);
            }
            shot("04_south_polar_geysers", v.getX() + 14.5, y, v.getZ() + 9.5, MoonScenario.yaw(-14, -9), -18F, 140, erupting(v, 0.15, false),
                    MoonScenario::farView);
        }
        int y0 = stage(level, SaturnModule.ENCELADUS_SNOW.get().defaultBlockState(), SaturnModule.ENCELADUS_ICE.get().defaultBlockState(),
                new Block[]{SaturnModule.ENCELADUS_SNOW.get(), SaturnModule.ENCELADUS_ICE.get(), SaturnModule.SULCUS_ICE.get()}, new Block[0]);
        mob(level, SaturnEntities.GEYSER_DRIFTER.get(), SX, y0 + 2.5, SZ + 5, 180F);
        shot("05_geyser_drifter", SX + 0.5, y0 + 3.2, SZ + 9.5, 180F, 8F, 80, at(0.15), null);
        lander(level, com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece.Variant.ENCELADUS, "06_crashed_lander", 0.12);
    }
}
