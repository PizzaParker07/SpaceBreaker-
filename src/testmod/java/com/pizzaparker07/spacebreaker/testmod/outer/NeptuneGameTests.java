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
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void cryovolcanismIsInTheTree(GameTestHelper helper) {
        var node = Tech.all(helper.getLevel().registryAccess()).get(Spacebreaker.id("cryovolcanism"));
        helper.assertTrue(node != null && node.tier() == 5, "cryovolcanism is a tier-5 tech");
        helper.succeed();
    }
}
