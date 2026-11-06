package com.pizzaparker07.spacebreaker.testmod.moon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.moon.dungeon.LavaTubePiece;
import com.pizzaparker07.spacebreaker.content.moon.dungeon.MoonLoot;
import com.pizzaparker07.spacebreaker.content.moon.dungeon.MoonStructures;
import com.pizzaparker07.spacebreaker.content.moon.dungeon.OutpostPiece;
import com.pizzaparker07.spacebreaker.content.moon.entity.DustSwimmer;
import com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities;
import com.pizzaparker07.spacebreaker.content.moon.entity.RegolithTick;
import com.pizzaparker07.spacebreaker.content.moon.entity.StunBolt;
import com.pizzaparker07.spacebreaker.content.moon.entity.SurveyDrone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MoonLifeGameTests {
    private static final String EMPTY = "empty";

    // ------------------------------------------------------------------ creatures

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void regolithTickBurrowsInSoilAndSurfacesWhenHit(GameTestHelper helper) {
        helper.setBlock(1, 0, 1, MoonModule.LUNAR_REGOLITH.get());
        helper.setBlock(3, 0, 3, MoonModule.MARE_BASALT.get());
        RegolithTick inSoil = helper.spawn(MoonEntities.REGOLITH_TICK.get(), 1, 1, 1);
        RegolithTick onRock = helper.spawn(MoonEntities.REGOLITH_TICK.get(), 3, 1, 3);
        inSoil.setNoAi(true);
        onRock.setNoAi(true);
        helper.assertFalse(inSoil.isBurrowed(), "a new tick is above ground");
        helper.assertTrue(((Object) inSoil) instanceof Monster, "ticks are hostile");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(inSoil.tryBurrow() && inSoil.isBurrowed(), "a tick buries itself in regolith");
            helper.assertFalse(onRock.tryBurrow(), "a tick cannot dig into basalt");
            inSoil.hurt(helper.getLevel().damageSources().generic(), 1F);
            helper.assertFalse(inSoil.isBurrowed(), "a hit tick bursts out of the dust");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void elderTickIsTougher(GameTestHelper helper) {
        RegolithTick tick = helper.spawn(MoonEntities.REGOLITH_TICK.get(), 1, 2, 1);
        RegolithTick elder = helper.spawn(MoonEntities.ELDER_REGOLITH_TICK.get(), 3, 2, 3);
        helper.assertTrue(elder.isElder() && !tick.isElder(), "elder flag");
        helper.assertTrue(elder.getMaxHealth() >= tick.getMaxHealth() * 3, "elders have much more health");
        helper.assertTrue(elder.getBbWidth() > tick.getBbWidth(), "elders are bigger");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void dustSwimmerIsPassive(GameTestHelper helper) {
        DustSwimmer swimmer = helper.spawn(MoonEntities.DUST_SWIMMER.get(), 2, 2, 2);
        helper.assertFalse(((Object) swimmer) instanceof Monster, "dust swimmers are harmless");
        helper.assertFalse(swimmer.removeWhenFarAway(200), "swimmers do not despawn like monsters");
        helper.assertTrue(MoonEntities.DUST_SWIMMER.get().getCategory() == net.minecraft.world.entity.MobCategory.CREATURE, "creature category");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void surveyDroneFiresStunBolts(GameTestHelper helper) {
        SurveyDrone drone = helper.spawn(MoonEntities.SURVEY_DRONE.get(), 1, 3, 1);
        Zombie target = helper.spawn(EntityType.ZOMBIE, 3, 2, 3);
        helper.assertTrue(drone.isNoGravity(), "drones hover");
        drone.performRangedAttack(target, 1F);
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8);
        List<StunBolt> bolts = helper.getLevel().getEntitiesOfClass(StunBolt.class, area);
        helper.assertTrue(!bolts.isEmpty(), "a stun bolt must be fired");
        helper.succeed();
    }

    // ------------------------------------------------------------------ dungeons

    @GameTest(template = EMPTY)
    public static void dungeonDataIsRegistered(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE).containsKey(MoonStructures.OUTPOST_KEY), "survey outpost structure");
        helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE).containsKey(MoonStructures.LAVA_TUBE_KEY), "lava tube structure");
        helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE_SET).containsKey(MoonStructures.OUTPOST_SET), "outpost placement");
        var server = helper.getLevel().getServer();
        for (ResourceKey<LootTable> key : List.of(MoonLoot.OUTPOST_LAB, MoonLoot.OUTPOST_STORAGE, MoonLoot.OUTPOST_COMMAND,
                MoonLoot.LAVA_TUBE_PROBE)) {
            helper.assertTrue(server.reloadableRegistries().getLootTable(key) != LootTable.EMPTY, "loot table " + key.location());
        }
        helper.assertTrue(server.reloadableRegistries().getKeys(Registries.LOOT_TABLE).contains(MoonLoot.TECH_DATA.location()),
                "tech data hook table must exist for the research disks");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void outpostHasAirlockLootAndGuards(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // far from the other tests, on the flat test world
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(4000, 0, 4000);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, base.getX(), base.getZ());
        OutpostPiece piece = new OutpostPiece(99L, base.getX(), y, base.getZ(), Direction.SOUTH);
        BoundingBox bb = piece.getBoundingBox();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
            for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                level.getChunk(cx, cz);
                BoundingBox cb = new BoundingBox(cx << 4, level.getMinBuildHeight(), cz << 4, (cx << 4) + 15, level.getMaxBuildHeight(), (cz << 4) + 15);
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(cx * 31L + cz),
                        cb, new ChunkPos(cx, cz), bb.getCenter());
            }
        }
        int chests = 0;
        int spawners = 0;
        int doors = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            var state = level.getBlockState(p);
            if (state.is(Blocks.CHEST) && level.getBlockEntity(p) instanceof ChestBlockEntity chest && chest.getLootTable() != null) {
                chests++;
            }
            if (state.is(Blocks.SPAWNER) && level.getBlockEntity(p) instanceof SpawnerBlockEntity) {
                spawners++;
            }
            if (state.is(Blocks.IRON_DOOR)) {
                doors++;
            }
        }
        helper.assertTrue(chests >= 4, "outpost needs loot chests, found " + chests);
        helper.assertTrue(spawners == 1, "one drone dispenser");
        helper.assertTrue(doors == 4, "airlock with two iron doors (4 halves), found " + doors);
        int drones = level.getEntitiesOfClass(SurveyDrone.class, AABB.of(bb).inflate(2)).size();
        helper.assertTrue(drones >= 2, "drones guard the outpost, found " + drones);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void lavaTubePathIsContinuousAndSaves(GameTestHelper helper) {
        List<int[]> path = LavaTubePiece.walk(RandomSource.create(5L), 100, 200, (x, z) -> 80);
        helper.assertTrue(path.size() >= 22, "tube long enough");
        for (int i = 1; i < path.size(); i++) {
            int[] a = path.get(i - 1);
            int[] b = path.get(i);
            double step = Math.hypot(b[0] - a[0], b[2] - a[2]);
            helper.assertTrue(step <= 6, "tube is continuous, step " + step);
            helper.assertTrue(b[1] <= 80 - 9 && b[1] >= 80 - 16, "tube stays under the surface: y " + b[1]);
        }
        LavaTubePiece piece = new LavaTubePiece(5L, path, 80);
        CompoundTag tag = piece.createTag(StructurePieceSerializationContext.fromLevel(helper.getLevel()));
        LavaTubePiece loaded = new LavaTubePiece(StructurePieceSerializationContext.fromLevel(helper.getLevel()), tag);
        helper.assertTrue(loaded.getBoundingBox().equals(piece.getBoundingBox()), "piece survives save and load");
        helper.assertTrue(piece.getBoundingBox().maxY() >= 80, "the skylight reaches the surface");
        helper.succeed();
    }

    private MoonLifeGameTests() {
    }
}
