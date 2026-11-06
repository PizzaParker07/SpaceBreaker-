package com.pizzaparker07.spacebreaker.testmod.showcase;

import java.util.Map;
import java.util.TreeMap;

/** All showcase scenarios by name (-Pscenario=<name>). Add one line per scenario. */
public final class ShowcaseScenarios {
    private static final Map<String, ShowcaseScenario> ALL = new TreeMap<>();

    static {
        ALL.put("style_sample", new StyleSampleScenario());
        ALL.put("suit", new SuitScenario());
        ALL.put("industry", new IndustryScenario());
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
