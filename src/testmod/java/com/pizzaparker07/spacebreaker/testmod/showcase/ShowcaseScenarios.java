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
        ALL.put("suit_eva_halley", new SuitEvaScenario(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                com.pizzaparker07.spacebreaker.Spacebreaker.id("halley")), "eva_halley", () -> com.pizzaparker07.spacebreaker.content.belt.Comets.HALLEY.soil.get().defaultBlockState(), true));
        ALL.put("suit_eva_67p", new SuitEvaScenario(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                com.pizzaparker07.spacebreaker.Spacebreaker.id("churyumov_gerasimenko")), "eva_67p", () -> com.pizzaparker07.spacebreaker.content.belt.Comets.CHURYUMOV.soil.get().defaultBlockState(), true));
        ALL.put("suit_capsule", new SuitCapsuleScenario());
        ALL.put("base_airlock", new BaseAirlockScenario());
        ALL.put("suit_eva_titan", new SuitEvaScenario(com.pizzaparker07.spacebreaker.content.saturn.SaturnModule.TITAN, "eva_titan",
                net.minecraft.world.level.block.Blocks.PACKED_ICE::defaultBlockState));
        ALL.put("suit_eva_orbit", new SuitEvaScenario(com.pizzaparker07.spacebreaker.content.rocketry.nav.Location.Orbits.EARTH_ORBIT, "eva_orbit",
                net.minecraft.world.level.block.Blocks.IRON_BLOCK::defaultBlockState));
        ALL.put("blueprint", new BlueprintScenario());
        ALL.put("industry", new IndustryScenario());
        ALL.put("rover_moon", new RoverScenario(com.pizzaparker07.spacebreaker.content.moon.MoonModule.MOON, false));
        ALL.put("rover_mars", new RoverScenario(com.pizzaparker07.spacebreaker.content.mars.MarsModule.MARS, true));
        ALL.put("solar_dust", new SolarDustScenario());
        ALL.put("moon", new MoonScenario());
        ALL.put("sky_planets", new SkyPlanetsScenario(false));
        ALL.put("sky_planets_mars", new SkyPlanetsScenario(true));
        ALL.put("coma_sky", new ComaSkyScenario());
        ALL.put("research", new ResearchScenario());
        ALL.put("mars", new MarsScenario());
        ALL.put("mars_dust", new MarsDustScenario());
        ALL.put("mars_frost", new MarsFrostScenario());
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
        ALL.put("info", new InfoOverlayScenario());
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
        ALL.put("nix", new TinyMoonScenario(com.pizzaparker07.spacebreaker.content.pluto.PlutoMoonlets.NIX, null, 0, 0));
        ALL.put("hydra", new TinyMoonScenario(com.pizzaparker07.spacebreaker.content.pluto.PlutoMoonlets.HYDRA, null, 0, 0));
        ALL.put("churyumov_gerasimenko", new TinyMoonScenario(com.pizzaparker07.spacebreaker.content.belt.Comets.CHURYUMOV, null, 0, 0));
        ALL.put("encke", new TinyMoonScenario(com.pizzaparker07.spacebreaker.content.belt.Comets.ENCKE, null, 0, 0));
        ALL.put("halley", new TinyMoonScenario(com.pizzaparker07.spacebreaker.content.belt.Comets.HALLEY, null, 0, 0));
        ALL.put("bennu", new TinyMoonScenario(com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.BENNU, null, 0, 0));
        ALL.put("ryugu", new TinyMoonScenario(com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.RYUGU, "equatorial_ridge", 0, 0));
        ALL.put("eros", new TinyMoonScenario(com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.EROS, "himeros", com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.himeros().x(), com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.himeros().latitude()));
        ALL.put("itokawa", new TinyMoonScenario(com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.ITOKAWA, "muses_sea", com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.musesSea().x(), com.pizzaparker07.spacebreaker.content.belt.NearEarthAsteroids.musesSea().latitude()));
        ALL.put("nereid", new TinyMoonScenario(com.pizzaparker07.spacebreaker.content.neptune.Nereid.NEREID, null, 0, 0));
        ALL.put("proteus", new TinyMoonScenario(com.pizzaparker07.spacebreaker.content.neptune.Proteus.PROTEUS, "pharos_basin",
                com.pizzaparker07.spacebreaker.content.neptune.Proteus.pharos().x(), com.pizzaparker07.spacebreaker.content.neptune.Proteus.pharos().latitude()));
        ALL.put("perf_outer", new OuterPerfScenario());
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
