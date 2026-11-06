package com.pizzaparker07.spacebreaker.testmod.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.dungeons.Dungeons;
import com.pizzaparker07.spacebreaker.content.mars.MarsData;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.mars.dungeon.MarsLoot;
import com.pizzaparker07.spacebreaker.content.mars.dungeon.MarsStructures;
import com.pizzaparker07.spacebreaker.content.mars.dungeon.ResearchStationPiece;
import com.pizzaparker07.spacebreaker.content.mars.dungeon.RiverbedCavePiece;
import com.pizzaparker07.spacebreaker.content.mars.entity.CanyonLurker;
import com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities;
import com.pizzaparker07.spacebreaker.content.mars.entity.RustBeetle;
import com.pizzaparker07.spacebreaker.content.mars.entity.Stonegrazer;
import com.pizzaparker07.spacebreaker.content.research.TechDiskItem;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
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
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MarsLifeGameTests {
    private static final String EMPTY = WorldTests.EMPTY;

    @GameTest(template = EMPTY)
    public static void marsBiomesSpawnTheirCreatures(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        Set<EntityType<?>> seen = new HashSet<>();
        for (var key : MarsData.MARS_BIOMES) {
            var biome = biomes.getOrThrow(key);
            for (MobCategory c : MobCategory.values()) {
                for (MobSpawnSettings.SpawnerData d : biome.getMobSettings().getMobs(c).unwrap()) {
                    seen.add(d.type);
                }
            }
        }
        for (EntityType<?> t : List.of(MarsEntities.RUST_BEETLE.get(), MarsEntities.STONEGRAZER.get(), MarsEntities.CANYON_LURKER.get(),
                MarsEntities.POLAR_WORM.get())) {
            helper.assertTrue(seen.contains(t), "no martian biome spawns " + EntityType.getKey(t));
        }
        helper.assertFalse(seen.contains(MarsEntities.DUST_DEVIL.get()), "dust devils only form in storms, never as biome spawns");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void canyonLurkerWaitsDisguisedThenAmbushes(GameTestHelper helper) {
        CanyonLurker lurker = helper.spawn(MarsEntities.CANYON_LURKER.get(), new BlockPos(1, 2, 1));
        helper.assertTrue(lurker.isHidden(), "lurkers start disguised as rock");
        lurker.hurt(helper.getLevel().damageSources().generic(), 1F);
        helper.assertFalse(lurker.isHidden(), "a hit reveals the lurker");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void stonegrazerBreedsWithGypsum(GameTestHelper helper) {
        Stonegrazer g = helper.spawn(MarsEntities.STONEGRAZER.get(), new BlockPos(1, 2, 1));
        helper.assertTrue(g.isFood(new ItemStack(MarsModule.GYPSUM_CRYSTAL.get())), "gypsum is stonegrazer food");
        helper.assertFalse(g.isFood(new ItemStack(net.minecraft.world.item.Items.WHEAT)), "stonegrazers do not eat wheat");
        helper.assertTrue(g.getBreedOffspring(helper.getLevel(), g) instanceof Stonegrazer, "offspring");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rustBeetleIsNeutral(GameTestHelper helper) {
        RustBeetle b = helper.spawn(MarsEntities.RUST_BEETLE.get(), new BlockPos(1, 2, 1));
        helper.assertTrue(b.getTarget() == null, "beetles do not hunt");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void marsDungeonsAreRegistered(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE).containsKey(MarsStructures.STATION_KEY), "station structure");
        helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE).containsKey(MarsStructures.RIVERBED_KEY), "riverbed structure");
        helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE_SET).containsKey(MarsStructures.STATION_SET), "station placement");
        helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE_SET).containsKey(MarsStructures.RIVERBED_SET), "riverbed placement");
        var defs = Dungeons.all(access);
        helper.assertTrue(defs.containsKey(Spacebreaker.id("mars_research_station")), "station dungeon definition");
        helper.assertTrue(defs.containsKey(Spacebreaker.id("mars_riverbed_cave")), "riverbed dungeon definition");
        helper.succeed();
    }

    /** Every Mars dungeon table yields tech disks for real technologies, including the new Phase-2 ones. */
    @GameTest(template = EMPTY)
    public static void marsDungeonLootTeachesTechnology(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var server = level.getServer();
        Set<String> techs = new HashSet<>();
        for (ResourceKey<LootTable> key : List.of(MarsLoot.STATION_COMMAND, MarsLoot.STATION_LAB, MarsLoot.STATION_ISRU, MarsLoot.RIVERBED_PROBE)) {
            LootTable table = server.reloadableRegistries().getLootTable(key);
            helper.assertTrue(table != LootTable.EMPTY, "loot table " + key.location());
            boolean disk = false;
            for (int i = 0; i < 12; i++) {
                LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
                for (ItemStack s : table.getRandomItems(params, 1000L + i)) {
                    if (s.getItem() instanceof TechDiskItem) {
                        var id = TechDiskItem.tech(s);
                        helper.assertTrue(id != null && Tech.get(access(helper), id).isPresent(), "disk for unknown tech " + id + " in " + key.location());
                        techs.add(id.getPath());
                        disk = true;
                    }
                }
            }
            helper.assertTrue(disk, key.location() + " never gives a tech disk");
        }
        helper.assertTrue(techs.contains("dust_proof_seals") || techs.contains("methane_engines") || techs.contains("sabatier_process"),
                "Mars teaches its own technologies: " + techs);
        for (String id : List.of("dust_proof_seals", "heat_resistant_electronics", "acid_resistant_coatings", "polar_ice_mining")) {
            helper.assertTrue(Tech.get(access(helper), Spacebreaker.id(id)).isPresent(), "phase 2 tech " + id);
        }
        helper.succeed();
    }

    private static net.minecraft.core.RegistryAccess access(GameTestHelper helper) {
        return helper.getLevel().registryAccess();
    }

    private static void place(ServerLevel level, StructurePiece piece) {
        BoundingBox bb = piece.getBoundingBox();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
            for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                level.getChunk(cx, cz);
                BoundingBox cb = new BoundingBox(cx << 4, level.getMinBuildHeight(), cz << 4, (cx << 4) + 15, level.getMaxBuildHeight(), (cz << 4) + 15);
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(cx * 31L + cz),
                        cb, new ChunkPos(cx, cz), bb.getCenter());
            }
        }
    }

    private static int lootChests(ServerLevel level, BoundingBox bb) {
        int chests = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            if (level.getBlockState(p).is(Blocks.CHEST) && level.getBlockEntity(p) instanceof ChestBlockEntity chest && chest.getLootTable() != null) {
                chests++;
            }
        }
        return chests;
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void researchStationHasLootAirlockAndBeetles(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-4000, 0, 4000);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, base.getX(), base.getZ());
        ResearchStationPiece piece = new ResearchStationPiece(42L, base.getX(), y, base.getZ(), Direction.NORTH);
        place(level, piece);
        BoundingBox bb = piece.getBoundingBox();
        int doors = 0;
        int spawners = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            var st = level.getBlockState(p);
            if (st.is(Blocks.IRON_DOOR)) {
                doors++;
            }
            if (st.is(Blocks.SPAWNER)) {
                spawners++;
            }
        }
        helper.assertTrue(lootChests(level, bb) >= 4, "station needs loot chests, found " + lootChests(level, bb));
        helper.assertTrue(doors == 4, "airlock with two iron doors, found " + doors);
        helper.assertTrue(spawners == 1, "rust beetle nest in the garage");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void riverbedCaveIsContinuousAndSaves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(4000, 0, -4000);
        int top = 100; // the test world is a thin superflat: carve the channel in the open air above it
        RiverbedCavePiece piece = new RiverbedCavePiece(7L, base.getX(), top, base.getZ());
        CompoundTag tag = piece.createTag(StructurePieceSerializationContext.fromLevel(level));
        RiverbedCavePiece loaded = new RiverbedCavePiece(StructurePieceSerializationContext.fromLevel(level), tag);
        helper.assertTrue(loaded.getBoundingBox().equals(piece.getBoundingBox()), "piece survives save and load");
        helper.assertTrue(piece.entrance().getY() < top - 10, "the channel lies buried");
        helper.assertTrue(piece.getBoundingBox().maxY() >= top, "the sinkhole reaches the surface");
        place(level, piece);
        helper.assertTrue(level.getBlockState(piece.chamber()).isAir(), "the chamber is open");
        helper.assertTrue(lootChests(level, piece.getBoundingBox()) >= 1, "the probe wreck holds its data vault");
        helper.succeed();
    }

    private MarsLifeGameTests() {
    }
}
