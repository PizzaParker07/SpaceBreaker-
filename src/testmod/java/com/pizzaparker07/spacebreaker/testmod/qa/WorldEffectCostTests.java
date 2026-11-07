package com.pizzaparker07.spacebreaker.testmod.qa;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.MarsSeasons;
import com.pizzaparker07.spacebreaker.content.worldgen.Eclipses;
import com.pizzaparker07.spacebreaker.core.astro.BodyAdapter;
import com.pizzaparker07.spacebreaker.core.astro.GameClock;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.body.BodyType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Server cost of the world effects that the soak test cannot reach (the GameTest server has no Mars or moon level):
 * the eclipse search each moon level runs (once per eclipse, every 100 ticks while none is found within a year) and the
 * Mars seasonal frost (a few columns per player per tick). Times them directly and writes
 * {@code run-gametest/qa/world_effects.md}; fails only on a single eclipse search above {@link #SPIKE_MS}.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorldEffectCostTests {
    /** A search slower than this would be a visible hitch on the server tick. */
    static final double SPIKE_MS = 50;

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void eclipseSearchAndFrostAreCheap(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        var reg = Bodies.registry(access).orElseThrow();
        var sys = BodyAdapter.system(access).orElseThrow();
        double spr = GameClock.simSecondsPerTick(BodyAdapter.timeScale()) * 20.0;
        List<String> lines = new ArrayList<>();
        List<String> slow = new ArrayList<>();
        double worst = 0;
        int moons = 0;
        int never = 0;
        for (var entry : reg.entrySet()) {
            var b = entry.getValue();
            if (b.dimension().isEmpty() || b.parent().isEmpty()) {
                continue;
            }
            var parent = reg.getOptional(b.parent().get());
            if (parent.isEmpty() || parent.get().type() == BodyType.STAR) {
                continue;
            }
            String observer = entry.getKey().location().toString();
            String parentId = b.parent().get().toString();
            moons++;
            double best = Double.MAX_VALUE;
            Eclipses.Event ev = null;
            double at = 0;
            for (int run = 0; run < 3; run++) {
                at = 1.0e6 * run;
                long t0 = System.nanoTime();
                ev = Eclipses.event(sys, observer, parentId, at, Eclipses.MIN_SHOWN_SECONDS * spr, 3.2e7, 3 * 3600);
                best = Math.min(best, (System.nanoTime() - t0) / 1e6);
            }
            worst = Math.max(worst, best);
            if (ev == null) {
                never++;
            }
            String line = String.format(Locale.ROOT, "| %s | %s | %.2f ms | %s |", observer, parentId, best,
                    ev == null ? "none within a year: searched again every 100 ticks (≈ " + String.format(Locale.ROOT, "%.3f", best / 100) + " ms/tick)"
                            : String.format(Locale.ROOT, "next %.1f days after the search time, peak %.0f %%", (ev.start() - at) / 86400.0, ev.peak() * 100));
            lines.add(line);
            if (best > SPIKE_MS) {
                slow.add(observer + " " + String.format(Locale.ROOT, "%.1f ms", best));
            }
        }
        // Mars frost: time the column update on a test patch (overworld blocks, the same code path)
        var level = h.getLevel();
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                level.setBlock(h.absolutePos(new BlockPos(x, 1, z)), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        int calls = 0;
        long f0 = System.nanoTime();
        for (int round = 0; round < 20; round++) {
            for (int x = 0; x < 8; x++) {
                for (int z = 0; z < 8; z++) {
                    BlockPos p = h.absolutePos(new BlockPos(x, 1, z));
                    MarsSeasons.updateColumn(level, p.getX(), p.getZ(), round % 2 == 0);
                    calls++;
                }
            }
        }
        double perColumnUs = (System.nanoTime() - f0) / 1e3 / calls;
        double perPlayerTickMs = perColumnUs * MarsSeasons.COLUMNS_PER_TICK / 1000;
        StringBuilder md = new StringBuilder("## World effects cost (`qa/WorldEffectCostTests`)\n\n");
        md.append(String.format(Locale.ROOT, "Eclipse search per moon level (best of 3, %d moon levels, %d with no eclipse within a year), worst %.2f ms:%n%n",
                moons, never, worst));
        md.append("| level | parent | one search | result |\n|---|---|---|---|\n");
        lines.forEach(l -> md.append(l).append('\n'));
        md.append(String.format(Locale.ROOT, "%nMars seasonal frost: %.1f µs per column update (alternately laying and sublimating), "
                + "%d columns per player per tick → %.3f ms per player per tick.%n", perColumnUs, MarsSeasons.COLUMNS_PER_TICK, perPlayerTickMs));
        ProgressionReport.write("world_effects.md", md.toString());
        h.assertTrue(moons > 0, "moon levels found in the body registry");
        h.assertTrue(slow.isEmpty(), "eclipse searches slower than " + SPIKE_MS + " ms: " + slow);
        h.assertTrue(perPlayerTickMs < 1.0, "frost costs " + perPlayerTickMs + " ms per player per tick");
        h.succeed();
    }

    private WorldEffectCostTests() {
    }
}
