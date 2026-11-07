package com.pizzaparker07.spacebreaker.testmod.qa;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.part.Propellant;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Balance what-if for propellant density: re-runs the whole progression model with the propellant a tank holds scaled
 * per propellant, and writes {@code run-gametest/qa/density_whatif.md}. Heavy (one full model per option), so it only
 * runs with {@code -Dspacebreaker.qa.densityWhatIf=true}; otherwise it passes at once.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DensityWhatIfTests {
    private static final List<String> WORLDS = List.of("mercury", "venus", "ceres", "vesta", "tempel_1", "churyumov_gerasimenko",
            "halley", "io", "europa", "ganymede", "callisto", "titan", "enceladus", "titania", "triton", "pluto", "charon");

    @GameTest(template = "empty", timeoutTicks = 2000)
    public static void propellantDensityWhatIf(GameTestHelper h) {
        if (!Boolean.getBoolean("spacebreaker.qa.densityWhatIf")) {
            h.succeed();
            return;
        }
        Map<String, Map<Propellant, Double>> options = new LinkedHashMap<>();
        options.put("A: no factor (today)", Map.of());
        options.put("B: realistic, today's tanks (methalox 0.81, hydrolox 0.35, NTR 0.07)",
                Map.of(Propellant.METHALOX, 0.81, Propellant.HYDROLOX, 0.35, Propellant.NUCLEAR, 0.07));
        options.put("C: square root (methalox 0.90, hydrolox 0.59, NTR 0.26)",
                Map.of(Propellant.METHALOX, Math.sqrt(0.81), Propellant.HYDROLOX, Math.sqrt(0.35), Propellant.NUCLEAR, Math.sqrt(0.07)));
        options.put("D: NTR only 0.35", Map.of(Propellant.NUCLEAR, 0.35));
        StringBuilder md = new StringBuilder("## Propellant density what-if (`qa/DensityWhatIfTests`)\n\n");
        md.append("| option | goals | techs | grand_tour gaps | round-trip worlds | ")
                .append(String.join(" | ", WORLDS)).append(" |\n|---|---|---|---|---|")
                .append("---|".repeat(WORLDS.size())).append('\n');
        StringBuilder detail = new StringBuilder();
        var probe = h.makeMockPlayer(GameType.SURVIVAL);
        try {
            for (var opt : options.entrySet()) {
                RocketSearch.DENSITY.clear();
                RocketSearch.DENSITY.putAll(opt.getValue());
                ProgressionModel m = new ProgressionModel(h.getLevel().getServer(), probe, SbConfig.realism());
                m.scan();
                m.run();
                long goals = ProgressionReport.goals().stream().filter(g -> m.has(g.fact())).count();
                long techs = m.techTree.keySet().stream().filter(id -> m.has(ProgressionModel.tech(id))).count();
                List<String> gaps = ProgressionAuditTests.grandTourGaps(h.getLevel().getServer(), m);
                long round = m.facts.keySet().stream().filter(k -> k.startsWith("world:spacebreaker:")).count();
                List<String> steps = new ArrayList<>();
                for (String w : WORLDS) {
                    var f = m.facts.get("world:spacebreaker:" + w);
                    steps.add(f == null ? "—" : String.valueOf(f.step()));
                }
                md.append(String.format(Locale.ROOT, "| %s | %d/%d | %d/%d | %s | %d | %s |%n", opt.getKey(), goals,
                        ProgressionReport.goals().size(), techs, m.techTree.size(), gaps.isEmpty() ? "none" : gaps.size() + " " + gaps,
                        round, String.join(" | ", steps)));
                for (String w : List.of("io", "pluto", "titan", "mercury")) {
                    var r = m.missions.get("spacebreaker:" + w + "/surface");
                    detail.append("- ").append(opt.getKey().substring(0, 1)).append(" ").append(w).append(": ")
                            .append(r == null ? "no round trip" : r.mode() + ", " + ProgressionModel.describe(r.rocket())).append('\n');
                }
                Spacebreaker.LOGGER.info("QA density what-if {}: goals {}, grand_tour gaps {}, round trips {}", opt.getKey(), goals, gaps, round);
            }
        } finally {
            RocketSearch.DENSITY.clear();
        }
        md.append("\nStep at which a crewed round trip to each world becomes possible (— = never).\n\nRockets used:\n\n").append(detail);
        ProgressionReport.write("density_whatif.md", md.toString());
        h.succeed();
    }

    /**
     * Insulated tanks against boil-off: the whole model on the balanced and the realistic setting, with and without
     * insulated tanks in the rocket search. Runs only with {@code -Dspacebreaker.qa.insulationWhatIf=true}; writes
     * {@code run-gametest/qa/insulation_whatif.md}.
     */
    @GameTest(template = "empty", timeoutTicks = 2000)
    public static void insulationWhatIf(GameTestHelper h) {
        if (!Boolean.getBoolean("spacebreaker.qa.insulationWhatIf")) {
            h.succeed();
            return;
        }
        StringBuilder md = new StringBuilder("## Insulated tanks what-if (`qa/DensityWhatIfTests.insulationWhatIf`)\n\n");
        md.append("| setting | insulated tanks | goals | techs | grand_tour gaps | round-trip worlds | ")
                .append(String.join(" | ", WORLDS)).append(" |\n|---|---|---|---|---|---|")
                .append("---|".repeat(WORLDS.size())).append('\n');
        StringBuilder detail = new StringBuilder();
        var probe = h.makeMockPlayer(GameType.SURVIVAL);
        try {
            for (SbConfig.Realism realism : List.of(SbConfig.Realism.BALANCED, SbConfig.Realism.REALISTIC)) {
                for (boolean insulated : List.of(false, true)) {
                    RocketSearch.INSULATED = insulated;
                    ProgressionModel m = new ProgressionModel(h.getLevel().getServer(), probe, realism);
                    m.scan();
                    m.run();
                    long goals = ProgressionReport.goals().stream().filter(g -> m.has(g.fact())).count();
                    long techs = m.techTree.keySet().stream().filter(id -> m.has(ProgressionModel.tech(id))).count();
                    List<String> gaps = ProgressionAuditTests.grandTourGaps(h.getLevel().getServer(), m);
                    long round = m.facts.keySet().stream().filter(k -> k.startsWith("world:spacebreaker:")).count();
                    List<String> steps = new ArrayList<>();
                    for (String w : WORLDS) {
                        var f = m.facts.get("world:spacebreaker:" + w);
                        steps.add(f == null ? "—" : String.valueOf(f.step()));
                    }
                    md.append(String.format(Locale.ROOT, "| %s | %s | %d/%d | %d/%d | %s | %d | %s |%n", realism, insulated ? "yes" : "no",
                            goals, ProgressionReport.goals().size(), techs, m.techTree.size(),
                            gaps.isEmpty() ? "none" : gaps.size() + " " + gaps, round, String.join(" | ", steps)));
                    var toOrbit = RocketSearch.route(com.pizzaparker07.spacebreaker.content.rocketry.nav.Location.EARTH_SURFACE,
                            com.pizzaparker07.spacebreaker.content.rocketry.nav.Location.EARTH_ORBIT, realism);
                    double bestDv = m.best.stream().mapToDouble(RocketSearch.Candidate::totalDv).max().orElse(0);
                    detail.append(String.format(Locale.ROOT, "- %s%s: Earth surface → orbit needs %.0f m/s; best buildable rocket %.0f m/s vacuum Δv (%d designs)%n",
                            realism, insulated ? " insulated" : " plain", RocketSearch.need(toOrbit, realism), bestDv, m.best.size()));
                    for (String w : List.of("mars/surface", "jupiter/orbit", "titan/surface", "pluto/surface", "hale_bopp/orbit")) {
                        var r = m.missions.get("spacebreaker:" + w);
                        detail.append("- ").append(realism).append(insulated ? " insulated " : " plain ").append(w).append(": ")
                                .append(r == null ? "no round trip" : r.mode() + ", " + ProgressionModel.describe(r.rocket())).append('\n');
                    }
                }
            }
        } finally {
            RocketSearch.INSULATED = true;
        }
        md.append("\nStep at which a crewed round trip to each world becomes possible (— = never).\n\nRockets used:\n\n").append(detail);
        ProgressionReport.write("insulation_whatif.md", md.toString());
        h.succeed();
    }

    /**
     * Comet missions with and without the nuclear thermal engine: Halley is meant to need it, Encke, 67P and Tempel 1
     * to stay reachable with chemical rockets. Runs only with {@code -Dspacebreaker.qa.cometWhatIf=true}; writes
     * {@code run-gametest/qa/comet_whatif.md}.
     */
    @GameTest(template = "empty", timeoutTicks = 2000)
    public static void cometWhatIf(GameTestHelper h) {
        if (!Boolean.getBoolean("spacebreaker.qa.cometWhatIf")) {
            h.succeed();
            return;
        }
        List<String> comets = List.of("halley", "encke", "churyumov_gerasimenko", "tempel_1", "hale_bopp");
        StringBuilder md = new StringBuilder("## Comet missions with and without the nuclear thermal engine\n\n");
        var probe = h.makeMockPlayer(GameType.SURVIVAL);
        try {
            for (boolean ntr : List.of(true, false)) {
                RocketSearch.EXCLUDED.clear();
                if (!ntr) {
                    RocketSearch.EXCLUDED.add(Spacebreaker.id("nuclear_thermal_engine"));
                }
                ProgressionModel m = new ProgressionModel(h.getLevel().getServer(), probe, SbConfig.realism());
                m.scan();
                m.run();
                md.append("### ").append(ntr ? "all engines" : "without the nuclear thermal engine").append("\n\n");
                md.append("grand_tour gaps: ").append(ProgressionAuditTests.grandTourGaps(h.getLevel().getServer(), m)).append("\n\n");
                for (String c : comets) {
                    for (String where : List.of("surface", "orbit")) {
                        var f = m.facts.get("world:spacebreaker:" + c);
                        var r = m.missions.get("spacebreaker:" + c + "/" + where);
                        md.append("- ").append(c).append('/').append(where).append(": ")
                                .append(r == null ? "no round trip" : r.mode() + ", " + ProgressionModel.describe(r.rocket()))
                                .append(where.equals("surface") && f != null ? " (world step " + f.step() + ")" : "").append('\n');
                    }
                }
                md.append('\n');
            }
        } finally {
            RocketSearch.EXCLUDED.clear();
        }
        ProgressionReport.write("comet_whatif.md", md.toString());
        h.succeed();
    }

    /**
     * The 1.0 matrix: the whole model on every realism setting. Runs only with
     * {@code -Dspacebreaker.qa.realismMatrix=true}; writes {@code run-gametest/qa/realism_matrix.md}.
     */
    @GameTest(template = "empty", timeoutTicks = 2000)
    public static void realismMatrix(GameTestHelper h) {
        if (!Boolean.getBoolean("spacebreaker.qa.realismMatrix")) {
            h.succeed();
            return;
        }
        StringBuilder md = new StringBuilder("## Progression on every realism setting\n\n");
        md.append("| setting | goals | techs | grand_tour gaps | round-trip worlds | Earth orbit | Moon | Mars | unobtainable items | model steps |\n")
                .append("|---|---|---|---|---|---|---|---|---|---|\n");
        var probe = h.makeMockPlayer(GameType.SURVIVAL);
        for (SbConfig.Realism realism : SbConfig.Realism.values()) {
            ProgressionModel m = new ProgressionModel(h.getLevel().getServer(), probe, realism);
            m.scan();
            m.run();
            long goals = ProgressionReport.goals().stream().filter(g -> m.has(g.fact())).count();
            long techs = m.techTree.keySet().stream().filter(id -> m.has(ProgressionModel.tech(id))).count();
            List<String> gaps = ProgressionAuditTests.grandTourGaps(h.getLevel().getServer(), m);
            long round = m.facts.keySet().stream().filter(k -> k.startsWith("world:spacebreaker:")).count();
            java.util.function.Function<String, String> step = k -> {
                var f = m.facts.get(k);
                return f == null ? "—" : String.valueOf(f.step());
            };
            String report = ProgressionReport.render(m);
            int i = report.indexOf("## Items that cannot be obtained");
            long unobtainable = i < 0 ? -1 : report.substring(i).lines().skip(2).takeWhile(l -> l.startsWith("- ")).count();
            int steps = m.facts.values().stream().mapToInt(ProgressionModel.Fact::step).max().orElse(0);
            md.append(String.format(Locale.ROOT, "| %s | %d/%d | %d/%d | %s | %d | %s | %s | %s | %d | %d |%n", realism, goals,
                    ProgressionReport.goals().size(), techs, m.techTree.size(), gaps.isEmpty() ? "none" : String.valueOf(gaps.size()),
                    round, step.apply("world:spacebreaker:earth_orbit"), step.apply("world:spacebreaker:moon"),
                    step.apply("world:spacebreaker:mars"), unobtainable, steps));
        }
        ProgressionReport.write("realism_matrix.md", md.toString());
        h.succeed();
    }

    private DensityWhatIfTests() {
    }
}
