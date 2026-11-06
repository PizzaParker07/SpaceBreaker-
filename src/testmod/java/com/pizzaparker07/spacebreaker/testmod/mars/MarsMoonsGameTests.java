package com.pizzaparker07.spacebreaker.testmod.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.mars.dungeon.MarsLoot;
import com.pizzaparker07.spacebreaker.content.mars.dungeon.MarsStructures;
import com.pizzaparker07.spacebreaker.content.mars.dungeon.MoonletDungeonPiece;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
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
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MarsMoonsGameTests {
    @GameTest(template = WorldTests.EMPTY)
    public static void phobosAndDeimosAreTinyAirlessMoonsOfMars(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        for (String name : new String[]{"phobos", "deimos"}) {
            var stem = WorldTests.stem(helper, name);
            helper.assertTrue(stem.generator() instanceof PlanetChunkGenerator, name + " uses the planet engine");
            helper.assertTrue(stem.type().value().effectsLocation().equals(Spacebreaker.id(name)), name + " sky effects");
            CelestialBody body = Bodies.forDimension(access, name.equals("phobos") ? MarsModule.PHOBOS : MarsModule.DEIMOS)
                    .orElseThrow(() -> new AssertionError(name + " has no body"));
            helper.assertTrue(body.atmosphere().isVacuum(), name + " is airless");
            helper.assertTrue(body.physical().surfaceGravity() < 0.01, name + " has almost no gravity");
            helper.assertTrue(body.parent().map(p -> p.equals(Spacebreaker.id("mars"))).orElse(false), name + " orbits Mars");
            TerrainProfile p = WorldTests.profile(helper, name);
            helper.assertTrue(p.poleDistance() < 1000, name + " is small");
        }
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void phobosIsGrooved(GameTestHelper helper) {
        TerrainProfile p = WorldTests.profile(helper, "phobos");
        helper.assertTrue(p.landforms().grooves().isPresent(), "Phobos has groove chains");
        PlanetTerrain t = new PlanetTerrain(p, 3L);
        // grooves cut regular dips: along a line across them the height must dip repeatedly
        int dips = 0;
        double prev = t.height(0, 0);
        boolean falling = false;
        for (int i = 1; i < 400; i++) {
            double h = t.height(0, i);
            if (h < prev - 0.3 && !falling) {
                dips++;
                falling = true;
            } else if (h > prev + 0.3) {
                falling = false;
            }
            prev = h;
        }
        helper.assertTrue(dips >= 4, "groove dips across Phobos: " + dips);
        WorldTests.dumpMap(t, "phobos", 0, 0, 1800, 450);
        PlanetChunkGenerator gen = WorldTests.generator(helper, "phobos", 11L);
        var col = gen.getBaseColumn(40, 40, LevelHeightAccessor.create(-64, 384), null);
        helper.assertTrue(col.getBlock(gen.surfaceY(40, 40, LevelHeightAccessor.create(-64, 384))).getBlock() instanceof LunarRegolithBlock,
                "Phobos soil keeps boot prints forever");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void moonletDungeonsHoldTheirVaults(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var access = level.registryAccess();
        helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE).containsKey(MarsStructures.PHOBOS_PROBE_KEY), "probe structure");
        helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE).containsKey(MarsStructures.DEIMOS_CACHE_KEY), "cache structure");
        for (var key : new net.minecraft.resources.ResourceKey[]{MarsLoot.PHOBOS_PROBE, MarsLoot.DEIMOS_CACHE}) {
            @SuppressWarnings("unchecked")
            var k = (net.minecraft.resources.ResourceKey<LootTable>) key;
            helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(k) != LootTable.EMPTY, "loot " + k.location());
        }
        for (boolean cache : new boolean[]{false, true}) {
            BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(cache ? 3000 : -3000, 0, 3000);
            MoonletDungeonPiece piece = new MoonletDungeonPiece(9L, cache, base.getX(), 100, base.getZ());
            BoundingBox bb = piece.getBoundingBox();
            for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
                for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                    level.getChunk(cx, cz);
                    piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(cx),
                            new BoundingBox(cx << 4, -64, cz << 4, (cx << 4) + 15, 319, (cz << 4) + 15), new ChunkPos(cx, cz), bb.getCenter());
                }
            }
            int chests = 0;
            for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
                if (level.getBlockState(p).is(Blocks.CHEST) && level.getBlockEntity(p) instanceof ChestBlockEntity c && c.getLootTable() != null) {
                    chests++;
                }
            }
            helper.assertTrue(chests >= 1, (cache ? "ice cache" : "probe wreck") + " needs its loot chest");
        }
        helper.succeed();
    }

    private MarsMoonsGameTests() {
    }
}
