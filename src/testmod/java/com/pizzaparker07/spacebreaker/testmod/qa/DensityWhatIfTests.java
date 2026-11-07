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

    private DensityWhatIfTests() {
    }
}
