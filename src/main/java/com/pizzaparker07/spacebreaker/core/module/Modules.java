package com.pizzaparker07.spacebreaker.core.module;

import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.blueprint.BlueprintModule;
import com.pizzaparker07.spacebreaker.content.worldgen.WorldgenModule;

import java.util.List;

/** Every feature module, in load order. Add one line per new module. */
public final class Modules {
    public static final List<SbModule> ALL = List.of(
            new EarthModule(),
            new com.pizzaparker07.spacebreaker.content.audio.AudioModule(),
            new com.pizzaparker07.spacebreaker.content.fluids.FluidsModule(),
            new WorldgenModule(),
            new MoonModule(),
            new MachinesModule(),
            new com.pizzaparker07.spacebreaker.content.vehicles.VehiclesModule(),
            new SuitModule(),
            new BlueprintModule(),
            new com.pizzaparker07.spacebreaker.content.research.ResearchModule(),
            new com.pizzaparker07.spacebreaker.content.dungeons.DungeonsModule(),
            new RocketryModule(),
            new com.pizzaparker07.spacebreaker.content.mars.MarsModule(),
            new com.pizzaparker07.spacebreaker.content.venus.VenusModule(),
            new com.pizzaparker07.spacebreaker.content.mercury.MercuryModule(),
            new com.pizzaparker07.spacebreaker.content.belt.OuterSystemModule(),
            new com.pizzaparker07.spacebreaker.compat.CompatModule()
    );

    private Modules() {
    }
}
