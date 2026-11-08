package com.pizzaparker07.spacebreaker.testmod.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mercury.IceMinePiece;
import com.pizzaparker07.spacebreaker.content.mercury.MercuryEntities;
import com.pizzaparker07.spacebreaker.content.mercury.MercuryModule;
import com.pizzaparker07.spacebreaker.content.mercury.MercuryStructures;
import com.pizzaparker07.spacebreaker.content.mercury.TerminatorCrawler;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.worldgen.Crater;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.SkyMath;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MercuryGameTests {
    @GameTest(template = WorldTests.EMPTY)
    public static void mercuryIsScorchedAndFrozen(GameTestHelper helper) {
        var stem = WorldTests.stem(helper, "mercury");
        helper.assertTrue(stem.generator() instanceof PlanetChunkGenerator, "mercury uses the planet engine");
        CelestialBody b = Bodies.forDimension(helper.getLevel().registryAccess(), MercuryModule.MERCURY)
                .orElseThrow(() -> new AssertionError("mercury has no body"));
        helper.assertTrue(b.atmosphere().isVacuum(), "airless");
        helper.assertTrue(b.climate().dayTempC() > 400 && b.climate().nightTempC() < -150, "extreme day and night");
        double sun = SkyMath.angularDiameter(695700, b.orbit().semiMajorAxisKm());
        helper.assertTrue(sun > 1.2, "the sun looks almost three times wider than from Earth: " + sun);
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 300)
    public static void mercuryHasScarpsAndPolarIce(GameTestHelper helper) {
        TerrainProfile p = WorldTests.profile(helper, "mercury");
        helper.assertTrue(p.landforms().scarps().isPresent(), "lobate scarps");
        PlanetTerrain t = new PlanetTerrain(p, 8L);
        PlanetTerrain flat = new PlanetTerrain(new TerrainProfile(p.body(), p.baseHeight(), p.poleDistance(), p.relief(), p.lowlands(),
                p.craters(), p.poles(), p.materials()), 8L);
        double maxStep = 0;
        for (int i = 0; i < 3000; i++) {
            double x = (i * 7919L) % 12000 - 6000;
            double z = (i * 104729L) % 8000 - 4000;
            maxStep = Math.max(maxStep, t.height(x, z) - flat.height(x, z));
        }
        helper.assertTrue(maxStep > 8, "scarps raise the crust by several blocks, max " + maxStep);
        boolean shadowed = false;
        for (Crater c : t.cratersNear(-1500, p.poleDistance() * 0.9, 1500, p.poleDistance())) {
            if (c.radius() >= 8 && Math.abs(t.latitude(c.z())) > 84) {
                shadowed |= t.column(c.x(), c.z()).shadowed(p);
            }
        }
        helper.assertTrue(shadowed, "ice hides in permanently shadowed polar craters");
        WorldTests.dumpMap(t, "mercury", 0, 0, 6000, 500);
        PlanetChunkGenerator gen = WorldTests.generator(helper, "mercury", 5L);
        var acc = net.minecraft.world.level.LevelHeightAccessor.create(-64, 384);
        helper.assertTrue(gen.getBaseColumn(10, 10, acc, null).getBlock(gen.surfaceY(10, 10, acc)).getBlock() instanceof LunarRegolithBlock,
                "airless regolith keeps prints");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 100)
    public static void terminatorCrawlerHidesFromHeatAndCold(GameTestHelper helper) {
        TerminatorCrawler c = helper.spawn(MercuryEntities.TERMINATOR_CRAWLER.get(), new BlockPos(1, 2, 1));
        c.setBurrowed(true);
        helper.assertTrue(c.isBurrowed(), "burrows");
        c.hurt(helper.getLevel().damageSources().generic(), 1F);
        helper.assertFalse(c.isBurrowed(), "a hit drives it out of its burrow");
        helper.assertTrue(TerminatorCrawler.TWILIGHT_DEG > 5 && TerminatorCrawler.TWILIGHT_DEG < 45, "active band around the terminator");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void iceMineHasIceLootAndDrones(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(MercuryStructures.ICE_MINE_KEY), "ice mine structure");
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-5000, 0, -5000);
        IceMinePiece piece = new IceMinePiece(4L, base.getX(), 100, base.getZ());
        BoundingBox bb = piece.getBoundingBox();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
            for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                level.getChunk(cx, cz);
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(cx),
                        new BoundingBox(cx << 4, -64, cz << 4, (cx << 4) + 15, 319, (cz << 4) + 15), new ChunkPos(cx, cz), bb.getCenter());
            }
        }
        int chests = 0;
        int ice = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            var st = level.getBlockState(p);
            if (level.getBlockEntity(p) instanceof com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlockEntity ch
                    && ch.getLootTable() != null) {
                chests++;
            }
            if (st.is(com.pizzaparker07.spacebreaker.content.mercury.MercuryModule.MERCURY_ICE.get())) {
                ice++;
            }
        }
        helper.assertTrue(chests >= 2, "office and stores lockers, found " + chests);
        helper.assertTrue(ice > 300, "the mine is cut into ice, found " + ice);
        int frost = 0;
        int signs = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            var be = level.getBlockEntity(p);
            if (be instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity d && d.getItem(0).is(net.minecraft.world.item.Items.POWDER_SNOW_BUCKET)) {
                frost++;
            }
            if (be instanceof net.minecraft.world.level.block.entity.SignBlockEntity) {
                signs++;
            }
            var st = level.getBlockState(p);
            if (st.is(Blocks.LADDER) || st.is(Blocks.BIRCH_WALL_SIGN)) {
                helper.assertTrue(st.canSurvive(level, p), "a ladder or sign hangs in the air at " + p);
            }
        }
        helper.assertTrue(frost == 1, "the powder-snow trap, found " + frost);
        helper.assertTrue(signs == 2, "the sign at the ice-sealed store and the last-shift log, found " + signs);
        helper.succeed();
    }

    /** The hollow snail is Mercury's harmless, useful creature: on a hollow deposit it leaves sulfur behind. */
    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 120)
    public static void hollowSnailGrazesSulfur(GameTestHelper helper) {
        BlockPos floor = new BlockPos(1, 1, 1);
        helper.setBlock(floor, com.pizzaparker07.spacebreaker.content.mercury.MercuryModule.HOLLOW_DEPOSIT.get());
        var snail = helper.spawn(com.pizzaparker07.spacebreaker.content.mercury.MercuryEntities.HOLLOW_SNAIL.get(), floor.above());
        snail.setNoAi(true);
        helper.assertFalse(snail instanceof net.minecraft.world.entity.monster.Enemy, "the snail is no monster");
        snail.grazeSoon();
        helper.succeedWhen(() -> helper.assertItemEntityPresent(com.pizzaparker07.spacebreaker.content.venus.VenusModule.SULFUR.get(), floor.above(), 2.0));
    }

    /** On Mercury anything between you and the sun makes shade, and shade is survivable even at noon. */
    @GameTest(template = WorldTests.EMPTY)
    public static void shadeMakesASafeCorridor(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        org.joml.Vector3d sun = new org.joml.Vector3d(0.6, 0.5, 0.0).normalize();
        helper.assertFalse(com.pizzaparker07.spacebreaker.content.mercury.MercuryClimate.shadowed(level, at, sun), "open sky: sunlit");
        // a wall toward the sun, like a scarp face
        BlockPos wall = BlockPos.containing(at.getX() + 0.5 + sun.x * 3, at.getY() + 0.5 + sun.y * 3, at.getZ() + 0.5 + sun.z * 3);
        level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.mercury.MercuryClimate.shadowed(level, at, sun), "behind the wall: shadow");
        level.setBlock(wall, Blocks.GLASS.defaultBlockState(), 3);
        helper.assertFalse(com.pizzaparker07.spacebreaker.content.mercury.MercuryClimate.shadowed(level, at, sun), "glass casts no shadow");
        level.setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.mercury.MercuryClimate.shaded(430) == com.pizzaparker07.spacebreaker.content.mercury.MercuryClimate.SHADE_C,
                "noon shade is survivable");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.mercury.MercuryClimate.shaded(-150) == -150, "shade never warms the night");
        helper.succeed();
    }

    /**
     * The 3:2 resonance at this world's (warm-pole) longitude: near perihelion the sun peeks over the horizon, sinks
     * back below it and rises a second time; the sun moves backwards there, forwards the rest of the day, and the day
     * keeps its length. Measured through the warped clock, the same one the sky, light and temperature read.
     */
    @GameTest(template = WorldTests.EMPTY)
    public static void mercurySunRisesTwice(GameTestHelper helper) {
        long day = 24000;
        var clock = new com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime.Clock(day, 0,
                com.pizzaparker07.spacebreaker.content.mercury.MercuryDay::trueFraction);
        java.util.function.DoubleUnaryOperator elevation = mean -> com.pizzaparker07.spacebreaker.content.worldgen.SkyMath.sunElevation(
                clock.timeOfDay(Math.round(mean * day)), 0);
        double p = com.pizzaparker07.spacebreaker.content.mercury.MercuryDay.PERIHELION;
        double before = elevation.applyAsDouble(p - 0.045);
        double peek = elevation.applyAsDouble(p - 0.023);
        double dip = elevation.applyAsDouble(p + 0.023);
        double again = elevation.applyAsDouble(p + 0.06);
        helper.assertTrue(before < 0, "night before the first sunrise, sun at " + before);
        helper.assertTrue(peek > 0.3, "first sunrise: the sun peeks over the horizon, at " + peek);
        helper.assertTrue(dip < -0.3, "then it sinks back below it, at " + dip);
        helper.assertTrue(again > 0.5, "and rises a second time, at " + again);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.mercury.MercuryDay.trueFraction(p + 0.01)
                < com.pizzaparker07.spacebreaker.content.mercury.MercuryDay.trueFraction(p), "at perihelion the sun moves backwards");
        for (double f = p + 0.06; f < p + 0.44; f += 0.01) {
            helper.assertTrue(com.pizzaparker07.spacebreaker.content.mercury.MercuryDay.trueFraction(f + 0.01)
                    > com.pizzaparker07.spacebreaker.content.mercury.MercuryDay.trueFraction(f), "forwards away from perihelion, at " + f);
        }
        helper.assertTrue(Math.abs(com.pizzaparker07.spacebreaker.content.mercury.MercuryDay.trueFraction(p + 0.5) - (p + 0.5)) < 1e-9
                && Math.abs(com.pizzaparker07.spacebreaker.content.mercury.MercuryDay.trueFraction(p + 1) - (p + 1)) < 1e-9, "the day keeps its length: " + com.pizzaparker07.spacebreaker.content.mercury.MercuryDay.trueFraction(p + 0.5) + " vs " + (p + 0.5) + ", " + com.pizzaparker07.spacebreaker.content.mercury.MercuryDay.trueFraction(p + 1) + " vs " + (p + 1));
        double peri = com.pizzaparker07.spacebreaker.content.mercury.MercuryDay.distance(p);
        double aph = com.pizzaparker07.spacebreaker.content.mercury.MercuryDay.distance(p + 0.25);
        helper.assertTrue(Math.abs(aph / peri - 1.518) < 0.01, "the sun looks 1.5 times wider at perihelion, ratio " + aph / peri);
        var plain = new com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime.Clock(day, 0);
        helper.assertTrue(plain.fraction(1234) == plain.meanFraction(1234), "other worlds keep an even day");
        helper.succeed();
    }

    private MercuryGameTests() {
    }
}
