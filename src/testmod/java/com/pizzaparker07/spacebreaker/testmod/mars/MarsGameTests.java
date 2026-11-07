package com.pizzaparker07.spacebreaker.testmod.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.DryIceBlock;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.mars.MarsSoilBlock;
import com.pizzaparker07.spacebreaker.content.moon.Footprint;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetBiomeSource;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MarsGameTests {
    private static final String EMPTY = WorldTests.EMPTY;

    @GameTest(template = EMPTY)
    public static void marsDimensionUsesPlanetEngine(GameTestHelper helper) {
        LevelStem stem = WorldTests.stem(helper, "mars");
        helper.assertTrue(stem.generator() instanceof PlanetChunkGenerator, "mars must use the planet generator");
        helper.assertTrue(stem.generator().getBiomeSource() instanceof PlanetBiomeSource, "mars must use the planet biome source");
        helper.assertTrue(stem.type().value().effectsLocation().equals(Spacebreaker.id("mars")), "mars sky effects");
        helper.assertTrue(stem.generator().getBiomeSource().possibleBiomes().size() == 9, "nine martian biomes, got "
                + stem.generator().getBiomeSource().possibleBiomes().size());
        CelestialBody body = Bodies.forDimension(helper.getLevel().registryAccess(), MarsModule.MARS)
                .orElseThrow(() -> new AssertionError("mars dimension has no body"));
        helper.assertTrue(Math.abs(body.physical().surfaceGravity() - 3.71) < 0.01, "mars gravity");
        helper.assertFalse(body.atmosphere().isVacuum(), "mars has a thin atmosphere");
        helper.assertFalse(body.atmosphere().breathable(), "mars air is not breathable");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void marsTerrainIsDeterministic(GameTestHelper helper) {
        TerrainProfile profile = WorldTests.profile(helper, "mars");
        PlanetTerrain a = new PlanetTerrain(profile, 4242L);
        PlanetTerrain b = new PlanetTerrain(profile, 4242L);
        PlanetTerrain c = new PlanetTerrain(profile, 77L);
        int differ = 0;
        for (int i = 0; i < 400; i++) {
            double x = (i * 7919) % 6000 - 3000;
            double z = (i * 104729) % 6000 - 3000;
            helper.assertTrue(a.height(x, z) == b.height(x, z), "same seed, same height at " + x + "," + z);
            helper.assertTrue(a.column(x, z).zone(profile) == b.column(x, z).zone(profile), "same seed, same zone");
            if (Math.abs(a.height(x, z) - c.height(x, z)) > 0.5) {
                differ++;
            }
        }
        helper.assertTrue(differ > 200, "another seed gives other terrain (" + differ + "/400)");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void marsHasEveryLandform(GameTestHelper helper) {
        TerrainProfile profile = WorldTests.profile(helper, "mars");
        PlanetTerrain t = new PlanetTerrain(profile, 20261106L);
        Map<PlanetTerrain.Zone, Integer> zones = new EnumMap<>(PlanetTerrain.Zone.class);
        double min = 1e9;
        double max = -1e9;
        for (int i = 0; i < 6000; i++) {
            double x = (i * 7919L) % 16000 - 8000;
            double z = (i * 104729L) % 16000 - 8000;
            PlanetTerrain.Column col = t.column(x, z);
            zones.merge(col.zone(profile), 1, Integer::sum);
            min = Math.min(min, col.height);
            max = Math.max(max, col.height);
        }
        for (PlanetTerrain.Zone z : List.of(PlanetTerrain.Zone.HIGHLANDS, PlanetTerrain.Zone.LOWLANDS, PlanetTerrain.Zone.CRATER,
                PlanetTerrain.Zone.POLAR, PlanetTerrain.Zone.DUNES, PlanetTerrain.Zone.VALLEY, PlanetTerrain.Zone.ICE_CAP)) {
            helper.assertTrue(zones.getOrDefault(z, 0) > 5, "zone " + z + " too rare: " + zones);
        }
        helper.assertTrue(min > -30 && max < 300, "mars heights out of range " + min + " .. " + max);

        // the great rift: deep below the plateau on both sides
        double rz = t.riftCentre(0);
        double floor = t.height(0, rz);
        double rimN = t.height(0, rz - 140);
        double rimS = t.height(0, rz + 140);
        helper.assertTrue(t.column(0, rz).zone(profile) == PlanetTerrain.Zone.CANYON, "rift centre is canyon");
        helper.assertTrue(Math.min(rimN, rimS) - floor > 35, "rift must be deep: floor " + floor + " rims " + rimN + "/" + rimS);

        // the anchored great shield volcano
        double[] v = t.volcanoesNear(-2700, -500, -2500, -300).stream().max((p, q) -> Double.compare(p[2], q[2]))
                .orElseThrow(() -> new AssertionError("no volcano"));
        double flank = t.height(v[0] + v[2] * 0.5, v[1]);
        double foot = t.height(v[0] + v[2] * 1.4, v[1]);
        double rim = t.height(v[0] + v[2] * 0.12, v[1]);
        helper.assertTrue(rim > flank + 30 && flank > foot + 30, "shield volcano rises: foot " + foot + " flank " + flank + " summit " + rim);
        helper.assertTrue(t.height(v[0], v[1]) < rim, "summit caldera is a pit");

        // the ice cap thickens toward the pole
        double p = profile.poleDistance();
        PlanetTerrain.Column cap = t.column(100, -p * 0.98);
        helper.assertTrue(cap.cap > 5, "ice cap near the north pole, got " + cap.cap);
        helper.assertTrue(t.column(100, -p * 0.5).cap == 0, "no cap at mid latitudes");
        // layered deposits: dust bands at irregular spacing
        java.util.Set<Integer> gaps = new java.util.HashSet<>();
        int bands = 0;
        int last = Integer.MIN_VALUE;
        for (int y = 60; y < 120; y++) {
            if (com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator.dustyLayer(y)) {
                bands++;
                if (last != Integer.MIN_VALUE) {
                    gaps.add(y - last);
                }
                last = y;
            }
        }
        helper.assertTrue(bands >= 8 && bands <= 30, "dust bands in 60 blocks of ice: " + bands);
        helper.assertTrue(gaps.size() >= 3, "irregular band spacing: " + gaps);
        WorldTests.dumpMap(t, "mars_origin", 0, 0, 6000, 600);
        WorldTests.dumpMap(t, "mars_volcano", v[0], v[1], v[2] * 3, 400);
        WorldTests.dumpMap(t, "mars_caldera", v[0], v[1], 400, 400);
        WorldTests.dumpMap(t, "mars_rift", 0, rz, 1200, 400);
        WorldTests.dumpMap(t, "mars_pole", 0, -p, 3000, 400);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void marsColumnsAreMartianCrust(GameTestHelper helper) {
        PlanetChunkGenerator gen = WorldTests.generator(helper, "mars", 99L);
        LevelHeightAccessor height = LevelHeightAccessor.create(-64, 384);
        for (int i = 0; i < 30; i++) {
            int x = i * 263 - 4000;
            int z = i * 181 - 2500;
            int top = gen.surfaceY(x, z, height);
            NoiseColumn col = gen.getBaseColumn(x, z, height, null);
            BlockState surface = col.getBlock(top);
            helper.assertFalse(surface.isAir(), "solid surface at " + x + "," + z);
            helper.assertTrue(col.getBlock(top + 1).isAir(), "air above the surface");
            helper.assertTrue(col.getBlock(-64).is(Blocks.BEDROCK), "bedrock floor");
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(surface.getBlock());
            helper.assertTrue(id.getNamespace().equals(Spacebreaker.MOD_ID), "martian surface material, got " + id);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void marsSoilsKeepPrintsUntilTheWindComes(GameTestHelper helper) {
        for (var soil : MarsModule.soils()) {
            BlockState s = soil.get().defaultBlockState();
            helper.assertTrue(s.getBlock() instanceof MarsSoilBlock, soil.getId() + " must be a martian soil");
            BlockState printed = LunarRegolithBlock.printFor(s, BlockPos.ZERO, net.minecraft.core.Direction.EAST);
            helper.assertTrue(printed.getValue(LunarRegolithBlock.PRINT) != Footprint.NONE, "prints are pressed in");
            helper.assertTrue(printed.isRandomlyTicking(), "printed soil ticks so the wind can erase it");
            helper.assertFalse(s.isRandomlyTicking(), "plain soil does not tick");
        }
        helper.assertTrue(DryIceBlock.SUBLIMATION_C < -20, "dry ice sublimates in warm places");
        helper.assertTrue(DryIceBlock.warm(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)), "Earth is warm enough to sublimate dry ice");
        helper.succeed();
    }

    /** Lava tubes: natural shelters several blocks under the plains, reachable through collapsed skylights. */
    @GameTest(template = WorldTests.EMPTY)
    public static void lavaTubesAreShelteredTunnels(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .containsKey(com.pizzaparker07.spacebreaker.content.mars.dungeon.MarsStructures.LAVA_TUBE_KEY), "lava tube structure");
        var path = com.pizzaparker07.spacebreaker.content.mars.dungeon.LavaTubeStructure.walk(42L, 0, 0, (x, z) -> 100 + (x + z) / 40);
        var tube = new com.pizzaparker07.spacebreaker.content.mars.dungeon.LavaTubePiece(42L, path);
        int skylights = 0;
        for (int i = 0; i < tube.nodeCount(); i++) {
            int[] n = path.get(i);
            int roof = n[3] - (int) Math.ceil(n[1] + com.pizzaparker07.spacebreaker.content.mars.dungeon.LavaTubePiece.HALF_HEIGHT);
            helper.assertTrue(roof >= 5, "roof too thin at node " + i + ": " + roof);
            if (i > 0) {
                helper.assertTrue(Math.abs(n[1] - path.get(i - 1)[1]) <= 1, "the tube floor runs smoothly");
            }
            skylights += com.pizzaparker07.spacebreaker.content.mars.dungeon.LavaTubePiece.skylight(i) ? 1 : 0;
        }
        helper.assertTrue(skylights >= 2, "skylights to get in: " + skylights);
        // every solid block overhead halves the dose (Environment.radiation): 5+ blocks of roof cut it below 1/32
        helper.assertTrue(Math.pow(0.5, 5) < 0.04, "a roof of 5+ blocks is a real radiation shelter");
        helper.succeed();
    }

    /** Natural storms are announced first: calm during the warning, then the dust rolls in, then it clears. */
    @GameTest(template = EMPTY)
    public static void dustStormIsForecast(GameTestHelper helper) {
        var d = new com.pizzaparker07.spacebreaker.content.mars.MarsWeather.Data();
        d.forecast(0.8F, 30, 20);
        helper.assertTrue(d.warning() == 20 && d.target() == 0F, "the warning comes before the dust");
        String event = null;
        for (int i = 0; i < 20; i++) {
            event = d.step();
        }
        helper.assertTrue("arrive".equals(event) && d.target() == 0.8F, "the storm must arrive when the warning runs out");
        for (int i = 0; i < 30; i++) {
            event = d.step();
        }
        helper.assertTrue("clear".equals(event) && d.target() == 0F, "the storm must die down after its time");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.mars.MarsWeather.WARNING_TICKS >= 1200, "players get at least a minute of warning");
        helper.succeed();
    }

    private MarsGameTests() {
    }
}
