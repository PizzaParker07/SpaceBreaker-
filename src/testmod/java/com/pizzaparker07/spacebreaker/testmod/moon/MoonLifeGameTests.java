package com.pizzaparker07.spacebreaker.testmod.moon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.moon.dungeon.LavaTubePiece;
import com.pizzaparker07.spacebreaker.content.moon.dungeon.MoonLoot;
import com.pizzaparker07.spacebreaker.content.moon.dungeon.MoonStructures;
import com.pizzaparker07.spacebreaker.content.moon.dungeon.OutpostPiece;
import com.pizzaparker07.spacebreaker.content.moon.entity.DustSwimmer;
import com.pizzaparker07.spacebreaker.content.moon.entity.HeatSiphon;
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

    /**
     * The Moon's rays are laid in Fresh Regolith, filled solid toward the crater; every other world keeps the speckled
     * rays it always had (ray fill 1), so the setting changed nothing outside the Moon.
     */
    @GameTest(template = EMPTY)
    public static void moonRaysAreFreshRegolithOtherWorldsUnchanged(GameTestHelper helper) {
        var profiles = helper.getLevel().registryAccess().registryOrThrow(com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile.REGISTRY);
        var moon = profiles.getOptional(Spacebreaker.id("moon")).orElseThrow(() -> new AssertionError("moon terrain profile"));
        helper.assertTrue(moon.materials().raySoil().is(MoonModule.FRESH_REGOLITH.get()), "Moon rays are fresh regolith");
        helper.assertTrue(moon.craters().rayFill() > 1, "Moon rays are filled toward the crater, fill " + moon.craters().rayFill());
        helper.assertTrue(MoonModule.FRESH_REGOLITH.get().defaultBlockState().is(MoonModule.LUNAR_SOILS), "fresh regolith is a lunar soil");
        int others = 0;
        for (var e : profiles.entrySet()) {
            if (!e.getKey().location().equals(Spacebreaker.id("moon"))) {
                helper.assertTrue(e.getValue().craters().rayFill() == 1.0, e.getKey().location() + " keeps speckled rays");
                helper.assertTrue(e.getValue().craters().rayMinRadius() == 0.0, e.getKey().location() + " keeps rays from every fresh crater");
                others++;
            }
        }
        helper.assertTrue(others > 5, "other terrain profiles checked: " + others);
        helper.succeed();
    }

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
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(helper.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        var disks = server.reloadableRegistries().getLootTable(MoonLoot.TECH_DATA).getRandomItems(params);
        // Every roll yields a tech disk; the second pool may add field samples or experiment data.
        helper.assertTrue(disks.stream().anyMatch(st -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem())
                .equals(Spacebreaker.id("tech_disk"))), "Moon tech data must roll a tech disk, got " + disks);
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void outpostHasAirlockLootAndGuards(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // far from the other tests, on the flat test world; ground height read only once the chunk is loaded
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(4000, 0, 4000);
        OutpostPiece piece = new OutpostPiece(99L, base.getX(), surface(level, base), base.getZ(), Direction.SOUTH);
        BoundingBox bb = piece.getBoundingBox();
        build(level, piece);
        int chests = 0;
        int spawners = 0;
        int doors = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            var state = level.getBlockState(p);
            if (level.getBlockEntity(p) instanceof com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlockEntity chest
                    && chest.getLootTable() != null) {
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
        helper.assertTrue(doors == 6, "airlock with two iron doors plus the vault door (6 halves), found " + doors);
        // drones placed into a freshly loaded chunk can take a few ticks to show up in entity lookups
        helper.succeedWhen(() -> {
            int drones = level.getEntitiesOfClass(SurveyDrone.class, AABB.of(bb).inflate(4)).size();
            helper.assertTrue(drones >= 2, "drones guard the outpost, found " + drones);
        });
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

    @GameTest(template = EMPTY)
    public static void glimmerBeetleShedsShellsWhenBrushed(GameTestHelper helper) {
        var beetle = helper.spawn(MoonEntities.GLIMMER_BEETLE.get(), 2, 2, 2);
        helper.assertTrue(beetle.hasShell(), "a new beetle has a full shell");
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BRUSH));
        var first = beetle.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(first.consumesAction(), "brushing works");
        helper.assertFalse(beetle.hasShell(), "the shell regrows after brushing");
        helper.assertTrue(beetle.glow() < 1F, "a beetle without its shell glows dimmer");
        var shells = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new AABB(beetle.blockPosition()).inflate(2), e -> e.getItem().is(MoonModule.GLIMMER_SHELL.get()));
        helper.assertTrue(shells.size() == 1, "one glimmer shell dropped");
        helper.assertFalse(beetle.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(), "no second shell right away");
        helper.assertTrue(beetle.isAlive() && beetle.getHealth() == beetle.getMaxHealth(), "brushing does not hurt the beetle");
        helper.setBlock(1, 1, 1, MoonModule.GLIMMER_LAMP.get());
        helper.assertTrue(helper.getBlockState(new BlockPos(1, 1, 1)).getLightEmission() >= 14, "glimmer lamps light the lunar night without power");
        helper.succeed();
    }

    /**
     * The vault puzzle in all four piece orientations: the sign hangs on its wall, the arrow trap aims across the
     * pressure plate, and the door stays shut until a power cell (block of redstone) fills the socket above it.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void outpostVaultOpensWithPowerCell(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        java.util.List<BlockPos> doors = new java.util.ArrayList<>();
        Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (int i = 0; i < dirs.length; i++) {
            BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-4000 - i * 64, 0, 4000);
            OutpostPiece piece = new OutpostPiece(98L, base.getX(), surface(level, base), base.getZ(), dirs[i]);
            BoundingBox bb = piece.getBoundingBox();
            build(level, piece);
            String o = " (" + dirs[i] + ")";
            // local (22, F+1, 5) with F = 4
            BlockPos door = piece.localToWorld(22, 5, 5);
            BlockPos socket = piece.localToWorld(22, 7, 5);
            BlockPos signPos = piece.localToWorld(21, 6, 5);
            BlockPos plate = piece.localToWorld(21, 5, 5);
            BlockPos dispPos = piece.localToWorld(21, 5, 4);
            helper.assertTrue(level.getBlockState(door).is(Blocks.IRON_DOOR), "vault door in place" + o);
            helper.assertFalse(level.getBlockState(door).getValue(net.minecraft.world.level.block.DoorBlock.OPEN), "vault starts sealed" + o);
            helper.assertTrue(level.getBlockState(socket).isAir(), "an empty power socket above the door" + o);
            var sign = level.getBlockState(signPos);
            helper.assertTrue(sign.getBlock() instanceof net.minecraft.world.level.block.WallSignBlock && sign.canSurvive(level, signPos),
                    "warning sign hangs on the tunnel wall" + o);
            helper.assertTrue(level.getBlockEntity(signPos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity,
                    "warning sign has its text" + o);
            var disp = level.getBlockState(dispPos);
            helper.assertTrue(disp.is(Blocks.DISPENSER), "dispenser in the tunnel wall" + o);
            helper.assertTrue(dispPos.relative(disp.getValue(net.minecraft.world.level.block.DispenserBlock.FACING)).equals(plate),
                    "the trap aims across the pressure plate" + o);
            helper.assertTrue(level.getBlockEntity(dispPos) instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity d && !d.isEmpty(),
                    "armed arrow trap" + o);
            // every attached block (buttons, signs, lanterns, doors, plates) survives in this orientation
            for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
                var st = level.getBlockState(p);
                if (st.getBlock() instanceof net.minecraft.world.level.block.ButtonBlock || st.getBlock() instanceof net.minecraft.world.level.block.SignBlock
                        || st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock || st.getBlock() instanceof net.minecraft.world.level.block.LanternBlock
                        || st.getBlock() instanceof net.minecraft.world.level.block.BasePressurePlateBlock) {
                    helper.assertTrue(st.canSurvive(level, p), st.getBlock().getName().getString() + " is supported at " + p.immutable() + o);
                }
            }
            level.setBlock(socket, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            doors.add(door);
        }
        helper.runAfterDelay(2, () -> {
            for (BlockPos door : doors) {
                helper.assertTrue(level.getBlockState(door).getValue(net.minecraft.world.level.block.DoorBlock.OPEN), "the power cell opens the vault at " + door);
            }
            helper.succeed();
        });
    }

    /**
     * Every soil that keeps boot prints also takes rover wheel tracks: the print property offers "track" and the
     * blockstate file has a model for it in every facing.
     */
    @GameTest(template = EMPTY)
    public static void soilsTakeWheelTracks(GameTestHelper helper) {
        int soils = 0;
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (!(block instanceof com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock)) {
                continue;
            }
            soils++;
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            var state = block.defaultBlockState().setValue(com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock.PRINT,
                    com.pizzaparker07.spacebreaker.content.moon.Footprint.TRACK);
            String path = "/assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json";
            try (var in = MoonLifeGameTests.class.getResourceAsStream(path)) {
                helper.assertTrue(in != null, "blockstate file for " + id);
                String json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    helper.assertTrue(json.contains("facing=" + d.getSerializedName() + ",print=track"), "wheel track model for " + id + " facing " + d);
                }
            } catch (java.io.IOException e) {
                helper.fail("cannot read " + path + ": " + e);
            }
            helper.setBlock(1, 1, 1, state);
            helper.assertBlockProperty(new BlockPos(1, 1, 1), com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock.PRINT,
                    com.pizzaparker07.spacebreaker.content.moon.Footprint.TRACK);
        }
        helper.assertTrue(soils >= 4, "print soils found: " + soils);
        helper.succeed();
    }

    /**
     * Lunar night: a heat siphon walks to a charged battery bank and drinks from it, glowing brighter as it fills,
     * and its touch chills a player (vanilla freezing).
     */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void heatSiphonDrainsBatteries(GameTestHelper helper) {
        // a roof keeps the frost creature out of the sun
        for (int x = -1; x <= 6; x++) {
            for (int z = -1; z <= 2; z++) {
                helper.setBlock(x, 4, z, Blocks.STONE);
            }
        }
        BlockPos batteryPos = new BlockPos(5, 1, 0);
        helper.setBlock(batteryPos, com.pizzaparker07.spacebreaker.content.machines.MachinesModule.BATTERY_BANK.block().get());
        var battery = (com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity) helper.getBlockEntity(batteryPos);
        battery.energyCapability(null).receiveEnergy(200_000, false);
        int before = battery.energyCapability(null).getEnergyStored();
        HeatSiphon siphon = helper.spawn(MoonEntities.HEAT_SIPHON.get(), new BlockPos(0, 1, 0));
        helper.assertTrue(siphon.glow() < 0.4F, "a hungry siphon glows faintly");
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        int frozen = player.getTicksFrozen();
        siphon.doHurtTarget(player);
        helper.assertTrue(player.getTicksFrozen() >= frozen + HeatSiphon.CHILL, "its touch chills the player");
        helper.succeedWhen(() -> {
            helper.assertTrue(siphon.isAlive(), "the siphon is alive");
            helper.assertTrue(siphon.fed() >= 400, "the siphon drank from the battery: " + siphon.fed());
            helper.assertTrue(battery.energyCapability(null).getEnergyStored() <= before - 400, "the battery lost charge");
            helper.assertTrue(siphon.glow() > 0.3F, "glows brighter as it fills");
        });
    }

    /** The heat siphon spawns in every Moon biome (written into the biome files by datagen) and has a loot table. */
    @GameTest(template = EMPTY)
    public static void heatSiphonSpawnsOnTheMoon(GameTestHelper helper) {
        for (String biome : List.of("moon_highlands", "moon_mare", "moon_crater", "moon_volcanic", "moon_polar")) {
            String path = "/data/spacebreaker/worldgen/biome/" + biome + ".json";
            try (var in = MoonLifeGameTests.class.getResourceAsStream(path)) {
                helper.assertTrue(in != null, "biome file " + path);
                helper.assertTrue(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("spacebreaker:heat_siphon"),
                        "heat siphons spawn in " + biome);
            } catch (java.io.IOException e) {
                helper.fail("cannot read " + path + ": " + e);
            }
        }
        var key = ResourceKey.create(Registries.LOOT_TABLE, Spacebreaker.id("entities/heat_siphon"));
        helper.assertTrue(helper.getLevel().getServer().reloadableRegistries().getLootTable(key) != LootTable.EMPTY, "heat siphon loot table");
        helper.succeed();
    }

    /**
     * The polar ice-drilling station in all four orientations: running RTG with its pellet, station data chest,
     * core racks, the drill string down the bore hole, supported fittings and its heat siphon guards.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void iceDrillStationIsComplete(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        List<BoundingBox> boxes = new java.util.ArrayList<>();
        for (int i = 0; i < dirs.length; i++) {
            BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(3000 + i * 48, 0, -3000);
            var piece = new com.pizzaparker07.spacebreaker.content.moon.dungeon.IceDrillPiece(7L + i, base.getX(), surface(level, base), base.getZ(), dirs[i]);
            BoundingBox bb = piece.getBoundingBox();
            build(level, piece);
            String o = " (" + dirs[i] + ")";
            BlockPos rtg = piece.localToWorld(1, 4, 1);
            helper.assertTrue(level.getBlockEntity(rtg) instanceof com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity be
                    && be.items().getStackInSlot(0).is(com.pizzaparker07.spacebreaker.content.machines.MachinesModule.RTG_FUEL_PELLET.get()),
                    "the RTG still holds its pellet" + o);
            helper.assertTrue(level.getBlockEntity(piece.localToWorld(2, 4, 1)) instanceof com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlockEntity chest
                    && MoonLoot.ICE_DRILL_CONTROL.equals(chest.getLootTable()), "station data chest" + o);
            int racks = 0;
            for (int z = 9; z <= 10; z++) {
                if (level.getBlockEntity(piece.localToWorld(12, 4, z)) instanceof com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlockEntity barrel
                        && MoonLoot.ICE_DRILL_CORES.equals(barrel.getLootTable())) {
                    racks++;
                }
            }
            helper.assertTrue(racks == 2, "two ice core racks" + o);
            for (int yy = 0; yy <= 10; yy++) {
                helper.assertTrue(level.getBlockState(piece.localToWorld(9, yy, 9)).is(Blocks.CHAIN), "drill string at local y " + yy + o + ": " + level.getBlockState(piece.localToWorld(9, yy, 9)) + " at " + piece.localToWorld(9, yy, 9) + " bb " + bb);
            }
            for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
                var st = level.getBlockState(p);
                if (st.getBlock() instanceof net.minecraft.world.level.block.LanternBlock || st.is(Blocks.LIGHTNING_ROD)) {
                    helper.assertTrue(st.canSurvive(level, p), st.getBlock().getName().getString() + " is supported at " + p.immutable() + o);
                }
            }
            boxes.add(bb);
        }
        // the guards appear a few ticks after the build; in this sunny test world they then slowly sublimate, so the
        // check is on the most seen at once, not on the count at one moment
        int[] most = new int[boxes.size()];
        helper.onEachTick(() -> {
            for (int i = 0; i < boxes.size(); i++) {
                most[i] = Math.max(most[i], level.getEntitiesOfClass(HeatSiphon.class, AABB.of(boxes.get(i)).inflate(4)).size());
            }
        });
        helper.succeedWhen(() -> {
            for (int i = 0; i < boxes.size(); i++) {
                helper.assertTrue(most[i] >= 2, "heat siphons haunt the station, most seen " + most[i]);
            }
        });
    }

    /**
     * Micrometeorites: a hit costs half a heart and wears one worn suit piece; a player in the open is exposed, one under
     * a roof is not; the damage type exists; showers only fall on airless planet surfaces (not on Earth).
     */
    @GameTest(template = EMPTY)
    public static void micrometeoritesHitTheExposed(GameTestHelper helper) {
        var mc = com.pizzaparker07.spacebreaker.content.worldgen.MeteorShowers.class;
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .containsKey(com.pizzaparker07.spacebreaker.content.worldgen.MeteorShowers.MICROMETEORITE), "micrometeorite damage type");
        helper.assertFalse(com.pizzaparker07.spacebreaker.content.worldgen.MeteorShowers.eligible(helper.getLevel()), "no showers under Earth's air");
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos open = helper.absolutePos(new BlockPos(1, 1, 1));
        player.moveTo(open.getX() + 0.5, open.getY(), open.getZ() + 0.5);
        var plate = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, plate);
        float health = player.getHealth();
        player.invulnerableTime = 0;
        com.pizzaparker07.spacebreaker.content.worldgen.MeteorShowers.strike(player, RandomSource.create(1));
        helper.assertTrue(player.getHealth() <= health - com.pizzaparker07.spacebreaker.content.worldgen.MeteorShowers.DAMAGE + 0.01F,
                "a hit costs health: " + health + " -> " + player.getHealth());
        helper.assertTrue(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getDamageValue()
                >= com.pizzaparker07.spacebreaker.content.worldgen.MeteorShowers.WEAR, "a hit wears the armour");
        // a shower is capped: even unlucky, it can take at most a few hearts
        float worst = com.pizzaparker07.spacebreaker.content.worldgen.MeteorShowers.MAX_PER_SHOWER;
        helper.assertTrue(worst <= 6F, "at most three hearts per shower");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.worldgen.MeteorShowers.SUITED_DAMAGE
                < com.pizzaparker07.spacebreaker.content.worldgen.MeteorShowers.DAMAGE, "a sealed suit softens the hits");
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        boolean skyOpen = helper.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, eye.getX(), eye.getZ()) <= eye.getY();
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.worldgen.MeteorShowers.exposed(player) == skyOpen, "in the open you are exposed");
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 2; z++) {
                helper.setBlock(x, 4, z, Blocks.STONE);
            }
        }
        helper.assertFalse(com.pizzaparker07.spacebreaker.content.worldgen.MeteorShowers.exposed(player), "a roof overhead keeps you safe");
        helper.succeed();
    }

    /** Micrometeorites wear an EVA suit piece through the suit's wear model, but never past 75 %: a shower never makes it leak. */
    @GameTest(template = EMPTY)
    public static void micrometeoritesNeverHoleASuit(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.item.ItemStack suit = null;
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem s && s.getEquipmentSlot()
                    == net.minecraft.world.entity.EquipmentSlot.CHEST) {
                suit = new net.minecraft.world.item.ItemStack(item);
                break;
            }
        }
        helper.assertTrue(suit != null && suit.isDamageableItem(), "an EVA suit torso to test with");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, suit);
        var rnd = RandomSource.create(4);
        for (int i = 0; i < 2000; i++) {
            player.setHealth(player.getMaxHealth());
            player.invulnerableTime = 0;
            com.pizzaparker07.spacebreaker.content.worldgen.MeteorShowers.strike(player, rnd);
        }
        var worn = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        helper.assertTrue(!worn.isEmpty() && worn.getDamageValue() > 0, "the suit took wear: " + worn);
        helper.assertTrue(worn.getDamageValue() <= Math.floor(worn.getMaxDamage() * com.pizzaparker07.spacebreaker.content.worldgen.MeteorShowers.SUIT_WEAR_LIMIT),
                "never past 75 %: " + worn.getDamageValue() + " / " + worn.getMaxDamage());
        helper.succeed();
    }

    /** Ground height at a far-away test spot, with its chunk loaded first (an unloaded chunk reads as the world floor). */
    private static int surface(ServerLevel level, BlockPos pos) {
        level.getChunk(pos);
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
    }

    /**
     * Builds a structure piece in the live test level like world generation: every chunk force-loaded first (so the
     * guards placed in it stay loaded and ticking), then the piece chunk by chunk.
     */
    private static void build(ServerLevel level, net.minecraft.world.level.levelgen.structure.StructurePiece piece) {
        BoundingBox bb = piece.getBoundingBox();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
            for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
            }
        }
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
            for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                BoundingBox cb = new BoundingBox(cx << 4, level.getMinBuildHeight(), cz << 4, (cx << 4) + 15, level.getMaxBuildHeight(), (cz << 4) + 15);
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(cx * 31L + cz),
                        cb, new ChunkPos(cx, cz), bb.getCenter());
            }
        }
    }

    private MoonLifeGameTests() {
    }
}
