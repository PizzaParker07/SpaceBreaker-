package com.pizzaparker07.spacebreaker.testmod.outer;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.neptune.NeptuneModule;
import com.pizzaparker07.spacebreaker.content.pluto.PlutoData;
import com.pizzaparker07.spacebreaker.content.pluto.PlutoModule;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.testmod.mars.WorldTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Cold traps follow the astro polar light data (water ice only in shadowed polar crater floors of airless, cold
 * bodies). The volatile ices of the outer worlds come from their own landforms and features and must not change:
 * Triton's pink nitrogen cap, Pluto's and Charon's red polar frost, Pluto's nitrogen plain, and everything off the poles.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OuterColdTrapTests {
    private static final LevelHeightAccessor HEIGHT = LevelHeightAccessor.create(-64, 384);

    private static PlanetChunkGenerator bound(GameTestHelper helper, String world) {
        PlanetChunkGenerator gen = WorldTests.generator(helper, world, 11L);
        gen.bindPolar(BodyAdapter.polarIllumination(helper.getLevel().registryAccess(), Spacebreaker.id(world).toString()).orElse(null));
        return gen;
    }

    private static BlockState surface(PlanetChunkGenerator gen, int x, int z) {
        return gen.getBaseColumn(x, z, HEIGHT, null).getBlock(gen.surfaceY(x, z, HEIGHT));
    }

    private static int zAt(PlanetChunkGenerator gen, double latitude) {
        return (int) Math.round(-latitude / 90.0 * gen.terrain().profile().poleDistance());
    }

    /** The surface at a pole, over a small patch: some of it must be the cap's frost. */
    private static int capCount(PlanetChunkGenerator gen, double latitude, Block frost) {
        int z = zAt(gen, latitude);
        int n = 0;
        for (int x = -40; x <= 40; x += 8) {
            for (int dz = -16; dz <= 16; dz += 8) {
                n += surface(gen, x, z + dz).is(frost) ? 1 : 0;
            }
        }
        return n;
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 200)
    public static void polarCapsOfTheOuterWorldsSurviveTheColdTrapRule(GameTestHelper helper) {
        PlanetChunkGenerator triton = bound(helper, "triton");
        helper.assertTrue(capCount(triton, -86, NeptuneModule.PINK_NITROGEN_FROST.get()) > 20, "Triton keeps its pink nitrogen cap");
        PlanetChunkGenerator pluto = bound(helper, "pluto");
        PlanetChunkGenerator charon = bound(helper, "charon");
        int red = capCount(charon, 86, PlutoModule.RED_POLAR_FROST.get()) + capCount(charon, -86, PlutoModule.RED_POLAR_FROST.get());
        helper.assertTrue(red > 20, "Charon keeps its red polar frost: " + red);
        // Pluto's nitrogen ice is laid by its basin and cell features, which never ask about cold traps
        helper.assertTrue(PlutoData.convectionCells().topBlock().map(b -> b.is(PlutoModule.NITROGEN_ICE.get())).orElse(false),
                "Pluto's heart is still paved with nitrogen ice");
        helper.assertTrue(pluto.terrain().profile().landforms().volcanoes().isPresent(), "and its water-ice mountains stand");
        helper.succeed();
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 300)
    public static void coldTrapRuleOnlyTouchesShadowedPolarFloors(GameTestHelper helper) {
        for (String world : new String[]{"pluto", "titan", "triton", "miranda", "oberon"}) {
            PlanetChunkGenerator bound = bound(helper, world);
            PlanetChunkGenerator plain = WorldTests.generator(helper, world, 11L);
            plain.bindPolar(null);
            for (int i = 0; i < 24; i++) {
                int x = (i * 97) % 600 - 300;
                int z = zAt(bound, (i * 37) % 120 - 60);
                // the same seed and the same columns: off the poles nothing may differ
                BlockState a = surface(bound, x, z);
                BlockState b = surface(plain, x, z);
                helper.assertTrue(a == b, world + " at " + x + "," + z + ": " + a + " vs " + b);
            }
        }
        helper.succeed();
    }
}
