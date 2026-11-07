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
        // a new machine recipe type the model does not know would silently hide everything made with it
        h.assertTrue(m.unmappedRecipeTypes.isEmpty(), "recipe types the progression model does not map to a machine "
                + "(add them to ProgressionModel.MACHINE_FOR_KIND): " + m.unmappedRecipeTypes);
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
        // uncrewed missions to an airless world: a satellite in Moon orbit and a probe on the Moon
        for (String mission : List.of("observation_satellite → spacebreaker:moon/orbit", "space_probe → spacebreaker:moon/surface")) {
            String r = m.uncrewedResults.getOrDefault(mission, "not tried (part not buildable or not unlocked)");
            h.assertTrue(r.startsWith("flown"), mission + ": " + r);
        }
        h.succeed();
    }

    /**
     * Every "visit one world of each group" advancement (grand_tour) can be completed: each requirement group has at least
     * one dimension the model can fly to and back from.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void grandTourIsCompletable(GameTestHelper h) {
        ProgressionModel m = model(h);
        h.assertTrue(h.getLevel().getServer().getAdvancements().get(Spacebreaker.id("main/grand_tour")) != null,
                "advancement spacebreaker:main/grand_tour exists");
        List<String> impossible = grandTourGaps(h.getLevel().getServer(), m);
        h.assertTrue(impossible.isEmpty(), "grand_tour groups with no reachable world: " + impossible);
        h.succeed();
    }

    /**
     * Comet balance (decided for 1.0): Halley is the end-game target, reached with the nuclear thermal engine by step
     * 17 and never by a direct chemical round trip (refuelling from its ice later is allowed); 67P and Tempel 1 stay
     * reachable with chemical rockets; Hale–Bopp is only seen in the sky.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void cometBalanceHolds(GameTestHelper h) {
        ProgressionModel m = model(h);
        var ntr = Spacebreaker.id("nuclear_thermal_engine");
        List<RocketSearch.Candidate> chemical = m.best.stream().filter(c -> !c.design().parts().contains(ntr)).toList();
        double gEarth = m.gravityOf(net.minecraft.world.level.Level.OVERWORLD);
        java.util.function.Function<String, RocketSearch.Result> chemicalRoundTrip = name -> {
            var loc = com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.get(Spacebreaker.id(name), false);
            if (loc.isEmpty()) {
                return null;
            }
            return RocketSearch.mission(chemical, loc.get(), m.realism, gEarth, m.gravityOf(loc.get().dimension()), null);
        };
        var halley = m.facts.get("world:spacebreaker:halley");
        h.assertTrue(halley != null && halley.step() <= 17, "Halley round trip by step 17: " + (halley == null ? "never" : halley.step()));
        h.assertTrue(chemicalRoundTrip.apply("halley") == null, "no direct chemical round trip to Halley");
        for (String c : List.of("churyumov_gerasimenko", "tempel_1")) {
            h.assertTrue(chemicalRoundTrip.apply(c) != null, c + " is reachable with a chemical rocket");
        }
        h.assertTrue(m.missions.keySet().stream().noneMatch(k -> k.contains("hale_bopp")) && !m.has("world:spacebreaker:hale_bopp"),
                "Hale-Bopp has no mission (sky only)");
        h.succeed();
    }

    /**
     * The realistic setting (1.0 target, decided with rocketry after QA-20): a fresh world reaches Earth orbit. The Moon and
     * Mars need heavier rockets there and are not required.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void realisticSettingReachesOrbit(GameTestHelper h) {
        var probe = h.makeMockPlayer(GameType.SURVIVAL);
        ProgressionModel m = new ProgressionModel(h.getLevel().getServer(), probe, SbConfig.Realism.REALISTIC);
        m.scan();
        m.run();
        ProgressionReport.Goal first = ProgressionReport.goals().get(0);
        h.assertTrue(m.has(first.fact()), "realistic setting: " + first.title() + " is reachable");
        h.succeed();
    }

    /** Requirement groups of grand_tour with no world the model reaches and returns from. */
    static List<String> grandTourGaps(net.minecraft.server.MinecraftServer server, ProgressionModel m) {
        var holder = server.getAdvancements().get(Spacebreaker.id("main/grand_tour"));
        if (holder == null) {
            return List.of("advancement missing");
        }
        var adv = holder.value();
        List<String> impossible = new ArrayList<>();
        for (List<String> group : adv.requirements().requirements()) {
            boolean ok = false;
            for (String key : group) {
                var criterion = adv.criteria().get(key);
                if (criterion != null && criterion.triggerInstance()
                        instanceof net.minecraft.advancements.critereon.ChangeDimensionTrigger.TriggerInstance t
                        && t.to().map(d -> m.has(ProgressionModel.world(d))).orElse(false)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                impossible.add(group.toString());
            }
        }
        return impossible;
    }

    private ProgressionAuditTests() {
    }
}
