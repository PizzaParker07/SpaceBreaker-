package com.pizzaparker07.spacebreaker.testmod.outer;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.CellsFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.Columns;
import com.pizzaparker07.spacebreaker.content.pluto.PlutoData;
import com.pizzaparker07.spacebreaker.content.pluto.PlutoModule;
import com.pizzaparker07.spacebreaker.content.pluto.entity.FrostTortoise;
import com.pizzaparker07.spacebreaker.content.pluto.entity.NitrogenSlider;
import com.pizzaparker07.spacebreaker.content.pluto.entity.PlutoEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.CelestialBody;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.testmod.mars.WorldTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlutoGameTests {
    @GameTest(template = WorldTests.EMPTY)
    public static void plutoAndCharonAreColdDistantWorlds(GameTestHelper helper) {
        for (String w : new String[]{"pluto", "charon"}) {
            helper.assertTrue(WorldTests.stem(helper, w).generator() instanceof PlanetChunkGenerator, w + " uses the planet engine");
            CelestialBody b = Bodies.forDimension(helper.getLevel().registryAccess(), w.equals("pluto") ? PlutoModule.PLUTO : PlutoModule.CHARON)
                    .orElseThrow(() -> new AssertionError(w + " has no body"));
            helper.assertTrue(b.physical().surfaceGravity() < 1.0, w + " has a weak gravity (m/s²): " + b.physical().surfaceGravity());
            helper.assertTrue(b.climate().dayTempC() < -200, w + " is colder than -200 °C");
        }
        TerrainProfile pluto = WorldTests.profile(helper, "pluto");
        TerrainProfile charon = WorldTests.profile(helper, "charon");
        helper.assertTrue(pluto.poleDistance() > charon.poleDistance(), "Pluto is the bigger world");
        helper.assertTrue(charon.landforms().rift().isPresent() && charon.landforms().iceCap().isPresent(), "Charon has its chasm belt and polar cap");
        helper.assertTrue(pluto.landforms().volcanoes().map(v -> v.anchors().size()).orElse(0) >= 3, "water-ice mountains stand by the heart");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 300)
    public static void plutosHeartIsAFlatNitrogenPlainOfCells(GameTestHelper helper) {
        PlanetChunkGenerator gen = WorldTests.generator(helper, "pluto", 9L);
        PlanetTerrain t = gen.terrain();
        BasinFeature.Config heart = PlutoData.heart();
        double cz = BasinFeature.centreZ(t, heart);
        helper.assertTrue(Math.abs(t.latitude(cz) - PlutoData.HEART_LATITUDE) < 0.5, "the basin sits at its latitude");
        double floor = BasinFeature.offset(t, heart, heart.x() + heart.radius() * 0.4, cz);
        double outside = BasinFeature.offset(t, heart, heart.x() + heart.radius() * 2.2, cz);
        helper.assertTrue(floor < -6 && floor > -20, "a shallow, flat floor: " + floor);
        helper.assertTrue(outside == 0, "untouched far away");
        // cells: swells and border troughs over the basin, nothing far outside
        CellsFeature.Config cells = PlutoData.convectionCells();
        double lo = 99;
        double hi = -99;
        for (int i = 0; i < 400; i++) {
            double x = heart.x() + (i * 37 % 400) - 200;
            double z = cz + (i * 91 % 400) - 200;
            double o = CellsFeature.offset(cells, CellsFeature.sample(t, cells, x, z)) * CellsFeature.weight(t, cells, x, z);
            lo = Math.min(lo, o);
            hi = Math.max(hi, o);
        }
        helper.assertTrue(lo < -1.5 && hi > 0.6 && hi < 1.6, "cells gently domed, troughs sunk: " + lo + ".." + hi);
        helper.assertTrue(CellsFeature.weight(t, cells, heart.x() + heart.radius() * 1.5, cz) == 0, "no cells outside the basin");
        // the real feature on a chunk of the test level: nitrogen ice on top, tholin debris in the troughs
        ServerLevel level = helper.getLevel();
        int cx = (int) Math.floor(heart.x()) >> 4;
        int czc = (int) Math.floor(cz) >> 4;
        int ice = 0;
        int debris = 0;
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                level.getChunk(cx + dx, czc + dz);
                BlockPos origin = new BlockPos((cx + dx) << 4, 0, (czc + dz) << 4);
                new CellsFeature().place(new FeaturePlaceContext<>(Optional.empty(), level, gen, RandomSource.create(1), origin, cells));
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int px = origin.getX() + x;
                        int pz = origin.getZ() + z;
                        var top = level.getBlockState(new BlockPos(px, Columns.top(level, px, pz), pz));
                        ice += top.is(PlutoModule.NITROGEN_ICE.get()) ? 1 : 0;
                        debris += top.is(PlutoModule.STAINED_NITROGEN_ICE.get()) ? 1 : 0;
                    }
                }
            }
        }
        helper.assertTrue(ice > 500 && debris > 10 && debris < ice, "nitrogen ice with stained ice in the troughs: " + ice + " ice, " + debris + " debris");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 100)
    public static void nitrogenSliderSlidesOnNitrogenIce(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), PlutoModule.NITROGEN_ICE.get());
        helper.setBlock(new BlockPos(4, 1, 1), PlutoModule.PLUTONIAN_FROST.get());
        NitrogenSlider on = helper.spawn(PlutoEntities.NITROGEN_SLIDER.get(), new BlockPos(1, 2, 1));
        NitrogenSlider off = helper.spawn(PlutoEntities.NITROGEN_SLIDER.get(), new BlockPos(4, 2, 1));
        helper.succeedWhen(() -> {
            helper.assertTrue(on.onGround() && off.onGround(), "both settled");
            helper.assertTrue(on.isSliding() && !off.isSliding(), "slides only on nitrogen ice");
            helper.assertTrue(on.slideFactor() > off.slideFactor() * 2, "and goes much faster there: " + on.slideFactor() + " vs " + off.slideFactor());
        });
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 100)
    public static void frostTortoiseHidesInItsShell(GameTestHelper helper) {
        FrostTortoise t = helper.spawn(PlutoEntities.FROST_TORTOISE.get(), new BlockPos(2, 2, 2));
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        helper.assertFalse(t.inShell(), "starts out of its shell");
        t.hurt(helper.getLevel().damageSources().playerAttack(player), 4F);
        helper.assertTrue(t.inShell(), "a hit sends it into its shell");
        float before = t.getHealth();
        t.invulnerableTime = 0;
        t.hurt(helper.getLevel().damageSources().playerAttack(player), 8F);
        helper.assertTrue(before - t.getHealth() < 3F, "inside, it takes only a fraction of the damage: " + (before - t.getHealth()));
        helper.assertTrue(t.isFood(new net.minecraft.world.item.ItemStack(PlutoModule.AMMONIA_CRYSTAL.get())), "breeds on ammonia");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY)
    public static void plutoAndCharonTechnologiesExist(GameTestHelper helper) {
        var all = Tech.all(helper.getLevel().registryAccess());
        for (String id : new String[]{"nitrogen_glaciology", "kuiper_belt_navigation"}) {
            var node = all.get(Spacebreaker.id(id));
            helper.assertTrue(node != null, id + " is in the tree");
            helper.assertTrue(node.tier() >= 3 && node.tier() <= 5, id + " is a phase-3 tech");
        }
        helper.succeed();
    }
}
