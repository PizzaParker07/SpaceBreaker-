package com.pizzaparker07.spacebreaker.testmod.outer;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.BeltModule;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.jupiter.JupiterModule;
import com.pizzaparker07.spacebreaker.content.machines.IsruRecipes;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe;
import com.pizzaparker07.spacebreaker.content.neptune.NeptuneModule;
import com.pizzaparker07.spacebreaker.content.pluto.PlutoModule;
import com.pizzaparker07.spacebreaker.content.saturn.SaturnModule;
import com.pizzaparker07.spacebreaker.content.uranus.UranusModule;
import com.pizzaparker07.spacebreaker.testmod.mars.WorldTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Rocket fuel from the outer worlds: every world's water ice melts to water in the ice melter, water splits into hydrogen
 * and oxygen in the electrolyser, and both liquefy for the tanks. Titan adds methane from its lakes (methalox).
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OuterFuelTests {
    @GameTest(template = WorldTests.EMPTY)
    public static void outerWorldIceMeltsIntoRocketFuel(GameTestHelper helper) {
        var rm = helper.getLevel().getRecipeManager();
        List<RecipeHolder<ProcessingRecipe>> melting = rm.getAllRecipesFor(MachinesModule.MELTING.type().get());
        Map<String, ItemLike> ices = Map.of(
                "ceres", BeltModule.CEREAN_ICE.get(), "europa", JupiterModule.EUROPAN_ICE.get(), "ganymede", JupiterModule.GROOVED_ICE.get(),
                "callisto", JupiterModule.BRIGHT_FROST.get(),
                "titan", SaturnModule.TITAN_ICE.base().get(), "enceladus", SaturnModule.ENCELADUS_ICE.get(),
                "miranda", UranusModule.MIRANDAN_ICE.base().get(), "triton", NeptuneModule.TRITON_ICE.base().get(),
                "pluto", PlutoModule.PLUTONIAN_ICE.base().get(), "charon", PlutoModule.CHARON_ICE.get());
        var bodies = helper.getLevel().registryAccess().registryOrThrow(com.pizzaparker07.spacebreaker.core.body.Bodies.REGISTRY);
        ices.forEach((world, ice) -> {
            helper.assertTrue(bodies.getHolderOrThrow(com.pizzaparker07.spacebreaker.core.body.Bodies.key(world))
                    .is(com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations.REFUEL), world + " is marked as a world where fuel can be made");
            ItemStack stack = new ItemStack(ice);
            helper.assertTrue(stack.is(IsruRecipes.WATER_ICES), world + " ice is tagged c:ices/water");
            boolean melts = melting.stream().anyMatch(r -> !r.value().ingredients.isEmpty() && r.value().ingredients.getFirst().test(stack)
                    && r.value().fluidResults.stream().anyMatch(f -> f.is(Fluids.WATER)));
            helper.assertTrue(melts, world + " ice melts to water");
        });
        var electrolysis = rm.getAllRecipesFor(MachinesModule.ELECTROLYSIS.type().get());
        helper.assertTrue(electrolysis.stream().anyMatch(r -> r.value().fluidInput.map(f -> f.test(new net.neoforged.neoforge.fluids.FluidStack(Fluids.WATER, 1000)))
                .orElse(false)), "water splits in the electrolyser");
        var liquefaction = rm.getAllRecipesFor(MachinesModule.LIQUEFACTION.type().get());
        helper.assertTrue(!liquefaction.isEmpty(), "gases liquefy for the tanks");
        // Titan: the lakes are liquid methane, ready to burn with oxygen from the ice
        helper.assertTrue(SaturnModule.TITAN_METHANE.get().fluid.isSame(FluidsModule.LIQUID_METHANE.source.get()), "Titan lakes hold liquid methane");
        helper.succeed();
    }
}
