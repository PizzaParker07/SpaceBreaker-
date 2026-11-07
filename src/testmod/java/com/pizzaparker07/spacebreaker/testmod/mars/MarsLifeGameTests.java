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
            if (st.is(com.pizzaparker07.spacebreaker.content.suit.SuitModule.AIRLOCK_DOOR.get())) {
                doors++;
            }
            if (st.is(Blocks.SPAWNER)) {
                spawners++;
            }
        }
        int lockers = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            if (level.getBlockEntity(p) instanceof com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlockEntity l
                    && l.getLootTable() != null) {
                lockers++;
            }
        }
        helper.assertTrue(lockers >= 4, "station needs loot in its supply lockers, found " + lockers);
        helper.assertTrue(lootChests(level, bb) == 0, "no vanilla chests in the station");
        helper.assertTrue(doors == 4, "airlock with two airlock doors, found " + doors);
        helper.assertTrue(spawners == 1, "rust beetle nest in the garage");
        int traps = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            if (level.getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity d
                    && d.getItem(0).is(net.minecraft.world.item.Items.ARROW)) {
                traps++;
            }
            var st = level.getBlockState(p);
            if (st.is(Blocks.STONE_BUTTON)) {
                helper.assertTrue(st.canSurvive(level, p), "an airlock button hangs in the air at " + p);
            }
        }
        helper.assertTrue(traps == 1, "the perimeter-defence arrow trap, found " + traps);
        helper.succeed();
    }

    /** The lost lander: a sealed data recorder with probe data, a spread parachute and the wreck, far out for a rover. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void lostLanderHoldsProbeData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .containsKey(com.pizzaparker07.spacebreaker.content.mars.dungeon.MarsStructures.LANDER_KEY), "lander structure");
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-4000, 0, -4000);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, base.getX(), base.getZ());
        var piece = new com.pizzaparker07.spacebreaker.content.mars.dungeon.LanderWreckPiece(9L, base.getX(), y, base.getZ());
        place(level, piece);
        BoundingBox bb = piece.getBoundingBox();
        int carpet = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            var st = level.getBlockState(p);
            carpet += st.is(Blocks.ORANGE_CARPET) || st.is(Blocks.WHITE_CARPET) ? 1 : 0;
        }
        helper.assertTrue(lootChests(level, bb) == 1, "one data recorder, found " + lootChests(level, bb));
        helper.assertTrue(carpet > 15, "a spread parachute, found " + carpet);
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

    /** Phobos' Stickney: a real, fixed giant crater, with one anchored lander on its floor held down by harpoon tethers. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void stickneyHoldsTheAnchoredLander(GameTestHelper helper) {
        var profile = WorldTests.profile(helper, "phobos");
        var terrain = new com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain(profile, 1234L);
        var stickney = terrain.namedCrater("stickney").orElseThrow(() -> new AssertionError("Phobos must have Stickney"));
        helper.assertTrue(stickney.radius() >= 200, "Stickney spans a large part of the moon");
        double floor = terrain.height(stickney.x(), stickney.z());
        double outside = terrain.height(stickney.x() + stickney.radius() * 2.5, stickney.z());
        helper.assertTrue(outside - floor > 35, "Stickney is a deep bowl: floor " + floor + ", outside " + outside);
        helper.assertTrue(Math.hypot(stickney.x(), stickney.z()) < 600, "Stickney lies near the landing site");
        var site = com.pizzaparker07.spacebreaker.content.mars.dungeon.StickneyLanderStructure.site(WorldTests.stem(helper, "phobos").generator())
                .orElseThrow(() -> new AssertionError("the lander has a site"));
        helper.assertTrue(Math.hypot(site.getX() - stickney.x(), site.getZ() - stickney.z()) < stickney.radius() * 0.3, "the lander sits on the floor");
        helper.assertTrue(access(helper).registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .containsKey(com.pizzaparker07.spacebreaker.content.mars.dungeon.MarsStructures.STICKNEY_KEY), "stickney structure");

        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-4000, 0, 4000);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, base.getX(), base.getZ());
        var piece = new com.pizzaparker07.spacebreaker.content.mars.dungeon.StickneyLanderPiece(5L, base.getX(), y, base.getZ());
        place(level, piece);
        BoundingBox bb = piece.getBoundingBox();
        int chains = 0;
        int signs = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            var st = level.getBlockState(p);
            chains += st.is(Blocks.CHAIN) ? 1 : 0;
            signs += st.is(Blocks.OAK_SIGN) ? 1 : 0;
        }
        int recorders = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            if (level.getBlockEntity(p) instanceof com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlockEntity l && l.getLootTable() != null) {
                recorders++;
            }
        }
        helper.assertTrue(recorders == 1, "one survey recorder, found " + recorders);
        helper.assertTrue(chains >= 12, "three harpoon tethers, found " + chains + " chain blocks");
        helper.assertTrue(signs == 1, "the plate that explains the anchoring");
        helper.succeed();
    }

    /** Peak height of a standing jump (MC physics: jump velocity and gravity scaled like Gravity.apply, 0.98 drag). */
    private static double jumpPeak(double factor) {
        double v = 0.42 * Math.pow(factor, 0.25);
        double g = 0.08 * factor;
        double y = 0;
        double peak = 0;
        for (int t = 0; t < 20000 && y >= 0; t++) {
            y += v;
            v = (v - g) * 0.98;
            peak = Math.max(peak, y);
        }
        return peak;
    }

    /**
     * Deimos' relay spire: a spiral of rock stacks, each RISE blocks higher and several blocks away from the last; the
     * rise is beyond an Earth jump but within a Deimos jump at every realism setting, and the relay cache sits on top.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void deimosSpireNeedsLowGravityJumps(GameTestHelper helper) {
        var deimos = com.pizzaparker07.spacebreaker.core.body.Bodies.forDimension(access(helper), MarsModule.DEIMOS).orElseThrow(() -> new AssertionError("deimos body"));
        double real = deimos.physical().surfaceGravity() / com.pizzaparker07.spacebreaker.core.body.CelestialBody.EARTH_GRAVITY;
        int rise = com.pizzaparker07.spacebreaker.content.mars.dungeon.HopSpirePiece.RISE;
        helper.assertTrue(jumpPeak(1.0) < rise, "on Earth a stack is out of reach (peak " + jumpPeak(1.0) + ")");
        for (var realism : com.pizzaparker07.spacebreaker.core.config.SbConfig.Realism.values()) {
            double peak = jumpPeak(realism.scaleGravity(real));
            helper.assertTrue(peak > rise + 0.2, "on Deimos (" + realism + ") a stack is in reach, peak " + peak);
        }
        helper.assertTrue(access(helper).registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .containsKey(com.pizzaparker07.spacebreaker.content.mars.dungeon.MarsStructures.HOP_SPIRE_KEY), "spire structure");

        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(4000, 0, 4000);
        level.getChunk(base);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, base.getX(), base.getZ());
        var piece = new com.pizzaparker07.spacebreaker.content.mars.dungeon.HopSpirePiece(3L, base.getX(), y, base.getZ());
        place(level, piece);
        var stacks = piece.stacks();
        for (int k = 0; k < stacks.size(); k++) {
            BlockPos top = stacks.get(k);
            helper.assertTrue(!level.getBlockState(top.below()).isAir() && level.getBlockState(top).isAir(), "stack " + k + " stands at " + top + ": " + level.getBlockState(top.below()) + " / " + level.getBlockState(top));
            if (k > 0) {
                BlockPos prev = stacks.get(k - 1);
                helper.assertTrue(top.getY() - prev.getY() == rise, "each stack rises by " + rise);
                double gap = Math.hypot(top.getX() - prev.getX(), top.getZ() - prev.getZ()) - 2;
                helper.assertTrue(gap >= 3.5, "stacks stand apart (gap " + gap + ")");
            }
        }
        BlockPos summit = piece.summit();
        helper.assertTrue(summit.getY() - stacks.get(stacks.size() - 1).getY() == rise, "the summit is one hop above the last stack");
        helper.assertTrue(level.getBlockEntity(summit) instanceof com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlockEntity l
                && l.getLootTable() != null, "the relay cache (a supply locker) sits on the summit");
        helper.succeed();
    }

    /** The Tether Spinner: flung away from its silk anchor, the taut line stops it and reels it back. Fed chondrite. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void tetherSpinnerReelsBackOnItsLine(GameTestHelper helper) {
        var spinner = helper.spawn(com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.TETHER_SPINNER.get(), new BlockPos(1, 12, 1));
        spinner.setNoAi(true);
        spinner.setNoGravity(true);
        net.minecraft.world.phys.Vec3 start = spinner.position();
        double tether = com.pizzaparker07.spacebreaker.content.mars.entity.TetherSpinner.TETHER;
        spinner.anchorAt(start.add(-tether - 2, 0, 0));
        spinner.setDeltaMovement(0.8, 0, 0);
        helper.assertTrue(spinner.isFood(new net.minecraft.world.item.ItemStack(MarsModule.CARBONACEOUS_CHONDRITE.get())), "it eats chondrite");
        helper.runAfterDelay(20, () -> {
            double reach = spinner.position().distanceTo(spinner.anchor());
            helper.assertTrue(reach < tether + 2.5, "the line holds it near its anchor, at " + reach);
            helper.assertTrue(spinner.getDeltaMovement().x < 0, "the line reels it back, vx " + spinner.getDeltaMovement().x);
            helper.succeed();
        });
    }

    /** Martian dust: clean to fully covered in about three sols, six times faster in a full storm. */
    @GameTest(template = EMPTY)
    public static void panelDustSettlesOverSols(GameTestHelper helper) {
        double calm = com.pizzaparker07.spacebreaker.core.env.DustLoad.rate(0);
        helper.assertTrue(Math.abs(calm * 3 * 24000 - 1) < 1e-9, "three calm sols cover a panel");
        helper.assertTrue(Math.abs(com.pizzaparker07.spacebreaker.core.env.DustLoad.rate(1) / calm - 6) < 1e-9, "storms settle six times faster");
        helper.assertTrue(com.pizzaparker07.spacebreaker.core.env.DustLoad.percent(0.427F) == 43, "dust shown as a percentage");
        helper.succeed();
    }

    /** A dust whirl swirls loose items aside, and fades away when nobody is near to see it. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void dustWhirlNudgesThingsAndFadesUnseen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        var whirl = com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.spawnWhirl(level, at);
        helper.assertTrue(whirl != null, "a whirl forms");
        whirl.setHeading(0F);
        var item = new net.minecraft.world.entity.item.ItemEntity(level, whirl.getX() + 1.0, whirl.getY() + 0.2, whirl.getZ(),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
        item.setNoGravity(true);
        level.addFreshEntity(item);
        net.minecraft.world.phys.Vec3 start = item.position();
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(item.position().distanceTo(start) > 0.05, "the whirl swirls the item aside");
        });
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(whirl.isRemoved(), "with no player within sight the whirl fades away");
            helper.assertFalse(whirl.shouldBeSaved(), "whirls are weather: never saved");
            item.discard();
            helper.succeed();
        });
    }

    /** A passing dust whirl sweeps a dusty solar panel clean; a brush wipes another by hand. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void whirlAndBrushCleanRealPanels(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos near = new BlockPos(1, 2, 1);
        BlockPos far = new BlockPos(1, 2, 6);
        BlockPos brushed = new BlockPos(4, 2, 6);
        for (BlockPos p : new BlockPos[]{near, far, brushed}) {
            helper.setBlock(p, com.pizzaparker07.spacebreaker.content.machines.MachinesModule.SOLAR_PANEL.get());
            panel(helper, p).setDust(1F);
        }
        var whirl = com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.spawnWhirl(level, helper.absolutePos(new BlockPos(2, 1, 1)));
        helper.assertTrue(whirl != null, "a whirl forms");
        whirl.setHeading((float) Math.PI); // drifting slowly away from the far panel
        com.pizzaparker07.spacebreaker.content.mars.PanelDust.brush(level, helper.absolutePos(brushed), panel(helper, brushed));
        helper.assertTrue(panel(helper, brushed).dust() == 0F, "the brush wipes the panel");
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(panel(helper, near).dust() == 0F, "the whirl swept the panel it passed, dust " + panel(helper, near).dust());
            helper.assertTrue(panel(helper, far).dust() == 1F, "a panel out of its reach stays dusty");
            float covered = com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity.dustFactor(1F);
            helper.assertTrue(Math.abs(covered - 0.6F) < 1e-6, "a fully dusty panel gives 60 %, got " + covered);
            whirl.discard();
            helper.succeed();
        });
    }

    private static com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity panel(GameTestHelper helper, BlockPos rel) {
        if (helper.getBlockEntity(rel) instanceof com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity be) {
            return be;
        }
        throw new net.minecraft.gametest.framework.GameTestAssertException("no solar panel at " + rel);
    }

    /**
     * Seasons: in northern winter (Ls 270) the northern cap spreads to about 52°, while the south is at its summer edge;
     * night frost reaches farther. Frost settles on open ground, never under a roof, and sublimates in spring.
     */
    @GameTest(template = EMPTY)
    public static void seasonalFrostGrowsInWinterAndSublimates(GameTestHelper helper) {
        double winterEdge = com.pizzaparker07.spacebreaker.content.mars.MarsSeasons.capEdge(270, true);
        helper.assertTrue(Math.abs(winterEdge - 52) < 1e-6, "northern winter cap reaches 52°, got " + winterEdge);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.mars.MarsSeasons.capEdge(270, false) == 82, "southern summer: residual cap only");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.mars.MarsSeasons.capEdge(90, false) < 53, "southern winter cap spreads too");
        var S = new Object() {
            double cov(double lat, double ls, boolean night) {
                return com.pizzaparker07.spacebreaker.content.mars.MarsSeasons.coverage(lat, ls, night);
            }
        };
        helper.assertTrue(S.cov(75, 270, false) == 1, "deep in the winter cap the ground is fully frosted");
        double edge = S.cov(53, 270, false);
        helper.assertTrue(edge > 0.25 && edge < 0.5, "at the cap edge only patches, got " + edge);
        helper.assertTrue(S.cov(45, 270, false) == 0, "no daytime frost beyond the cap");
        double night = S.cov(45, 270, true);
        helper.assertTrue(night > 0 && night < edge, "a thin night scatter beyond the cap, got " + night);
        helper.assertTrue(S.cov(60, 90, true) == 0, "none in northern summer");
        helper.assertTrue(S.cov(-70, 90, false) > 0.9, "but on the southern winter cap");
        int frosted = 0;
        for (int i = 0; i < 4000; i++) {
            frosted += com.pizzaparker07.spacebreaker.content.mars.MarsSeasons.wantsFrost(i * 7, i * 13 % 997, 53, 270, false) ? 1 : 0;
        }
        helper.assertTrue(Math.abs(frosted / 4000.0 - edge) < 0.08, "patches cover about that share of the ground, got " + frosted / 4000.0);
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.mars.MarsSeasons.season(275) == 3, "Ls 275 is northern winter");

        ServerLevel level = helper.getLevel();
        // a column far from the test frame, open to the sky
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-4000, 0, -4100);
        level.getChunk(base);
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, base.getX(), base.getZ());
        BlockPos open = new BlockPos(base.getX(), top - 1, base.getZ());
        level.setBlockAndUpdate(open, Blocks.STONE.defaultBlockState());
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.mars.MarsSeasons.updateColumn(level, open.getX(), open.getZ(), true) == 1, "frost forms");
        helper.assertTrue(level.getBlockState(open.above()).is(MarsModule.CO2_FROST_LAYER.get()), "a thin frost layer on the ground");
        helper.assertTrue(level.getBlockState(open).is(Blocks.STONE), "the ground below is untouched");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.mars.MarsSeasons.updateColumn(level, open.getX(), open.getZ(), true) == 0, "no double layers");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.mars.MarsSeasons.updateColumn(level, open.getX(), open.getZ(), false) == -1, "it sublimates");
        helper.assertTrue(level.getBlockState(open.above()).isAir(), "gone without a trace");
        helper.succeed();
    }

    /** Mars' air pressure follows the caps through the year: lowest in southern winter, highest in southern summer. */
    @GameTest(template = EMPTY)
    public static void marsPressureBreathesWithTheSeasons(GameTestHelper helper) {
        double min = 2;
        double max = 0;
        double minLs = 0;
        double maxLs = 0;
        double sum = 0;
        for (int ls = 0; ls < 360; ls++) {
            double f = com.pizzaparker07.spacebreaker.content.mars.MarsSeasons.pressureFactor(ls);
            sum += f;
            if (f < min) {
                min = f;
                minLs = ls;
            }
            if (f > max) {
                max = f;
                maxLs = ls;
            }
        }
        helper.assertTrue(Math.abs(sum / 360 - 1) < 1e-3, "the yearly mean is the body's pressure");
        helper.assertTrue(min > 0.85 && min < 0.9 && minLs > 140 && minLs < 180, "lowest in southern winter: " + min + " at Ls " + minLs);
        helper.assertTrue(max > 1.1 && max < 1.15 && maxLs > 240 && maxLs < 290, "highest in southern summer: " + max + " at Ls " + maxLs);
        helper.succeed();
    }

    /**
     * Water-ice clouds: canyon fog thickest just after sunrise and gone by noon and at night, filling the floor and
     * thinning upward; the aphelion cloud belt over the equator around Ls 95, absent in southern summer.
     */
    @GameTest(template = EMPTY)
    public static void marsIceCloudsFollowTimeAndSeason(GameTestHelper helper) {
        var C = new Object() {
            double fog(double f) {
                return com.pizzaparker07.spacebreaker.content.mars.MarsClouds.canyonFog(f);
            }

            double belt(double ls, double lat) {
                return com.pizzaparker07.spacebreaker.content.mars.MarsClouds.cloudBelt(ls, lat);
            }
        };
        helper.assertTrue(C.fog(0.02) > 0.9, "thick fog just after sunrise, got " + C.fog(0.02));
        helper.assertTrue(C.fog(0.10) < C.fog(0.04), "it burns off through the morning");
        helper.assertTrue(C.fog(0.25) == 0 && C.fog(0.5) == 0 && C.fog(0.75) == 0, "none at noon, sunset or midnight");
        helper.assertTrue(C.fog(0.94) == 0 && C.fog(0.98) > 0, "it gathers in the last hour before sunrise");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.mars.MarsClouds.fogAtHeight(2) == 1, "the canyon floor is in the fog");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.mars.MarsClouds.fogAtHeight(40) == 0, "the rims stand above it");
        helper.assertTrue(C.belt(95, 0) > 0.99, "the aphelion belt over the equator");
        helper.assertTrue(C.belt(95, 60) < 0.5 * C.belt(95, 0), "thinner far from the equator");
        helper.assertTrue(C.belt(270, 0) == 0 && C.belt(200, 0) == 0, "no belt in southern summer");
        helper.succeed();
    }

    private MarsLifeGameTests() {
    }

    /** The supply locker holds its loot table until opened, then shows its doors open and fills from the table. */
    @GameTest(template = EMPTY)
    public static void supplyLockerFillsAndOpens(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLocker.BLOCK.get().defaultBlockState());
        BlockPos pos = helper.absolutePos(rel);
        var locker = (com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(locker != null, "the locker has its block entity");
        locker.setLootTable(com.pizzaparker07.spacebreaker.content.mars.dungeon.MarsLoot.STATION_STORAGE, 7L);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() - 1.5);
        locker.unpackLootTable(player);
        helper.assertTrue(locker.getLootTable() == null, "opening rolls the loot table once");
        helper.assertFalse(locker.isEmpty(), "the storage loot table put something in it");
        locker.startOpen(player);
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlock.OPEN),
                "its doors stand open while in use");
        locker.stopOpen(player);
        helper.assertFalse(helper.getLevel().getBlockState(pos).getValue(com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlock.OPEN),
                "and close again");
        helper.succeed();
    }
}
