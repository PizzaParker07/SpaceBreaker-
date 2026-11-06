package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.LakeFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.LineaeFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.VentFeature;
import com.pizzaparker07.spacebreaker.content.jupiter.JupiterData;
import com.pizzaparker07.spacebreaker.content.jupiter.JupiterModule;
import com.pizzaparker07.spacebreaker.content.jupiter.dungeon.ObservatoryPiece;
import com.pizzaparker07.spacebreaker.content.jupiter.dungeon.ProbePiece;
import com.pizzaparker07.spacebreaker.content.jupiter.entity.JupiterEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Jupiter's moons. Europa ("jovian"): Jupiter in its sky at several phases, the cracked and stained ice, chaos terrain, a
 * geyser erupting, the blocks with a glow serpent pool, and the buried probe with its brine cave. Io: Jupiter huge over
 * the sulfur plains, a plume erupting, a lava lake, mountains, crabs and the observatory. Ganymede and Callisto: Jupiter
 * smaller each time, grooved and dark terrain, cratered plains with frost knobs, the Valhalla rings.
 */
final class JovianScenario extends OuterShowcase {
    private final String world;

    JovianScenario(String world) {
        this.world = world;
    }

    @Override
    public ResourceKey<Level> dimension() {
        return switch (world) {
            case "io" -> JupiterModule.IO;
            case "ganymede" -> JupiterModule.GANYMEDE;
            case "callisto" -> JupiterModule.CALLISTO;
            default -> JupiterModule.EUROPA;
        };
    }

    @Override
    protected void build(ServerLevel level, PlanetTerrain t) {
        switch (world) {
            case "io" -> io(level, t);
            case "ganymede" -> ganymede(level, t);
            case "callisto" -> callisto(level, t);
            default -> europa(level, t);
        }
    }

    private void europa(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_jupiter_over_europa", 0, 0, 0.62, 14);
        parent(level, t, "02_jupiter_half", 0, 0, 0.05, 12);
        LineaeFeature.Config cfg = JupiterData.europaLineae();
        double[] crack = MoonScenario.find(t, (x, z) -> LineaeFeature.crack(t, cfg, x, z) < 0.3 && LineaeFeature.chaos(t, cfg, x, z) < 0.1, 2500);
        if (crack != null) {
            look(level, "03_lineae", crack[0] + 30, crack[1] + 24, 14, crack[0], crack[1], at(0.12), 200, MoonScenario::farView);
        }
        double[] chaos = MoonScenario.find(t, (x, z) -> LineaeFeature.chaos(t, cfg, x, z) > 0.99 && LineaeFeature.chaos(t, cfg, x + 30, z + 30) > 0.99, 6000);
        if (chaos != null) {
            look(level, "04_chaos_terrain", chaos[0] + 22, chaos[1] + 18, 10, chaos[0], chaos[1], at(0.1), 200, MoonScenario::farView);
        }
        BlockPos geyser = null;
        for (int[] g : grid(-4, 4)) {
            int[] v = VentFeature.vent(t, new VentFeature.Config(JupiterModule.ICE_GEYSER.get().defaultBlockState(), java.util.List.of(), 420, 0.3, 7,
                    -90, 90, java.util.Optional.empty()), g[0], g[1]);
            if (v != null) {
                geyser = findBlock(level, v[0], v[1], 3, JupiterModule.ICE_GEYSER.get());
                if (geyser != null) {
                    break;
                }
            }
        }
        if (geyser != null) {
            BlockPos g = geyser;
            double y = ground(level, g.getX() + 14, g.getZ() + 10) + 3;
            shot("05_ice_geyser", g.getX() + 14.5, y, g.getZ() + 10.5, MoonScenario.yaw(-14, -10), -12F, 160, erupting(g, 0.18, false), null);
        }
        int y0 = stage(level, JupiterModule.EUROPAN_FROST.get().defaultBlockState(), JupiterModule.EUROPAN_ICE.get().defaultBlockState(),
                new Block[]{JupiterModule.EUROPAN_FROST.get(), JupiterModule.EUROPAN_ICE.get(), JupiterModule.STAINED_ICE.get(),
                        JupiterModule.REFROZEN_BRINE.get(), JupiterModule.SALT_ORE.get(), JupiterModule.EUROPAN_ICE_BRICKS.base().get()},
                new Block[]{JupiterModule.EUROPAN_ICE_BRICKS.stairs().get(), JupiterModule.EUROPAN_ICE_BRICKS.slab().get(),
                        JupiterModule.EUROPAN_ICE_BRICKS.wall().get(), JupiterModule.ICE_GEYSER.get()});
        // a brine pool in the stage with two serpents
        for (int x = SX - 6; x <= SX - 2; x++) {
            for (int z = SZ + 5; z <= SZ + 8; z++) {
                level.setBlock(new BlockPos(x, y0, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(new BlockPos(x, y0 - 1, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(new BlockPos(x, y0 - 2, z), JupiterModule.REFROZEN_BRINE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        mob(level, JupiterEntities.GLOW_SERPENT.get(), SX - 5, y0 - 0.9, SZ + 6, 80F);
        mob(level, JupiterEntities.GLOW_SERPENT.get(), SX - 3, y0 - 0.6, SZ + 7, 250F);
        shot("06_europa_blocks", SX + 0.5, y0 + 3.6, SZ + 12.5, 180F, 18F, 120, at(0.12), null);
        shot("07_glow_serpents", SX - 3.5, y0 + 2.6, SZ + 9.5, 180F, 62F, 80, at(0.12), null);
        int px = SX + 50;
        int pz = SZ - 40;
        int py = (int) ground(level, px + 10, pz + 10) - 1;
        ProbePiece probe = new ProbePiece(41L, px, py, pz, Direction.NORTH);
        MarsScenario.placePiece(level, probe);
        shot("08_probe_lander", px + 24.5, py + 9, pz + 26.5, MoonScenario.yaw(-14, -16), 24F, 160, at(0.14), null);
        BlockPos eye = probe.world(15, 5, 10);
        BlockPos pool = probe.world(5, 2, 11);
        shot("09_brine_cave", eye.getX() + 0.5, eye.getY() + 0.6, eye.getZ() + 0.5, MoonScenario.yaw(pool.getX() - eye.getX(), pool.getZ() - eye.getZ()),
                32F, 120, dark(0.14), null);
    }

    private void io(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_jupiter_over_io", 0, 0, 0.6, 18);
        BlockPos vent = null;
        VentFeature.Config plumes = JupiterData.ioPlumes();
        for (int[] g : grid(-4, 4)) {
            int[] v = VentFeature.vent(t, plumes, g[0], g[1]);
            if (v != null) {
                vent = findBlock(level, v[0], v[1], 3, JupiterModule.SULFUR_VENT.get());
                if (vent != null) {
                    break;
                }
            }
        }
        if (vent != null) {
            BlockPos v = vent;
            double y = ground(level, v.getX() + 30, v.getZ() + 22) + 4;
            shot("02_sulfur_plume", v.getX() + 30.5, y, v.getZ() + 22.5, MoonScenario.yaw(-30, -22), -22F, 200, erupting(v, 0.2, false),
                    MoonScenario::farView);
            look(level, "03_plume_deposits", v.getX() + 12, v.getZ() - 14, 18, v.getX(), v.getZ(), at(0.22), 160, null);
        }
        LakeFeature.Config lakes = JupiterData.ioPaterae();
        for (int[] g : grid(-4, 4)) {
            LakeFeature.Lake l = LakeFeature.lake(t, lakes, g[0], g[1]);
            if (l != null && l.radius() > 16) {
                look(level, "04_lava_lake", l.x() + l.radius() * 1.5, l.z() + l.radius() * 0.8, 6, l.x(), l.z(), at(0.12), 200, MoonScenario::farView);
                break;
            }
        }
        double[] mountain = MoonScenario.find(t, (x, z) -> t.column(x, z).volcanoS < 0.4, 6000);
        if (mountain != null) {
            look(level, "05_ionian_mountain", mountain[0] + 110, mountain[1] + 80, 6, mountain[0], mountain[1], at(0.08), 220, MoonScenario::farView);
        }
        int y0 = stage(level, JupiterModule.SULFUR_SAND.get().defaultBlockState(), JupiterModule.IONIAN_BASALT.base().get().defaultBlockState(),
                new Block[]{JupiterModule.SULFUR_SAND.get(), JupiterModule.SO2_FROST.get(), JupiterModule.RED_SULFUR.get(),
                        JupiterModule.IONIAN_BASALT.base().get(), JupiterModule.IONIAN_SULFUR_ORE.get(), JupiterModule.SULFUR_VENT.get()},
                new Block[]{JupiterModule.IONIAN_BASALT.stairs().get(), JupiterModule.IONIAN_BASALT.slab().get(), JupiterModule.IONIAN_BASALT.wall().get()});
        mob(level, JupiterEntities.SULFUR_CRAB.get(), SX - 2, y0 + 1, SZ + 6, 200F);
        mob(level, JupiterEntities.SULFUR_CRAB.get(), SX + 3, y0 + 1, SZ + 5, 150F);
        shot("06_io_blocks_and_crabs", SX + 0.5, y0 + 3.4, SZ + 12.5, 180F, 18F, 120, at(0.12), null);
        shot("07_sulfur_crab", SX - 1.5, y0 + 2.2, SZ + 9.0, 175F, 26F, 60, at(0.12), null);
        int ox = SX + 40;
        int oz = SZ - 36;
        int oy = (int) ground(level, ox + 8, oz + 8);
        MarsScenario.placePiece(level, new ObservatoryPiece(51L, ox, oy, oz, Direction.NORTH));
        shot("08_volcano_observatory", ox + 26.5, oy + 9, oz + 26.5, MoonScenario.yaw(-18, -18), 22F, 160, at(0.1), null);
    }

    private void ganymede(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_jupiter_over_ganymede", 0, 0, 0.3, 10);
        double[] grooved = MoonScenario.find(t, (x, z) -> t.lowland(x, z, t.latitude(z)) > 0.9, 3000);
        if (grooved != null) {
            look(level, "02_grooved_terrain", grooved[0], grooved[1], 18, grooved[0] + 60, grooved[1] + 40, at(0.08), 200, MoonScenario::farView);
        }
        double[] dark = MoonScenario.find(t, (x, z) -> t.lowland(x, z, t.latitude(z)) < 0.05 && t.column(x, z).craterS < 0.7, 3000);
        if (dark != null) {
            look(level, "03_dark_terrain_crater", dark[0] + 40, dark[1] + 30, 10, dark[0], dark[1], at(0.1), 200, MoonScenario::farView);
        }
        int y0 = stage(level, JupiterModule.GANYMEDE_REGOLITH.get().defaultBlockState(), JupiterModule.DARK_ICE_CRUST.get().defaultBlockState(),
                new Block[]{JupiterModule.GANYMEDE_REGOLITH.get(), JupiterModule.GROOVED_ICE.get(), JupiterModule.DARK_ICE_CRUST.get(),
                        JupiterModule.BRIGHT_FROST.get()}, new Block[0]);
        mob(level, JupiterEntities.GROOVE_GRAZER.get(), SX - 2, y0 + 1, SZ + 5, 200F);
        mob(level, JupiterEntities.GROOVE_GRAZER.get(), SX + 2, y0 + 1, SZ + 6, 150F);
        shot("04_ganymede_blocks", SX + 0.5, y0 + 3.4, SZ + 11.5, 180F, 18F, 100, at(0.12), null);
        shot("05_groove_grazer", SX - 1.0, y0 + 2.2, SZ + 8.8, 185F, 24F, 60, at(0.12), null);
        lander(level, com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece.Variant.GANYMEDE, "06_crashed_lander", 0.12);
    }

    private void callisto(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_jupiter_over_callisto", 0, 0, 0.3, 6);
        look(level, "02_callisto_craters", 0, 0, 16, 90, 70, at(0.06), 220, MoonScenario::farView);
        BasinFeature.Config v = JupiterData.valhalla();
        double cz = BasinFeature.centreZ(t, v);
        look(level, "03_valhalla_rings", v.x() + v.radius() * 0.35, cz + v.radius() * 0.1, 40, v.x() + v.radius() * 0.5, cz + v.radius() * 0.25,
                at(0.07), 240, MoonScenario::farView);
        int y0 = stage(level, JupiterModule.CALLISTO_REGOLITH.get().defaultBlockState(), JupiterModule.CALLISTAN_ROCK.base().get().defaultBlockState(),
                new Block[]{JupiterModule.CALLISTO_REGOLITH.get(), JupiterModule.BRIGHT_FROST.get(), JupiterModule.CALLISTAN_ROCK.base().get()},
                new Block[]{JupiterModule.CALLISTAN_ROCK.stairs().get(), JupiterModule.CALLISTAN_ROCK.slab().get(), JupiterModule.CALLISTAN_ROCK.wall().get()});
        mob(level, JupiterEntities.CRATER_HOPPER.get(), SX - 1, y0 + 1, SZ + 5, 200F);
        mob(level, JupiterEntities.CRATER_HOPPER.get(), SX + 3, y0 + 1, SZ + 6, 150F);
        shot("04_callisto_blocks", SX + 0.5, y0 + 3.4, SZ + 11.5, 180F, 18F, 100, at(0.12), null);
        shot("05_crater_hopper", SX - 0.5, y0 + 2.0, SZ + 8.5, 185F, 18F, 60, at(0.12), null);
        lander(level, com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece.Variant.CALLISTO, "06_crashed_lander", 0.12);
    }
}
