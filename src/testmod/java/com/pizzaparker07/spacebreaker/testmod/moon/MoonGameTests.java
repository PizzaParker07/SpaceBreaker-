package com.pizzaparker07.spacebreaker.testmod.moon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.Footprint;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.worldgen.Crater;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetBiomeSource;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import com.pizzaparker07.spacebreaker.content.worldgen.SkyMath;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.LevelStem;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.joml.Vector3d;

import java.util.List;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MoonGameTests {
    private static final String EMPTY = "empty";

    /**
     * The game test server never loads data-pack dimensions, so the Moon's level stem is decoded from its JSON,
     * exactly as a real world does it.
     */
    private static LevelStem moonStem(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        try (var reader = server.getResourceManager().openAsReader(Spacebreaker.id("dimension/moon.json"))) {
            JsonElement json = JsonParser.parseReader(reader);
            return LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()), json)
                    .getOrThrow(msg -> new AssertionError("moon dimension JSON is invalid: " + msg));
        } catch (java.io.IOException e) {
            throw new AssertionError("moon dimension JSON missing", e);
        }
    }

    private static PlanetChunkGenerator moonGenerator(GameTestHelper helper, long seed) {
        if (!(moonStem(helper).generator() instanceof PlanetChunkGenerator gen)) {
            throw new AssertionError("moon must use the planet generator");
        }
        gen.bindSeed(seed);
        return gen;
    }

    private static TerrainProfile moonProfile(GameTestHelper helper) {
        return helper.getLevel().registryAccess().registryOrThrow(TerrainProfile.REGISTRY)
                .getOptional(Spacebreaker.id("moon")).orElseThrow(() -> new AssertionError("moon terrain profile missing"));
    }

    // ------------------------------------------------------------------ dimension and body

    @GameTest(template = EMPTY)
    public static void moonDimensionUsesPlanetEngine(GameTestHelper helper) {
        LevelStem stem = moonStem(helper);
        helper.assertTrue(stem.generator() instanceof PlanetChunkGenerator, "moon must use the planet generator");
        helper.assertTrue(stem.generator().getBiomeSource() instanceof PlanetBiomeSource, "moon must use the planet biome source");
        helper.assertTrue(stem.type().value().effectsLocation().equals(Spacebreaker.id("moon")), "moon sky effects");
        helper.assertTrue(stem.type().value().hasSkyLight() && !stem.type().value().hasCeiling(), "open sky");
        helper.assertTrue(stem.generator().getBiomeSource().possibleBiomes().size() == 5, "five lunar biomes");
        var access = helper.getLevel().registryAccess();
        CelestialBody body = Bodies.forDimension(access, MoonModule.MOON).orElseThrow(() -> new AssertionError("moon dimension has no body"));
        helper.assertTrue(Math.abs(body.physical().surfaceGravity() - 1.62) < 0.01, "moon dimension must map to the Moon body");
        helper.assertTrue(body.atmosphere().isVacuum(), "the Moon is airless");
        helper.succeed();
    }

    // ------------------------------------------------------------------ generator

    @GameTest(template = EMPTY)
    public static void terrainIsDeterministic(GameTestHelper helper) {
        TerrainProfile profile = moonProfile(helper);
        PlanetTerrain a = new PlanetTerrain(profile, 12345L);
        PlanetTerrain b = new PlanetTerrain(profile, 12345L);
        PlanetTerrain c = new PlanetTerrain(profile, 999L);
        int differ = 0;
        for (int i = 0; i < 400; i++) {
            double x = (i * 7919) % 4000 - 2000;
            double z = (i * 104729) % 4000 - 2000;
            helper.assertTrue(a.height(x, z) == b.height(x, z), "same seed must give the same height at " + x + "," + z);
            helper.assertTrue(a.column(x, z).zone(profile) == b.column(x, z).zone(profile), "same seed must give the same zone");
            if (Math.abs(a.height(x, z) - c.height(x, z)) > 0.5) {
                differ++;
            }
        }
        helper.assertTrue(differ > 200, "a different seed must give different terrain (" + differ + "/400 differ)");
        helper.assertTrue(a.cratersNear(0, 0, 500, 500).equals(b.cratersNear(0, 0, 500, 500)), "crater fields must match");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void terrainStaysInsideTheWorld(GameTestHelper helper) {
        TerrainProfile profile = moonProfile(helper);
        PlanetTerrain t = new PlanetTerrain(profile, 77L);
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        int lowland = 0;
        for (int i = 0; i < 3000; i++) {
            double x = (i * 7919L) % 20000 - 10000;
            double z = (i * 104729L) % 10000 - 5000;
            PlanetTerrain.Column c = t.column(x, z);
            min = Math.min(min, c.height);
            max = Math.max(max, c.height);
            if (c.lowland > 0.5) {
                lowland++;
            }
        }
        helper.assertTrue(min > -40 && max < 250, "terrain height out of range: " + min + " .. " + max);
        helper.assertTrue(max - min > 30, "terrain too flat: " + min + " .. " + max);
        helper.assertTrue(lowland > 150 && lowland < 2400, "maria should cover a fair share of the near side: " + lowland + "/3000");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void craterShapesAreRealistic(GameTestHelper helper) {
        PlanetTerrain t = new PlanetTerrain(moonProfile(helper), 1L);
        Crater simple = t.shape(0, 0, 10, false, 42L);
        helper.assertTrue(!simple.complex(), "small craters are simple bowls");
        helper.assertTrue(simple.profile(0) < -0.15 * 20, "bowl depth ~0.2 D, got " + simple.profile(0));
        helper.assertTrue(simple.profile(1.0) > 0.5, "raised rim, got " + simple.profile(1.0));
        helper.assertTrue(simple.profile(1.0) > simple.profile(0.8) && simple.profile(0.8) > simple.profile(0.4), "walls rise to the rim");
        helper.assertTrue(simple.profile(1.5) < simple.profile(1.0) && simple.profile(1.5) > 0, "ejecta blanket thins outside the rim");
        helper.assertTrue(simple.profile(Crater.EJECTA_END) == 0, "no effect beyond the ejecta");

        Crater complex = t.shape(0, 0, 100, false, 43L);
        helper.assertTrue(complex.complex(), "large craters are complex");
        double depthRatio = -complex.profile(0.4) / 200;
        helper.assertTrue(depthRatio < 0.2 * 0.8, "complex craters are relatively shallower, d/D = " + depthRatio);
        helper.assertTrue(Math.abs(complex.profile(0.35) - complex.profile(0.25)) < 0.01, "complex craters have a flat floor");
        helper.assertTrue(complex.profile(0) > complex.profile(0.3) + 3, "complex craters have a central peak");
        helper.assertTrue(complex.profile(0) < complex.profile(1.0), "central peak stays below the rim");

        // sampled terrain around a real crater of the field: centre below rim
        Crater big = t.cratersNear(-3000, -3000, 3000, 3000).stream().filter(c -> c.radius() > 30).findFirst()
                .orElseThrow(() -> new AssertionError("no large crater in a 6 km square"));
        double centre = t.height(big.x() + big.radius() * 0.5, big.z());
        double rim = t.height(big.x() + big.radius(), big.z());
        helper.assertTrue(rim > centre + big.radius() * 0.1, "crater floor must lie well below the rim: " + centre + " vs " + rim);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void latitudeAndPolarIce(GameTestHelper helper) {
        TerrainProfile profile = moonProfile(helper);
        double p = profile.poleDistance();
        helper.assertTrue(PlanetTerrain.latitude(0, p) == 0, "z = 0 is the equator");
        helper.assertTrue(Math.abs(PlanetTerrain.latitude(-p, p) - 90) < 1e-9, "north pole at z = -poleDistance");
        helper.assertTrue(Math.abs(PlanetTerrain.latitude(p, p) + 90) < 1e-9, "south pole at z = +poleDistance");
        helper.assertTrue(Math.abs(PlanetTerrain.latitude(-1.5 * p, p) - 45) < 1e-9, "past the pole latitude falls again");
        PlanetTerrain t = new PlanetTerrain(profile, 5L);
        // somewhere near the pole a crater floor must be permanently shadowed
        boolean found = false;
        for (Crater c : t.cratersNear(-1500, p * 0.9, 1500, p)) {
            if (c.radius() >= 8 && Math.abs(t.latitude(c.z())) > 85) {
                found |= t.column(c.x(), c.z()).shadowed(profile);
            }
        }
        helper.assertTrue(found, "polar craters must hold shadowed, icy floors");
        helper.assertFalse(t.column(0, 0).shadowed(profile), "no ice at the equator");
        helper.assertTrue(t.column(0, p * 0.97).zone(profile) == PlanetTerrain.Zone.POLAR, "polar zone near the pole");
        helper.succeed();
    }

    /**
     * Cold traps: shadowed polar crater floors are found by the generator, stay about 60 °C below the lunar night
     * even at noon, and are beyond a first-tier suit but within a heated suit.
     */
    @GameTest(template = EMPTY)
    public static void polarColdTrapsNeedAHeatedSuit(GameTestHelper helper) {
        PlanetChunkGenerator gen = moonGenerator(helper, 5L);
        PlanetTerrain t = gen.terrain();
        double p = t.profile().poleDistance();
        int traps = 0;
        for (Crater c : t.cratersNear(-1500, p * 0.9, 1500, p)) {
            if (c.radius() >= 8 && Math.abs(t.latitude(c.z())) > 85 && gen.coldTrapFloor((int) c.x(), (int) c.z()) != Integer.MIN_VALUE) {
                traps++;
            }
        }
        helper.assertTrue(traps > 0, "polar craters hold cold traps");
        helper.assertTrue(gen.coldTrapFloor(0, 0) == Integer.MIN_VALUE, "no cold trap at the equator");
        double day = 120;
        double night = -173;
        double noon = day + com.pizzaparker07.spacebreaker.content.worldgen.ColdTraps.offsetC(day, night, (float) (Math.PI / 2));
        double midnight = night + com.pizzaparker07.spacebreaker.content.worldgen.ColdTraps.offsetC(day, night, (float) (-Math.PI / 2));
        helper.assertTrue(Math.abs(noon - (night - 60)) < 1e-6 && Math.abs(midnight - (night - 60)) < 1e-6,
                "a cold trap stays at the same deep cold day and night: " + noon + " / " + midnight);
        helper.assertTrue(noon < com.pizzaparker07.spacebreaker.content.suit.SuitTier.MK1.minTempC(), "too cold for a first-tier suit");
        helper.assertTrue(noon >= com.pizzaparker07.spacebreaker.content.suit.SuitTier.HEATED.minTempC(), "a heated suit copes");
        // a profile can switch cold traps off (and older profiles keep them on)
        var codec = TerrainProfile.Poles.CODEC;
        var off = codec.parse(com.mojang.serialization.JsonOps.INSTANCE, com.google.gson.JsonParser.parseString("{\"cold_traps\": false}")).getOrThrow();
        var dflt = codec.parse(com.mojang.serialization.JsonOps.INSTANCE, com.google.gson.JsonParser.parseString("{}")).getOrThrow();
        helper.assertFalse(off.coldTraps(), "cold_traps: false switches them off");
        helper.assertTrue(dflt.coldTraps(), "cold traps are on by default");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void moonColumnsAreLayeredLunarCrust(GameTestHelper helper) {
        PlanetChunkGenerator gen = moonGenerator(helper, 20261106L);
        LevelHeightAccessor height = LevelHeightAccessor.create(-64, 384);
        int lunarTops = 0;
        for (int i = 0; i < 24; i++) {
            int x = i * 211 - 2000;
            int z = i * 157 - 1500;
            int top = gen.surfaceY(x, z, height);
            NoiseColumn col = gen.getBaseColumn(x, z, height, null);
            BlockState surface = col.getBlock(top);
            helper.assertFalse(surface.isAir(), "surface block must be solid at " + x + "," + z);
            helper.assertTrue(col.getBlock(top + 1).isAir(), "air above the surface");
            helper.assertTrue(col.getBlock(-64).is(Blocks.BEDROCK), "bedrock floor");
            helper.assertTrue(BuiltInMoon.isMoonBlock(col.getBlock(top - 10)), "lunar rock below the soil");
            if (BuiltInMoon.isMoonBlock(surface)) {
                lunarTops++;
            }
            helper.assertTrue(gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, null) == top + 1, "base height");
        }
        helper.assertTrue(lunarTops == 24, "every surface must be lunar material");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void moonDungeonsGenerateNaturally(GameTestHelper helper) {
        long seed = 20261106L;
        PlanetChunkGenerator gen = moonGenerator(helper, seed);
        var access = helper.getLevel().registryAccess();
        var state = net.minecraft.world.level.levelgen.RandomState.create(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.dummy(),
                access.lookupOrThrow(net.minecraft.core.registries.Registries.NOISE), seed);
        var structureState = gen.createState(access.lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET), state, seed);
        LevelHeightAccessor height = LevelHeightAccessor.create(-64, 384);
        var structures = access.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        for (var key : List.of(com.pizzaparker07.spacebreaker.content.moon.dungeon.MoonStructures.OUTPOST_KEY,
                com.pizzaparker07.spacebreaker.content.moon.dungeon.MoonStructures.LAVA_TUBE_KEY)) {
            var holder = structures.getHolderOrThrow(key);
            var placements = structureState.getPlacementsForStructure(holder);
            helper.assertTrue(!placements.isEmpty(), key.location() + " has no placement on the Moon");
            int candidates = 0;
            int valid = 0;
            for (int cx = -80; cx <= 80 && valid < 3; cx++) {
                for (int cz = -80; cz <= 80 && valid < 3; cz++) {
                    if (!placements.get(0).isStructureChunk(structureState, cx, cz)) {
                        continue;
                    }
                    candidates++;
                    var ctx = new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(access, gen, gen.getBiomeSource(), state,
                            helper.getLevel().getStructureManager(), seed, new net.minecraft.world.level.ChunkPos(cx, cz), height,
                            b -> holder.value().biomes().contains(b));
                    if (holder.value().findValidGenerationPoint(ctx).isPresent()) {
                        valid++;
                    }
                }
            }
            helper.assertTrue(candidates > 0, key.location() + ": no candidate chunks");
            helper.assertTrue(valid > 0, key.location() + ": " + candidates + " candidate chunks but none valid");
        }
        helper.succeed();
    }

    /**
     * Performance budget of the planet engine: terrain + biomes for fresh chunks, measured on real proto-chunks.
     * Vanilla noise chunks take roughly 5-15 ms each on one core; the Moon must stay well inside that.
     */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void moonChunkGenerationIsFast(GameTestHelper helper) {
        long seed = 20261106L;
        PlanetChunkGenerator gen = moonGenerator(helper, seed);
        var access = helper.getLevel().registryAccess();
        var state = net.minecraft.world.level.levelgen.RandomState.create(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.dummy(),
                access.lookupOrThrow(net.minecraft.core.registries.Registries.NOISE), seed);
        LevelHeightAccessor height = LevelHeightAccessor.create(-64, 384);
        var biomes = access.registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        int warm = 16;
        int n = 96;
        long total = 0;
        for (int i = 0; i < warm + n; i++) {
            var pos = new net.minecraft.world.level.ChunkPos(1000 + i % 12, 400 + i / 12);
            var chunk = new net.minecraft.world.level.chunk.ProtoChunk(pos, net.minecraft.world.level.chunk.UpgradeData.EMPTY, height, biomes, null);
            long t0 = System.nanoTime();
            gen.createBiomes(state, null, null, chunk).join();
            gen.fillFromNoise(null, state, null, chunk).join();
            long dt = System.nanoTime() - t0;
            if (i >= warm) {
                total += dt;
            }
        }
        double ms = total / 1e6 / n;
        com.pizzaparker07.spacebreaker.Spacebreaker.LOGGER.info("[benchmark] Moon terrain + biomes: {} ms per chunk", String.format(java.util.Locale.ROOT, "%.2f", ms));
        helper.assertTrue(ms < 20, "Moon chunk generation too slow: " + ms + " ms per chunk");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void moonBiomesPlayMoonMusic(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        for (var key : List.of(com.pizzaparker07.spacebreaker.content.moon.MoonData.HIGHLANDS, com.pizzaparker07.spacebreaker.content.moon.MoonData.MARE,
                com.pizzaparker07.spacebreaker.content.moon.MoonData.CRATER, com.pizzaparker07.spacebreaker.content.moon.MoonData.VOLCANIC,
                com.pizzaparker07.spacebreaker.content.moon.MoonData.POLAR)) {
            var music = biomes.getOrThrow(key).getBackgroundMusic().orElseThrow(() -> new AssertionError(key.location() + " has no music"));
            helper.assertTrue(music.getEvent().is(Spacebreaker.id("music.moon")), key.location() + " should play music.moon");
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ time and sky

    @GameTest(template = EMPTY)
    public static void moonHasItsOwnLongDay(GameTestHelper helper) {
        CelestialBody moon = Bodies.get(helper.getLevel().registryAccess(), Bodies.MOON).orElseThrow();
        long ticks = PlanetTime.dayTicks(moon.rotation().dayLengthHours());
        helper.assertTrue(ticks > 24000 * 3, "a lunar day lasts several Earth days in game: " + ticks);
        helper.assertTrue(PlanetTime.dayTicks(24) == 24000, "Earth-length day stays 20 minutes");
        helper.assertTrue(PlanetTime.clock(helper.getLevel()).isEmpty(), "the overworld keeps vanilla time");
        PlanetTime.Clock clock = new PlanetTime.Clock(ticks, 0);
        long noon = ticks / 4;
        helper.assertTrue(Math.abs(clock.fraction(noon) - 0.25) < 1e-4, "quarter of the day is noon");
        float tod = clock.timeOfDay(noon);
        helper.assertTrue(tod < 1e-3 || tod > 1 - 1e-3, "noon = vanilla time of day 0, got " + tod);
        helper.assertTrue(Math.abs(clock.timeOfDay(ticks * 3 / 4) - 0.5) < 1e-3, "midnight = vanilla time of day 0.5");
        // the clock offset shifts local time without touching the shared day time
        PlanetTime.Clock shifted = new PlanetTime.Clock(ticks, ticks / 2);
        helper.assertTrue(Math.abs(shifted.fraction(noon) - 0.75) < 1e-4, "offset moves noon to midnight");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void skyGeometry(GameTestHelper helper) {
        // the sun culminates lower at higher latitude; near the poles it skims the horizon
        double noonEquator = SkyMath.sunElevation(0, 0);
        double noonPole = SkyMath.sunElevation(0, 88);
        helper.assertTrue(Math.abs(noonEquator - 90) < 1e-6, "sun overhead at noon on the equator");
        helper.assertTrue(noonPole < 3, "sun near the horizon at the pole, got " + noonPole);
        // Earth: high in the sky on the near side, below the horizon on the far side
        Vector3d near = SkyMath.parentDirection(0, SkyMath.SUB_PARENT_LONGITUDE, 0);
        Vector3d far = SkyMath.parentDirection(0, SkyMath.SUB_PARENT_LONGITUDE + 180, 0);
        helper.assertTrue(near.y > 0.95, "Earth overhead at the sub-Earth point");
        helper.assertTrue(far.y < -0.9, "Earth never rises on the far side");
        // phases: full Earth at lunar midnight, new Earth at lunar noon (seen from the sub-Earth point)
        Vector3d noonSun = SkyMath.sunDirection(0, 0);
        Vector3d midnightSun = SkyMath.sunDirection(0.5, 0);
        Vector3d overhead = new Vector3d(0, 1, 0);
        helper.assertTrue(SkyMath.illuminatedFraction(noonSun, overhead) < 0.01, "new Earth at noon");
        helper.assertTrue(SkyMath.illuminatedFraction(midnightSun, overhead) > 0.99, "full Earth at midnight");
        // apparent sizes from real data: Sun 0.53 deg from 1 AU, Earth 1.9 deg from the Moon
        helper.assertTrue(Math.abs(SkyMath.angularDiameter(695700, 1.496e8) - 0.533) < 0.01, "sun angular size");
        helper.assertTrue(Math.abs(SkyMath.angularDiameter(6371, 384400) - 1.9) < 0.02, "earth angular size");
        helper.succeed();
    }

    // ------------------------------------------------------------------ blocks

    @GameTest(template = EMPTY)
    public static void everyLunarSoilKeepsBootPrints(GameTestHelper helper) {
        List<DeferredBlock<LunarRegolithBlock>> soils = MoonModule.soils();
        for (int i = 0; i < soils.size(); i++) {
            BlockPos pos = new BlockPos(1 + i, 1, 2);
            helper.setBlock(pos, soils.get(i).get());
            BlockPos abs = helper.absolutePos(pos);
            BlockState state = helper.getLevel().getBlockState(abs);
            Zombie walker = helper.spawn(EntityType.ZOMBIE, 1 + i, 2, 2);
            state.getBlock().stepOn(helper.getLevel(), abs, state, walker);
            BlockState printed = helper.getLevel().getBlockState(abs);
            helper.assertTrue(printed.getValue(LunarRegolithBlock.PRINT) != Footprint.NONE, soils.get(i).getId() + " must keep a print");
            walker.discard();
        }
        // a print is permanent: stepping again does not change it
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 2));
        BlockState before = helper.getLevel().getBlockState(abs);
        BlockState again = LunarRegolithBlock.printFor(before, abs, Direction.EAST);
        helper.assertTrue(before.getValue(LunarRegolithBlock.PRINT) == again.getValue(LunarRegolithBlock.PRINT), "same foot for the same block");
        helper.succeed();
    }

    /** Lunar block check without hard-coding every block. */
    static final class BuiltInMoon {
        static boolean isMoonBlock(BlockState s) {
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock());
            return id.getNamespace().equals(Spacebreaker.MOD_ID);
        }
    }

    private MoonGameTests() {
    }
}
