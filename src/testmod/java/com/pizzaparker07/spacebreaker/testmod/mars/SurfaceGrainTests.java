package com.pizzaparker07.spacebreaker.testmod.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The inner worlds' ground is rough, but in steps a few blocks wide: no field of lone one-block holes and pillars.
 * Those used to pockmark the Martian plains wherever the block-scale unevenness ran finer than two blocks.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SurfaceGrainTests {
    /** Share of columns lower (or higher) than all four neighbours, over a patch of the world. */
    static double loneColumns(PlanetTerrain t, int x0, int z0, int size) {
        int[][] h = new int[size + 2][size + 2];
        for (int i = 0; i < size + 2; i++) {
            for (int j = 0; j < size + 2; j++) {
                h[i][j] = (int) Math.floor(t.height(x0 + i, z0 + j));
            }
        }
        int lone = 0;
        for (int i = 1; i <= size; i++) {
            for (int j = 1; j <= size; j++) {
                int c = h[i][j];
                int a = h[i - 1][j], b = h[i + 1][j], d = h[i][j - 1], e = h[i][j + 1];
                if ((c < a && c < b && c < d && c < e) || (c > a && c > b && c > d && c > e)) {
                    lone++;
                }
            }
        }
        return lone / (double) (size * size);
    }

    @GameTest(template = WorldTests.EMPTY, timeoutTicks = 400)
    public static void innerGroundHasNoLoneBlockPits(GameTestHelper helper) {
        StringBuilder report = new StringBuilder();
        boolean ok = true;
        for (String w : new String[]{"mars", "venus", "mercury"}) {
            PlanetTerrain t = new PlanetTerrain(WorldTests.profile(helper, w), 4242L);
            double worst = 0;
            for (int k = 0; k < 6; k++) {
                worst = Math.max(worst, loneColumns(t, -3000 + k * 1100, 700 - k * 900, 64));
            }
            report.append(w).append(' ').append(String.format("%.3f", worst)).append("; ");
            ok &= worst < 0.01;
        }
        helper.assertTrue(ok, "too many lone one-block pits or pillars: " + report);
        helper.succeed();
    }
}
