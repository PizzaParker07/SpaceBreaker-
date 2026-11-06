package com.pizzaparker07.spacebreaker.content.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipeBuilder;
import com.pizzaparker07.spacebreaker.content.mercury.MercuryModule;
import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.venus.VenusModule;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;

/**
 * How the raw materials of the inner planets enter the industry's machines (crusher recipes; the inputs carry
 * {@code c:} tags so later machines can take them by tag):
 * <ul>
 *   <li>perchlorate soil → perchlorate salt ({@code c:dusts/perchlorate}), the oxygen source of Mars;</li>
 *   <li>Martian, dusty and polar crater ice → plain ice ({@code c:ices/water}), which melts into water for the
 *       electrolyser;</li>
 *   <li>Venus's sulfur crust and Mercury's sulfide-rich hollow deposits → sulfur;</li>
 *   <li>Mercury's graphite burnt in oxygen → carbon dioxide (C + O₂ → CO₂, chemical reactor). With hydrogen and oxygen
 *       from polar crater ice this closes a methalox loop on Mercury: CO₂ + H₂ → methane (Sabatier), liquefied with
 *       oxygen into rocket propellant, so a rocket can refuel there for the way home.</li>
 * </ul>
 */
public final class InnerResources {
    private InnerResources() {
    }

    static void recipes(RecipeOutput out) {
        ProcessingRecipeBuilder.of(MachinesModule.CRUSHING).in(MarsModule.PERCHLORATE_SOIL.get()).out(MarsModule.PERCHLORATE_SALT.get())
                .bonus(MarsModule.PERCHLORATE_SALT.get(), 1, 0.25F).time(100).energy(16).save(out, Spacebreaker.id("crushing/perchlorate_soil"));
        ProcessingRecipeBuilder.of(MachinesModule.CRUSHING).in(MarsModule.MARTIAN_ICE.get()).out(Items.ICE)
                .time(60).energy(10).save(out, Spacebreaker.id("crushing/martian_ice"));
        ProcessingRecipeBuilder.of(MachinesModule.CRUSHING).in(MarsModule.DUSTY_ICE.get()).out(Items.ICE)
                .bonus(MarsModule.MARTIAN_SAND.get(), 1, 0.5F).time(80).energy(12).save(out, Spacebreaker.id("crushing/dusty_ice"));
        ProcessingRecipeBuilder.of(MachinesModule.CRUSHING).in(MoonModule.LUNAR_ICE.get()).out(Items.ICE)
                .time(60).energy(10).save(out, Spacebreaker.id("crushing/polar_crater_ice"));
        ProcessingRecipeBuilder.of(MachinesModule.CRUSHING).in(MercuryModule.MERCURY_ICE.get()).out(Items.ICE)
                .bonus(MercuryModule.GRAPHITE.get(), 1, 0.1F).time(60).energy(10).save(out, Spacebreaker.id("crushing/mercury_ice"));
        ProcessingRecipeBuilder.of(MachinesModule.CRUSHING).in(VenusModule.SULFUR_CRUST.get()).out(VenusModule.SULFUR.get(), 2)
                .time(80).energy(14).save(out, Spacebreaker.id("crushing/sulfur_crust"));
        ProcessingRecipeBuilder.of(MachinesModule.CRUSHING).in(MercuryModule.HOLLOW_DEPOSIT.get()).out(VenusModule.SULFUR.get())
                .bonus(VenusModule.SULFUR.get(), 1, 0.3F).time(80).energy(14).save(out, Spacebreaker.id("crushing/hollow_deposit"));
        ProcessingRecipeBuilder.of(MachinesModule.CHEMICAL_REACTION).in(MercuryModule.GRAPHITE.get())
                .fluidIn(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.TAG_OXYGEN, 500)
                .fluidOut(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.CARBON_DIOXIDE.get(), 500)
                .time(60).energy(40).save(out, Spacebreaker.id("chemical_reaction/graphite_combustion"));
    }
}
