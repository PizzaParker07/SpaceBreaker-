package com.pizzaparker07.spacebreaker.testmod.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.venus.AcidCoatingRecipe;
import com.pizzaparker07.spacebreaker.content.venus.VenusClimate;
import com.pizzaparker07.spacebreaker.content.venus.VenusModule;
import com.pizzaparker07.spacebreaker.content.venus.dungeon.AerostatPiece;
import com.pizzaparker07.spacebreaker.content.venus.dungeon.VenusLoot;
import com.pizzaparker07.spacebreaker.content.venus.dungeon.VenusStructures;
import com.pizzaparker07.spacebreaker.content.venus.entity.AcidRay;
import com.pizzaparker07.spacebreaker.content.venus.entity.VenusEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VenusGameTests {
    @GameTest(template = WorldTests.EMPTY)
    public static void venusIsAFurnaceUnderBreathableClouds(GameTestHelper helper) {
        var stem = WorldTests.stem(helper, "venus");
        helper.assertTrue(stem.generator() instanceof PlanetChunkGenerator, "venus uses the planet engine");
        helper.assertTrue(stem.type().value().effectsLocation().equals(Spacebreaker.id("venus")), "venus sky effects");
        CelestialBody body = Bodies.forDimension(helper.getLevel().registryAccess(), VenusModule.VENUS)
                .orElseThrow(() -> new AssertionError("venus has no body"));
        helper.assertTrue(body.atmosphere().pressureKpa() > 9000, "92 bar at the surface");
        helper.assertTrue(body.climate().dayTempC() > 400, "a furnace at the surface");
        var haze = body.atmosphere().haze().orElseThrow(() -> new AssertionError("venus has no haze data"));
        helper.assertTrue(haze.visibilityBlocks() > 16 && haze.visibilityBlocks() < 128, "a thick surface haze, got " + haze.visibilityBlocks());
        helper.assertTrue(haze.colour() != haze.highColour(), "dark orange haze below, pale cloud colour above");
        // the altitude model: 1 bar and room temperature somewhere in the cloud deck
        double surfaceP = body.atmosphere().pressureKpa();
        double cloudP = surfaceP * Math.exp(-(220 - VenusClimate.SURFACE) / VenusClimate.SCALE_HEIGHT);
        double cloudT = body.climate().dayTempC() - VenusClimate.LAPSE * (220 - VenusClimate.SURFACE);
        helper.assertTrue(cloudP > 50 && cloudP < 200, "about 1 bar in the clouds, got " + cloudP + " kPa");
        helper.assertTrue(cloudT > 0 && cloudT < 50, "about room temperature in the clouds, got " + cloudT + " C");
        helper.assertTrue(VenusClimate.sunlight(VenusClimate.SURFACE) < 0.2 && VenusClimate.sunlight(VenusClimate.CLOUD_TOP + 5) == 1,
                "dim at the surface, full sun above the clouds");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 300)
    public static void venusTerrainHasTesseraeVolcanoesAndFrost(GameTestHelper helper) {
        TerrainProfile p = WorldTests.profile(helper, "venus");
        PlanetTerrain t = new PlanetTerrain(p, 99L);
        int tessera = 0;
        int volcano = 0;
        double max = -1e9;
        for (int i = 0; i < 4000; i++) {
            double x = (i * 7919L) % 16000 - 8000;
            double z = (i * 104729L) % 16000 - 8000;
            PlanetTerrain.Column c = t.column(x, z);
            tessera += c.tessera > 0.5 ? 1 : 0;
            volcano += c.volcanoS < 0.9 ? 1 : 0;
            max = Math.max(max, c.height);
        }
        helper.assertTrue(tessera > 40, "tesserae too rare: " + tessera);
        helper.assertTrue(volcano > 20, "volcanoes too rare: " + volcano);
        helper.assertTrue(max < VenusClimate.CLOUD_BASE - 40, "the surface stays far below the clouds, max " + max);
        WorldTests.dumpMap(t, "venus", 0, 0, 6000, 500);
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void acidCoatingProtectsArmour(GameTestHelper helper) {
        AcidCoatingRecipe recipe = new AcidCoatingRecipe(CraftingBookCategory.EQUIPMENT);
        CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.IRON_HELMET), new ItemStack(VenusModule.ACID_COATING.get())));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "armour + coating matches");
        ItemStack out = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(out.is(Items.IRON_HELMET) && Boolean.TRUE.equals(out.get(VenusModule.ACID_COATED.get())), "coated helmet");
        CraftingInput twice = CraftingInput.of(2, 1, List.of(out, new ItemStack(VenusModule.ACID_COATING.get())));
        helper.assertFalse(recipe.matches(twice, helper.getLevel()), "no double coating");
        CraftingInput wrong = CraftingInput.of(2, 1, List.of(new ItemStack(Items.STICK), new ItemStack(VenusModule.ACID_COATING.get())));
        helper.assertFalse(recipe.matches(wrong, helper.getLevel()), "only armour can be coated");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 300)
    public static void aerostatFloatsWithLootAndGuards(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(VenusStructures.AEROSTAT_KEY), "aerostat structure");
        for (var k : List.of(VenusLoot.AEROSTAT_LAB, VenusLoot.AEROSTAT_STORES)) {
            helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(k) != LootTable.EMPTY, "loot " + k.location());
        }
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(5000, 0, -5000);
        AerostatPiece piece = new AerostatPiece(3L, base.getX(), 200, base.getZ(), Direction.NORTH);
        BoundingBox bb = piece.getBoundingBox();
        // keep the area loaded (entity sections too) for the whole test, so the guards are always visible to queries
        List<ChunkPos> forced = new java.util.ArrayList<>();
        for (int cx = (bb.minX() >> 4) - 1; cx <= (bb.maxX() >> 4) + 1; cx++) {
            for (int cz = (bb.minZ() >> 4) - 1; cz <= (bb.maxZ() >> 4) + 1; cz++) {
                level.setChunkForced(cx, cz, true);
                forced.add(new ChunkPos(cx, cz));
            }
        }
        // build only once the forced tickets have made the chunks (and their entity sections) fully loaded, so the
        // guards are added to a live area and never lost to load order
        java.util.concurrent.atomic.AtomicBoolean built = new java.util.concurrent.atomic.AtomicBoolean();
        helper.runAfterDelay(10, () -> {
            for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
                for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                    level.getChunk(cx, cz);
                    piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(cx),
                            new BoundingBox(cx << 4, -64, cz << 4, (cx << 4) + 15, 319, (cz << 4) + 15), new ChunkPos(cx, cz), bb.getCenter());
                }
            }
            int chests = 0;
            int fabric = 0;
            for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
                var st = level.getBlockState(p);
                if (level.getBlockEntity(p) instanceof com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlockEntity c
                        && c.getLootTable() != null) {
                    chests++;
                }
                if (st.is(VenusModule.ENVELOPE_FABRIC.get())) {
                    fabric++;
                }
            }
            helper.assertTrue(chests >= 2, "aerostat lab and stores lockers, found " + chests);
            helper.assertTrue(fabric > 200, "a big envelope, found " + fabric);
            // valve puzzle: of the three levers above the gondola door exactly one opens it
            List<BlockPos> levers = new java.util.ArrayList<>();
            List<BlockPos> doors = new java.util.ArrayList<>();
            int webs = 0;
            int trapdoors = 0;
            var airlock = com.pizzaparker07.spacebreaker.content.suit.SuitModule.AIRLOCK_DOOR.get();
            var open = com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.OPEN;
            for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
                var st = level.getBlockState(p);
                if (st.is(Blocks.LEVER)) {
                    levers.add(p.immutable());
                }
                if (st.is(airlock) && st.getValue(com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock.HALF)
                        == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
                    doors.add(p.immutable());
                }
                webs += st.is(Blocks.COBWEB) ? 1 : 0;
                trapdoors += st.is(Blocks.IRON_TRAPDOOR) ? 1 : 0;
            }
            helper.assertTrue(levers.size() == 3 && !doors.isEmpty(), "three valves and a sealed door, levers " + levers.size());
            helper.assertTrue(webs >= 1 && trapdoors >= 1, "walkway trap with a net");
            helper.assertTrue(doors.stream().noneMatch(d -> level.getBlockState(d).getValue(open)), "doors start closed");
            int opening = 0;
            for (BlockPos l : levers) {
                var lever = (net.minecraft.world.level.block.LeverBlock) Blocks.LEVER;
                lever.pull(level.getBlockState(l), level, l, null);
                if (doors.stream().anyMatch(d -> level.getBlockState(d).getValue(open))) {
                    opening++;
                }
                lever.pull(level.getBlockState(l), level, l, null);
            }
            helper.assertTrue(opening == 1, "exactly one valve opens the door, got " + opening);
            built.set(true);
        });
        var area = net.minecraft.world.phys.AABB.of(bb).inflate(48);
        helper.succeedWhen(() -> {
            helper.assertTrue(built.get(), "aerostat not built yet");
            var rays = level.getEntitiesOfClass(AcidRay.class, area);
            helper.assertTrue(rays.size() >= 2 && rays.stream().allMatch(AcidRay::isGuardian), "acid ray guardians, found " + rays.size());
            rays.forEach(net.minecraft.world.entity.Entity::discard);
            forced.forEach(c -> level.setChunkForced(c.x, c.z, false));
        });
    }

    /** The aerostat kit inflates a floating base: a full deck with railings under a fabric envelope, only in the cloud band. */
    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 100)
    public static void aerostatKitInflatesAFloatingBase(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertFalse(com.pizzaparker07.spacebreaker.content.venus.AerostatKitItem.inLiftBand(level, 220), "not on Earth");
        BlockPos deck = helper.absolutePos(BlockPos.ZERO).offset(-3000, 0, 3000).atY(220);
        level.getChunk(deck);
        int placed = com.pizzaparker07.spacebreaker.content.venus.AerostatKitItem.deploy(level, deck);
        int panels = 0;
        int fabric = 0;
        for (BlockPos p : BlockPos.betweenClosed(deck.offset(-6, 0, -6), deck.offset(6, 13, 6))) {
            panels += level.getBlockState(p).is(VenusModule.COMPOSITE_PANEL.get()) ? 1 : 0;
            fabric += level.getBlockState(p).is(VenusModule.ENVELOPE_FABRIC.get()) ? 1 : 0;
        }
        helper.assertTrue(panels == 81, "a 9x9 deck, got " + panels);
        helper.assertTrue(fabric > 60, "a lift envelope, got " + fabric);
        helper.assertTrue(level.getBlockState(deck.offset(0, 1, 4)).isAir(), "a gate in the railing");
        helper.assertTrue(com.pizzaparker07.spacebreaker.content.venus.AerostatKitItem.deploy(level, deck) == 0, "a second kit does not overwrite anything");
        helper.assertTrue(placed > 150, "placed " + placed);
        helper.succeed();
    }

    /** A descent probe sinks to the ground, lands and sends its owner Venus samples and landing data. */
    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void descentProbeSendsSurfaceSamples(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        com.pizzaparker07.spacebreaker.content.venus.entity.DescentProbe.deliverTo(player);
        int samples = 0;
        int data = 0;
        for (var stack : player.getInventory().items) {
            var kind = stack.get(com.pizzaparker07.spacebreaker.content.research.ResearchModule.SAMPLE.get());
            if (kind != null && kind.getPath().equals("venus")) {
                samples += stack.getCount();
            }
            if (kind != null && kind.getPath().equals("probe_landing")) {
                data += stack.getCount();
            }
        }
        helper.assertTrue(samples == 2 && data == 1, "two Venus samples and landing data, got " + samples + " / " + data);
        var probe = com.pizzaparker07.spacebreaker.content.venus.entity.DescentProbe.release(helper.getLevel(),
                net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 8, 1))), player.getUUID());
        helper.getLevel().addFreshEntity(probe);
        helper.succeedWhen(() -> {
            helper.assertTrue(probe.landed(), "the probe is still descending");
            helper.assertTrue(probe.getY() < helper.absolutePos(new BlockPos(1, 4, 1)).getY(), "it sank to the ground");
            helper.succeed();
        });
    }

    private VenusGameTests() {
    }
}
