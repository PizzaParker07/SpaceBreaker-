package com.pizzaparker07.spacebreaker.content.machines;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import static com.pizzaparker07.spacebreaker.content.earth.EarthModule.*;
import static com.pizzaparker07.spacebreaker.content.machines.MachineRecipes.has;
import static com.pizzaparker07.spacebreaker.content.machines.MachineRecipes.shaped;
import static com.pizzaparker07.spacebreaker.content.machines.MachinesModule.*;

/** Recipes for tanks and the multiblock machines. */
final class MachineRecipesExtra {
    private MachineRecipesExtra() {
    }

    static void add(RecipeOutput out) {
        shaped(out, PUMPJACK.item().get(), 1, has(STEEL.plateTag), "SRS", "SMS", "PCP",
                'R', STEEL.rodTag, 'S', STEEL.plateTag, 'M', ELECTRIC_MOTOR.get(), 'P', FLUID_PIPE.get(), 'C', BASIC_CIRCUIT.get());
        shaped(out, REFINERY.item().get(), 1, has(STEEL.plateTag), "APA", "SFS", "SCS",
                'A', ALUMINUM.plateTag, 'P', FLUID_PIPE.get(), 'S', STEEL.plateTag, 'F', Items.BLAST_FURNACE, 'C', BASIC_CIRCUIT.get());
        shaped(out, FLUID_TANK.get(), 1, has(STEEL.plateTag), "SSS", "G G", "SSS",
                'S', STEEL.plateTag, 'G', Tags.Items.GLASS_PANES_COLORLESS);
        shaped(out, PRESSURIZED_TANK.get(), 1, has(TITANIUM.plateTag), "TTT", "G G", "TST",
                'T', TITANIUM.plateTag, 'G', Tags.Items.GLASS_PANES_COLORLESS, 'S', SILICONE_SEAL.get());
        shaped(out, CRYOGENIC_TANK.get(), 1, has(AL_LI.plateTag), "FAF", "G G", "FAF",
                'F', ORANGE_FOAM_INSULATION.get(), 'A', AL_LI.plateTag, 'G', Tags.Items.GLASS_PANES_COLORLESS);
    }
}
