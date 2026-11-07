package com.pizzaparker07.spacebreaker.testmod.showcase;

import java.util.Map;
import java.util.TreeMap;

/** All showcase scenarios by name (-Pscenario=<name>). Add one line per scenario. */
public final class ShowcaseScenarios {
    private static final Map<String, ShowcaseScenario> ALL = new TreeMap<>();

    static {
        ALL.put("style_sample", new StyleSampleScenario());
        ALL.put("suit", new SuitScenario());
        ALL.put("suit_moon", new SuitMoonScenario());
        ALL.put("suit_tiers", new SuitTiersScenario());
        ALL.put("suit_eva_moon", new SuitEvaScenario(com.pizzaparker07.spacebreaker.content.moon.MoonModule.MOON, "eva_moon",
                () -> com.pizzaparker07.spacebreaker.content.moon.MoonModule.LUNAR_REGOLITH.get().defaultBlockState()));
        ALL.put("suit_eva_mars", new SuitEvaScenario(com.pizzaparker07.spacebreaker.content.mars.MarsModule.MARS, "eva_mars",
                () -> com.pizzaparker07.spacebreaker.content.mars.MarsModule.MARTIAN_REGOLITH.get().defaultBlockState()));
        ALL.put("suit_eva_titan", new SuitEvaScenario(com.pizzaparker07.spacebreaker.content.saturn.SaturnModule.TITAN, "eva_titan",
                net.minecraft.world.level.block.Blocks.PACKED_ICE::defaultBlockState));
        ALL.put("suit_eva_orbit", new SuitEvaScenario(com.pizzaparker07.spacebreaker.content.rocketry.nav.Location.Orbits.EARTH_ORBIT, "eva_orbit",
                net.minecraft.world.level.block.Blocks.IRON_BLOCK::defaultBlockState));
        ALL.put("blueprint", new BlueprintScenario());
        ALL.put("industry", new IndustryScenario());
        ALL.put("rover_moon", new RoverScenario(com.pizzaparker07.spacebreaker.content.moon.MoonModule.MOON, false));
        ALL.put("rover_mars", new RoverScenario(com.pizzaparker07.spacebreaker.content.mars.MarsModule.MARS, true));
        ALL.put("moon", new MoonScenario());
        ALL.put("sky_planets", new SkyPlanetsScenario(false));
        ALL.put("sky_planets_mars", new SkyPlanetsScenario(true));
        ALL.put("research", new ResearchScenario());
        ALL.put("mars", new MarsScenario());
        ALL.put("phobos", new MarsMoonScenario(false));
        ALL.put("deimos", new MarsMoonScenario(true));
        ALL.put("venus", new VenusScenario());
        ALL.put("mercury", new MercuryScenario());
        ALL.put("inner_orbit", new InnerOrbitScenario());
        ALL.put("rocketry", new RocketryScenario());
        ALL.put("compat", new CompatScenario());
        ALL.put("perf", new PerfScenario());
        ALL.put("lod", new LodScenario());
        ALL.put("jei", new JeiScenario(true));
        ALL.put("emi", new JeiScenario(false));
        ALL.put("belt", new BeltScenario(false));
        ALL.put("vesta", new BeltScenario(true));
        ALL.put("jovian", new JovianScenario("europa"));
        ALL.put("io", new JovianScenario("io"));
        ALL.put("ganymede", new JovianScenario("ganymede"));
        ALL.put("callisto", new JovianScenario("callisto"));
        ALL.put("saturnian", new SaturnianScenario(false));
        ALL.put("enceladus", new SaturnianScenario(true));
        ALL.put("miranda", new MirandaScenario());
        ALL.put("ariel", new ArielScenario());
        ALL.put("titania", new TitaniaScenario());
        ALL.put("umbriel", new UmbrielScenario());
        ALL.put("oberon", new OberonScenario());
        ALL.put("triton", new TritonScenario());
        ALL.put("pluto", new PlutoScenario(false));
        ALL.put("charon", new PlutoScenario(true));
        ALL.put("qa_playthrough", new QaPlaythroughScenario(new RocketryScenario()));
    }

    public static ShowcaseScenario get(String name) {
        ShowcaseScenario scenario = ALL.get(name);
        if (scenario == null) {
            throw new IllegalArgumentException("Unknown showcase scenario '" + name + "', known: " + ALL.keySet());
        }
        return scenario;
    }

    private ShowcaseScenarios() {
    }
}
