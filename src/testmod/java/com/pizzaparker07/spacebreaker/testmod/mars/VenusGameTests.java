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

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void aerostatFloatsWithLootAndGuards(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(VenusStructures.AEROSTAT_KEY), "aerostat structure");
        for (var k : List.of(VenusLoot.AEROSTAT_LAB, VenusLoot.AEROSTAT_STORES)) {
            helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(k) != LootTable.EMPTY, "loot " + k.location());
        }
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(5000, 0, -5000);
        AerostatPiece piece = new AerostatPiece(3L, base.getX(), 200, base.getZ(), Direction.NORTH);
        BoundingBox bb = piece.getBoundingBox();
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
            if (st.is(Blocks.CHEST) && level.getBlockEntity(p) instanceof ChestBlockEntity c && c.getLootTable() != null) {
                chests++;
            }
            if (st.is(VenusModule.ENVELOPE_FABRIC.get())) {
                fabric++;
            }
        }
        helper.assertTrue(chests >= 2, "aerostat lab and stores, found " + chests);
        helper.assertTrue(fabric > 200, "a big envelope, found " + fabric);
        var rays = level.getEntitiesOfClass(AcidRay.class, net.minecraft.world.phys.AABB.of(bb).inflate(4));
        helper.assertTrue(rays.size() >= 2 && rays.stream().allMatch(AcidRay::isGuardian), "acid ray guardians, found " + rays.size());
        helper.succeed();
    }

    private VenusGameTests() {
    }
}
