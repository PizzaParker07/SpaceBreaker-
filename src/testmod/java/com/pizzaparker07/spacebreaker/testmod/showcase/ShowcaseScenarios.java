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
        ALL.put("blueprint", new BlueprintScenario());
        ALL.put("industry", new IndustryScenario());
        ALL.put("moon", new MoonScenario());
        ALL.put("research", new ResearchScenario());
        ALL.put("mars", new MarsScenario());
        ALL.put("phobos", new MarsMoonScenario(false));
        ALL.put("deimos", new MarsMoonScenario(true));
        ALL.put("venus", new VenusScenario());
        ALL.put("mercury", new MercuryScenario());
        ALL.put("inner_orbit", new InnerOrbitScenario());
        ALL.put("rocketry", new RocketryScenario());
        ALL.put("compat", new CompatScenario());
        ALL.put("jei", new JeiScenario(true));
        ALL.put("emi", new JeiScenario(false));
        ALL.put("belt", new BeltScenario(false));
        ALL.put("vesta", new BeltScenario(true));
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
