package com.pizzaparker07.spacebreaker.testmod.outer;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.BeltData;
import com.pizzaparker07.spacebreaker.content.belt.BeltModule;
import com.pizzaparker07.spacebreaker.content.belt.SmallBodyTerrain;
import com.pizzaparker07.spacebreaker.content.belt.dungeon.BeltStructures;
import com.pizzaparker07.spacebreaker.content.belt.dungeon.MiningOutpostPiece;
import com.pizzaparker07.spacebreaker.content.belt.entity.BeltEntities;
import com.pizzaparker07.spacebreaker.content.belt.entity.RockMite;
import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.Columns;
import com.pizzaparker07.spacebreaker.content.belt.world.CraterFloorFeature;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.worldgen.Crater;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BeltGameTests {
    @GameTest(template = WorldTests.EMPTY)
    public static void ceresAndVestaAreSmallAirlessWorlds(GameTestHelper helper) {
        for (String w : new String[]{"ceres", "vesta"}) {
            helper.assertTrue(WorldTests.stem(helper, w).generator() instanceof PlanetChunkGenerator, w + " uses the planet engine");
            CelestialBody b = Bodies.forDimension(helper.getLevel().registryAccess(),
                    w.equals("ceres") ? BeltModule.CERES : BeltModule.VESTA).orElseThrow(() -> new AssertionError(w + " has no body"));
            helper.assertTrue(b.atmosphere().isVacuum(), w + " is airless");
            helper.assertTrue(b.physical().surfaceGravity() < 0.5, w + " has a tiny gravity: " + b.physical().surfaceGravity());
        }
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void smallBodyTerrainScalesWithRadius(GameTestHelper helper) {
        helper.assertTrue(Math.abs(SmallBodyTerrain.poleDistance(11) - 900) < 1, "a Phobos-sized body matches Phobos' world");
        double ceres = SmallBodyTerrain.poleDistance(469.7);
        double vesta = SmallBodyTerrain.poleDistance(262.7);
        helper.assertTrue(ceres > vesta && vesta > 900 && ceres < 6000, "worlds grow with the radius: " + vesta + " " + ceres);
        TerrainProfile p = WorldTests.profile(helper, "ceres");
        helper.assertTrue(p.poleDistance() == ceres, "the Ceres profile is built by the small-body builder");
        helper.assertTrue(p.landforms().volcanoes().isPresent(), "Ceres has cryovolcanic domes");
        // a procedural asteroid built from random numbers is a valid world too
        TerrainProfile rubble = SmallBodyTerrain.of(Spacebreaker.id("test_rubble"), 3.2).roughness(1.6).age(0.3)
                .materials(new SmallBodyTerrain.Materials(BeltModule.CERES_REGOLITH.get().defaultBlockState(),
                        BeltModule.CEREAN_ROCK.base().get().defaultBlockState())).build();
        PlanetTerrain t = new PlanetTerrain(rubble, 3L);
        double lo = 999;
        double hi = -999;
        for (int i = 0; i < 400; i++) {
            double h = t.height((i * 37) % 600 - 300, (i * 91) % 600 - 300);
            lo = Math.min(lo, h);
            hi = Math.max(hi, h);
        }
        helper.assertTrue(hi - lo > 6 && hi < 200 && lo > -40, "rubble world has relief within the build height: " + lo + ".." + hi);
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 300)
    public static void faculaeBrightenSomeCraterFloors(GameTestHelper helper) {
        PlanetChunkGenerator gen = WorldTests.generator(helper, "ceres", 11L);
        PlanetTerrain t = gen.terrain();
        CraterFloorFeature.Config cfg = BeltData.faculae();
        Crater host = null;
        int craters = 0;
        for (Crater c : t.cratersNear(-3000, -1500, 3000, 1500)) {
            if (c.radius() >= cfg.minRadius()) {
                craters++;
                if (host == null && CraterFloorFeature.holds(c, cfg)) {
                    host = c;
                }
            }
        }
        helper.assertTrue(host != null, "some of the " + craters + " large craters hold salt deposits");
        double[] dome = new double[1];
        double centre = CraterFloorFeature.strength(host, cfg, Columns.noise(t.seed(), 41), host.x(), host.z(), dome);
        helper.assertTrue(centre > 1.8, "the crater centre is bright salt");
        helper.assertTrue(CraterFloorFeature.strength(host, cfg, Columns.noise(t.seed(), 41), host.x() + host.radius() * 1.2, host.z(), dome) == 0,
                "nothing outside the rim");
        // run the real feature on a chunk of the test level under the crater centre
        ServerLevel level = helper.getLevel();
        int cx = (int) Math.floor(host.x()) >> 4;
        int cz = (int) Math.floor(host.z()) >> 4;
        level.getChunk(cx, cz);
        BlockPos origin = new BlockPos(cx << 4, 0, cz << 4);
        new CraterFloorFeature().place(new FeaturePlaceContext<>(Optional.empty(), level, gen, RandomSource.create(1), origin, cfg));
        int salt = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockPos top = new BlockPos(origin.getX() + x, Columns.top(level, origin.getX() + x, origin.getZ() + z), origin.getZ() + z);
                if (level.getBlockState(top).is(BeltModule.CARBONATE_CRUST.get())) {
                    salt++;
                }
            }
        }
        helper.assertTrue(salt > 0, "the feature lays carbonate crust on the floor, found " + salt);
        WorldTests.dumpMap(t, "ceres", 0, 0, 5000, 500);
        var acc = LevelHeightAccessor.create(-64, 384);
        helper.assertTrue(gen.getBaseColumn(10, 10, acc, null).getBlock(gen.surfaceY(10, 10, acc)).getBlock() instanceof LunarRegolithBlock,
                "Ceres regolith keeps boot prints");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void rheasilviaShapesVestasSouthPole(GameTestHelper helper) {
        PlanetTerrain t = WorldTests.generator(helper, "vesta", 5L).terrain();
        BasinFeature.Config cfg = BeltData.rheasilvia();
        double cz = BasinFeature.centreZ(t, cfg);
        helper.assertTrue(Math.abs(t.latitude(cz) + 90) < 0.5, "the basin is centred on the south pole");
        double peak = BasinFeature.offset(t, cfg, cfg.x(), cz);
        double floor = BasinFeature.offset(t, cfg, cfg.x() + cfg.radius() * 0.5, cz);
        double rimMax = 0;
        for (double s = 0.9; s < 1.15; s += 0.01) {
            rimMax = Math.max(rimMax, BasinFeature.offset(t, cfg, cfg.x() + cfg.radius() * s, cz));
        }
        double outside = BasinFeature.offset(t, cfg, cfg.x() + cfg.radius() * 2, cz);
        helper.assertTrue(peak - floor > 50 && peak > 10, "a huge central mountain rising from the floor: " + peak + " over " + floor);
        helper.assertTrue(floor < -20, "a deep floor: " + floor);
        helper.assertTrue(rimMax > 8, "a raised rim: " + rimMax);
        helper.assertTrue(outside == 0, "untouched far away");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 100)
    public static void rockMitesHideAndWakeTogether(GameTestHelper helper) {
        RockMite a = helper.spawn(BeltEntities.ROCK_MITE.get(), new BlockPos(1, 2, 1));
        RockMite b = helper.spawn(BeltEntities.ROCK_MITE.get(), new BlockPos(3, 2, 1));
        a.setHidden(true);
        b.setHidden(true);
        helper.assertTrue(a.isHidden() && b.isHidden(), "mites sit hidden in the gravel");
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        a.hurt(helper.getLevel().damageSources().playerAttack(player), 1F);
        helper.assertFalse(a.isHidden(), "a hit drives it out");
        helper.assertFalse(b.isHidden(), "and wakes the rest of the cluster");
        helper.assertTrue(b.getTarget() == player, "which turns on the attacker");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void miningOutpostHasLootIceAndNest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(BeltStructures.OUTPOST_KEY), "outpost structure");
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-6000, 0, 2000);
        MiningOutpostPiece piece = new MiningOutpostPiece(4L, base.getX(), 100, base.getZ(), Direction.EAST);
        place(level, piece);
        BoundingBox bb = piece.getBoundingBox();
        int chests = 0;
        int ice = 0;
        int spawners = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            var st = level.getBlockState(p);
            if (st.is(Blocks.CHEST) && level.getBlockEntity(p) instanceof ChestBlockEntity ch && ch.getLootTable() != null) {
                chests++;
            }
            if (st.is(BeltModule.CEREAN_ICE.get())) {
                ice++;
            }
            if (st.is(Blocks.SPAWNER)) {
                spawners++;
            }
        }
        helper.assertTrue(chests >= 3, "office, stores and core store chests, found " + chests);
        helper.assertTrue(ice > 200, "the drift is cut into buried ice, found " + ice);
        helper.assertTrue(spawners == 1, "one rock mite nest");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void beltTechnologiesExist(GameTestHelper helper) {
        var all = Tech.all(helper.getLevel().registryAccess());
        for (String id : new String[]{"microgravity_mining", "brine_chemistry"}) {
            var node = all.get(Spacebreaker.id(id));
            helper.assertTrue(node != null, id + " is in the tree");
            helper.assertTrue(node.tier() >= 3 && node.tier() <= 5, id + " is a phase-3 tech");
        }
        helper.succeed();
    }

    static void place(ServerLevel level, StructurePiece piece) {
        BoundingBox bb = piece.getBoundingBox();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
            for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                level.getChunk(cx, cz);
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(cx),
                        new BoundingBox(cx << 4, -64, cz << 4, (cx << 4) + 15, 319, (cz << 4) + 15), new ChunkPos(cx, cz), bb.getCenter());
            }
        }
    }

    private BeltGameTests() {
    }
}
