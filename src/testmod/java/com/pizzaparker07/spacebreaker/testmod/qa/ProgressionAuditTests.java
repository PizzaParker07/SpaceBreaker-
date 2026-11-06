package com.pizzaparker07.spacebreaker.testmod.qa;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Whole-game progression audit (see {@link ProgressionModel}). Writes {@code run-gametest/qa/progression.md}
 * (copied into docs/QA.md) and fails when a technology that used to be learnable becomes a dead end.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProgressionAuditTests {
    /**
     * Technologies the audit currently cannot reach in survival. Each entry is a reported bug (docs/QA.md). The test
     * fails when a technology NOT on this list is unreachable; remove entries as their owners fix them.
     */
    static final Set<String> KNOWN_DEAD_ENDS = Set.of();

    private static ProgressionModel cached;
    static ProgressionModel whatIf;

    static synchronized ProgressionModel model(GameTestHelper h) {
        if (cached == null) {
            var probe = h.makeMockPlayer(GameType.SURVIVAL);
            ProgressionModel m = new ProgressionModel(h.getLevel().getServer(), probe, SbConfig.realism());
            m.scan();
            m.run();
            cached = m;
            // what if: give every technology that has no possible source (and whose prerequisites are met) a disk on
            // Earth, round after round, to see the next dead end behind the first one
            List<ProgressionModel> rounds = new ArrayList<>();
            Set<ResourceLocation> assumed = new java.util.TreeSet<>();
            ProgressionModel last = m;
            for (int round = 0; round < 8; round++) {
                List<ResourceLocation> frontier = last.sourcelessFrontier();
                if (frontier.isEmpty()) {
                    break;
                }
                assumed.addAll(frontier);
                ProgressionModel next = new ProgressionModel(h.getLevel().getServer(), probe, SbConfig.realism());
                next.assumedDisks.addAll(assumed);
                next.scan();
                next.run();
                rounds.add(next);
                last = next;
            }
            whatIf = last == m ? null : last;
            ProgressionReport.write("progression.md", ProgressionReport.render(m)
                    + (whatIf == null ? "" : "\n# What if the source-less technologies got a disk on Earth\n\nAssumed disks: "
                    + String.join(", ", assumed.stream().map(ResourceLocation::getPath).toList()) + "\n\n"
                    + ProgressionReport.render(whatIf).replace("\n## ", "\n### ").replace("\n### ✅", "\n#### ✅").replace("\n### ❌", "\n#### ❌")));
            StringBuilder facts = new StringBuilder();
            m.facts.forEach((k, v) -> facts.append(v.step()).append('\t').append(k).append('\t').append(v.reason()).append('\t').append(v.deps()).append('\n'));
            ProgressionReport.write("facts.tsv", facts.toString());
        }
        return cached;
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void progressionReportIsWritten(GameTestHelper h) {
        ProgressionModel m = model(h);
        h.assertTrue(m.rules.size() > 100, "recipes were read: " + m.rules.size());
        h.assertTrue(m.worlds.size() > 2, "worlds were scanned: " + m.worlds.size());
        h.assertTrue(m.has("item:spacebreaker:research_lab"), "the research lab is buildable from a fresh world");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void noNewTechnologyDeadEnds(GameTestHelper h) {
        ProgressionModel m = model(h);
        List<String> fresh = new ArrayList<>();
        List<String> fixed = new ArrayList<>();
        for (ResourceLocation id : m.techTree.keySet()) {
            boolean known = KNOWN_DEAD_ENDS.contains(id.getPath());
            boolean reachable = m.has(ProgressionModel.tech(id));
            if (!reachable && !known) {
                fresh.add(id.getPath() + " (" + m.whyNot(id) + ")");
            } else if (reachable && known) {
                fixed.add(id.getPath());
            }
        }
        if (!fixed.isEmpty()) {
            Spacebreaker.LOGGER.info("QA: dead ends fixed, remove from KNOWN_DEAD_ENDS: {}", fixed);
        }
        h.assertTrue(fresh.isEmpty(), "new technology dead ends: " + fresh);
        h.succeed();
    }

    /**
     * With the source-less technologies given a disk (the "what if" model), the Moon and Mars missions must stay
     * possible: catches regressions that are not about research (recipes, fuel chain, rocket parts, transfer costs).
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void moonAndMarsStayReachableOnceResearchIsFixed(GameTestHelper h) {
        ProgressionModel base = model(h);
        ProgressionModel m = whatIf == null ? base : whatIf;
        List<String> lost = new ArrayList<>();
        for (ProgressionReport.Goal g : ProgressionReport.goals()) {
            if (!m.has(g.fact())) {
                lost.add(g.title());
            }
        }
        h.assertTrue(lost.isEmpty(), "goals no longer reachable even with every source-less technology on a disk: " + lost);
        h.succeed();
    }

    private ProgressionAuditTests() {
    }
}
