package com.pizzaparker07.spacebreaker.testmod.outer;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.world.LakeFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.LineaeFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.VentFeature;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.saturn.SaturnData;
import com.pizzaparker07.spacebreaker.content.saturn.SaturnModule;
import com.pizzaparker07.spacebreaker.content.saturn.TitanAir;
import com.pizzaparker07.spacebreaker.content.saturn.TitanWeather;
import com.pizzaparker07.spacebreaker.content.saturn.dungeon.HangarPiece;
import com.pizzaparker07.spacebreaker.content.saturn.dungeon.SaturnStructures;
import com.pizzaparker07.spacebreaker.content.saturn.entity.MethaneSkater;
import com.pizzaparker07.spacebreaker.content.saturn.entity.SaturnEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.testmod.mars.WorldTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SaturnGameTests {
    @GameTest(template = WorldTests.EMPTY)
    public static void titanHasThickColdAirAndEnceladusNone(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        helper.assertTrue(WorldTests.stem(helper, "titan").generator() instanceof PlanetChunkGenerator, "Titan uses the planet engine");
        helper.assertTrue(WorldTests.stem(helper, "enceladus").generator() instanceof PlanetChunkGenerator, "Enceladus uses the planet engine");
        CelestialBody titan = Bodies.forDimension(access, SaturnModule.TITAN).orElseThrow(() -> new AssertionError("titan has no body"));
        CelestialBody enceladus = Bodies.forDimension(access, SaturnModule.ENCELADUS).orElseThrow(() -> new AssertionError("enceladus has no body"));
        helper.assertTrue(titan.atmosphere().pressureKpa() > 120 && !titan.atmosphere().breathable(), "1.5 bar of nitrogen, nothing to breathe");
        helper.assertTrue(enceladus.atmosphere().isVacuum(), "Enceladus is airless");
        helper.assertTrue(titan.parent().map(p -> p.getPath().equals("saturn")).orElse(false), "Titan orbits Saturn");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 300)
    public static void titanHasDunesAndPolarMethaneLakes(GameTestHelper helper) {
        PlanetTerrain t = WorldTests.generator(helper, "titan", 4L).terrain();
        helper.assertTrue(t.profile().landforms().dunes().isPresent(), "hydrocarbon dunes");
        LakeFeature.Config cfg = SaturnData.titanLakes();
        int polar = 0;
        int equator = 0;
        double pole = t.profile().poleDistance();
        int rows = (int) (pole / cfg.cell());
        for (int gx = -8; gx < 8; gx++) {
            for (int gz = -rows; gz <= rows; gz++) {
                LakeFeature.Lake l = LakeFeature.lake(t, cfg, gx, gz);
                if (l != null) {
                    if (Math.abs(t.latitude(l.z())) > 55) {
                        polar++;
                    } else {
                        equator++;
                    }
                }
            }
        }
        helper.assertTrue(polar > 10 && equator == 0, "methane lakes gather near the poles: " + polar + " polar, " + equator + " equatorial");
        helper.assertTrue(SaturnModule.TITAN_METHANE.get().defaultBlockState().getFluidState().is(FluidsModule.TAG_LCH4),
                "the lakes are liquid methane");
        WorldTests.dumpMap(t, "titan", 0, 0, 5000, 400);
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 100)
    public static void thickAirAndShowers(GameTestHelper helper) {
        helper.assertTrue(TitanAir.TERMINAL < 0.5 && TitanAir.DRAG < 1, "falls slow to a gentle terminal speed");
        helper.assertTrue(TitanAir.LIFT > 0, "wings gain lift from speed");
        int wet = 0;
        for (long t = 0; t < 200 * TitanWeather.WINDOW; t += TitanWeather.WINDOW / 2) {
            wet += TitanWeather.rain(t) > 0 ? 1 : 0;
        }
        helper.assertTrue(wet > 10 && wet < 250, "methane showers come and go: " + wet + "/400");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 100)
    public static void skaterWalksOnMethane(GameTestHelper helper) {
        MethaneSkater s = helper.spawn(SaturnEntities.METHANE_SKATER.get(), new BlockPos(1, 2, 1));
        helper.assertTrue(s.canStandOnFluid(FluidsModule.LIQUID_METHANE.source.get().defaultFluidState()), "stands on liquid methane");
        helper.assertFalse(s.canStandOnFluid(Fluids.WATER.defaultFluidState()), "but sinks in water");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void enceladusTigerStripesAndGeysersAtTheSouthPole(GameTestHelper helper) {
        PlanetTerrain t = WorldTests.generator(helper, "enceladus", 6L).terrain();
        LineaeFeature.Config stripes = SaturnData.tigerStripes();
        double pole = t.profile().poleDistance();
        int south = 0;
        int north = 0;
        for (int i = 0; i < 2000; i++) {
            double x = (i * 7919L) % 1600 - 800;
            double zs = pole * (0.8 + 0.2 * ((i * 104729L) % 1000) / 1000.0);
            if (LineaeFeature.crack(t, stripes, x, zs) < 0.5) {
                south++;
            }
            if (LineaeFeature.crack(t, stripes, x, -zs) < 0.5) {
                north++;
            }
        }
        helper.assertTrue(south > north * 2 && south > 25, "fractures crowd the south pole: " + south + " south vs " + north + " north");
        VentFeature.Config geysers = SaturnData.southGeysers();
        int vents = 0;
        for (int gx = -10; gx < 10; gx++) {
            for (int gz = -40; gz < 40; gz++) {
                int[] v = VentFeature.vent(t, geysers, gx, gz);
                if (v != null) {
                    helper.assertTrue(t.latitude(v[1]) < -60, "geysers erupt only from the south polar terrain");
                    vents++;
                }
            }
        }
        helper.assertTrue(vents > 10, "many geysers: " + vents);
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void hangarHasFlyerAndLoot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(SaturnStructures.HANGAR_KEY), "hangar structure");
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-3000, 0, -7000);
        HangarPiece piece = new HangarPiece(8L, base.getX(), 110, base.getZ(), Direction.WEST);
        BeltGameTests.place(level, piece);
        BoundingBox bb = piece.getBoundingBox();
        int chests = 0;
        int copper = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            var st = level.getBlockState(p);
            if (st.is(Blocks.CHEST) && level.getBlockEntity(p) instanceof ChestBlockEntity ch && ch.getLootTable() != null) {
                chests++;
            }
            if (st.is(Blocks.WAXED_CUT_COPPER)) {
                copper++;
            }
        }
        helper.assertTrue(chests == 2, "workshop and lab chests, found " + chests);
        helper.assertTrue(copper >= 9, "the parked rotorcraft, found " + copper);
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void saturnianTechnologiesExist(GameTestHelper helper) {
        var all = Tech.all(helper.getLevel().registryAccess());
        for (String id : new String[]{"aerial_mobility", "hydrocarbon_processing", "plume_sampling"}) {
            var node = all.get(Spacebreaker.id(id));
            helper.assertTrue(node != null, id + " is in the tree");
            helper.assertTrue(node.tier() >= 3 && node.tier() <= 5, id + " is a phase-3 tech");
        }
        helper.succeed();
    }

    private SaturnGameTests() {
    }
}
