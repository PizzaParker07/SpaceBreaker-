package com.pizzaparker07.spacebreaker.testmod.outer;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.block.VentBlock;
import com.pizzaparker07.spacebreaker.content.belt.block.VentBlockEntity;
import com.pizzaparker07.spacebreaker.content.belt.world.CellsFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.VentFeature;
import com.pizzaparker07.spacebreaker.content.neptune.NeptuneData;
import com.pizzaparker07.spacebreaker.content.neptune.NeptuneModule;
import com.pizzaparker07.spacebreaker.content.neptune.entity.NeptuneEntities;
import com.pizzaparker07.spacebreaker.content.neptune.entity.PlumeBeetle;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.testmod.mars.WorldTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NeptuneGameTests {
    @GameTest(template = WorldTests.EMPTY)
    public static void tritonIsACapturedRetrogradeMoon(GameTestHelper helper) {
        helper.assertTrue(WorldTests.stem(helper, "triton").generator() instanceof PlanetChunkGenerator, "Triton uses the planet engine");
        CelestialBody b = Bodies.forDimension(helper.getLevel().registryAccess(), NeptuneModule.TRITON)
                .orElseThrow(() -> new AssertionError("Triton has no body"));
        helper.assertTrue(b.orbit().inclinationDeg() > 90, "its orbit runs backwards: " + b.orbit().inclinationDeg());
        helper.assertTrue(b.climate().dayTempC() < -230, "the coldest surface measured");
        helper.assertTrue(WorldTests.profile(helper, "triton").landforms().iceCap().isPresent(), "Triton has its pink polar cap");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void tritonHasCantaloupeTerrainAndPolarGeysers(GameTestHelper helper) {
        PlanetTerrain t = WorldTests.generator(helper, "triton", 4L).terrain();
        CellsFeature.Config cells = NeptuneData.cantaloupe();
        double lo = 99;
        double hi = -99;
        for (int i = 0; i < 400; i++) {
            double x = (i * 37 % 600) - 300;
            double z = (i * 91 % 600) - 300;
            double o = CellsFeature.offset(cells, CellsFeature.sample(t, cells, x, z)) * CellsFeature.weight(t, cells, x, z);
            lo = Math.min(lo, o);
            hi = Math.max(hi, o);
        }
        helper.assertTrue(lo < -2.5 && hi > 0.8, "round pits with ridges between them: " + lo + ".." + hi);
        double pole = t.profile().poleDistance();
        helper.assertTrue(CellsFeature.weight(t, cells, 0, pole * 0.9) == 0, "no cantaloupe in the polar cap");
        VentFeature.Config g = NeptuneData.geysers();
        int south = 0;
        int north = 0;
        for (int gx = -6; gx <= 6; gx++) {
            for (int gz = -40; gz <= 40; gz++) {
                int[] v = VentFeature.vent(t, g, gx, gz);
                if (v != null) {
                    if (t.latitude(v[1]) < 0) {
                        south++;
                    } else {
                        north++;
                    }
                }
            }
        }
        helper.assertTrue(south > 3 && north == 0, "geysers in the southern cap only: " + south + " / " + north);
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 100)
    public static void nitrogenGeyserLaunchesAPlumeBeetleThatLandsSoftly(GameTestHelper helper) {
        BlockPos vent = new BlockPos(2, 1, 2);
        helper.setBlock(vent, NeptuneModule.NITROGEN_GEYSER.get());
        PlumeBeetle b = helper.spawn(NeptuneEntities.PLUME_BEETLE.get(), vent.above());
        helper.assertTrue(((VentBlock) NeptuneModule.NITROGEN_GEYSER.get()).kind().damage() == 0, "a cold geyser: it lifts but does not burn");
        VentBlockEntity.erupt(helper.getLevel(), helper.absolutePos(vent), ((VentBlock) NeptuneModule.NITROGEN_GEYSER.get()).kind());
        helper.assertTrue(b.getDeltaMovement().y > 0.3, "thrown up by the geyser");
        helper.assertFalse(b.causeFallDamage(30F, 1F, helper.getLevel().damageSources().fall()), "and it never takes fall damage");
        helper.assertTrue(b.isFood(new ItemStack(NeptuneModule.PLUME_DUST.get())), "breeds on plume dust");
        helper.assertTrue(new ItemStack(NeptuneModule.METHANE_CLATHRATE.get()).getBurnTime(RecipeType.SMELTING) == 1600, "clathrate burns like coal");
        // the plume rains dust around the geyser: a renewable food for the beetles, capped so it never piles up
        VentBlock geyser = (VentBlock) NeptuneModule.NITROGEN_GEYSER.get();
        int dropped = 0;
        for (int i = 0; i < 20; i++) {
            dropped += VentBlockEntity.fallout(helper.getLevel(), helper.absolutePos(vent), geyser) ? 1 : 0;
        }
        helper.assertTrue(dropped == VentBlockEntity.FALLOUT_CAP, "fallout stops at the cap: " + dropped);
        var dust = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(vent)).inflate(12, 8, 12), e -> e.getItem().is(NeptuneModule.PLUME_DUST.get().asItem()));
        helper.assertTrue(dust.size() == VentBlockEntity.FALLOUT_CAP, "plume dust lies around the geyser: " + dust.size());
        dust.forEach(net.minecraft.world.entity.Entity::discard);
        helper.assertTrue(((VentBlock) com.pizzaparker07.spacebreaker.content.jupiter.JupiterModule.ICE_GEYSER.get()).fallout() == null, "ice geysers rain nothing");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void cryovolcanismIsInTheTree(GameTestHelper helper) {
        var node = Tech.all(helper.getLevel().registryAccess()).get(Spacebreaker.id("cryovolcanism"));
        helper.assertTrue(node != null && node.tier() == 5, "cryovolcanism is a tier-5 tech");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void geyserStationStandsOverAGeyser(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .containsKey(com.pizzaparker07.spacebreaker.content.neptune.dungeon.NeptuneStructures.STATION_KEY), "station structure");
        BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-5000, 0, 9000);
        var piece = new com.pizzaparker07.spacebreaker.content.neptune.dungeon.GeyserStationPiece(5L, base.getX(), 110, base.getZ(),
                net.minecraft.core.Direction.EAST);
        BeltGameTests.place(level, piece);
        var bb = piece.getBoundingBox();
        int chests = 0;
        int glass = 0;
        for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ())) {
            var st = level.getBlockState(p);
            if (level.getBlockEntity(p) instanceof com.pizzaparker07.spacebreaker.content.dungeons.locker.SupplyLockerBlockEntity ch
                    && ch.getLootTable() != null) {
                chests++;
            }
            glass += st.is(net.minecraft.world.level.block.Blocks.LIGHT_BLUE_STAINED_GLASS) ? 1 : 0;
        }
        int c = com.pizzaparker07.spacebreaker.content.neptune.dungeon.GeyserStationPiece.C;
        BlockPos vent = piece.world(c, 2, c);
        helper.assertTrue(level.getBlockState(vent).is(NeptuneModule.NITROGEN_GEYSER.get()), "a live geyser in the middle of the floor");
        helper.assertTrue(level.getBlockEntity(vent) instanceof VentBlockEntity, "with its vent working");
        helper.assertTrue(chests == 2, "lab and store chests, found " + chests);
        helper.assertTrue(glass > 60, "a glass dome, found " + glass);
        helper.assertTrue(level.getBlockState(piece.world(c, 15, c + 2)).is(net.minecraft.world.level.block.Blocks.WAXED_COPPER_BULB),
                "the mast light above the crown");
        helper.succeed();
    }
}
