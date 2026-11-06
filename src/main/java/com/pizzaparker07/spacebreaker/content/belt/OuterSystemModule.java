package com.pizzaparker07.spacebreaker.content.belt;

import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbDatapack;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.neoforged.bus.api.IEventBus;

import java.util.List;

/**
 * The outer Solar System: the asteroid belt (Ceres, Vesta), Jupiter's moons, Saturn's moons, and Pluto with Charon. One entry in the module
 * list that hands every call to the shared outer-world code and then to each region's module in order.
 */
public final class OuterSystemModule implements SbModule {
    private final List<SbModule> parts = List.of(
            new OuterWorlds(),
            new BeltModule(),
            new com.pizzaparker07.spacebreaker.content.jupiter.JupiterModule(),
            new com.pizzaparker07.spacebreaker.content.saturn.SaturnModule(),
            new com.pizzaparker07.spacebreaker.content.pluto.PlutoModule()
    );

    @Override
    public String id() {
        return "outer";
    }

    @Override
    public void register(IEventBus modBus) {
        parts.forEach(p -> p.register(modBus));
    }

    @Override
    public void addTranslations(Lang lang) {
        parts.forEach(p -> p.addTranslations(lang));
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        parts.forEach(p -> p.addBlockLoot(loot));
    }

    @Override
    public void addTags(SbTags tags) {
        parts.forEach(p -> p.addTags(tags));
    }

    @Override
    public void addRecipes(RecipeOutput output, HolderLookup.Provider registries) {
        parts.forEach(p -> p.addRecipes(output, registries));
    }

    @Override
    public void addDatapackEntries(SbDatapack datapack) {
        parts.forEach(p -> p.addDatapackEntries(datapack));
    }
}
