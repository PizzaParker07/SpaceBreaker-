package com.pizzaparker07.spacebreaker.testmod.industry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.MachineEntry;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe;
import com.pizzaparker07.spacebreaker.core.tech.TechRequirement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Progression guard: with no technology known, a player must be able to reach the Research Lab and a power source
 * (Combustion Generator) from vanilla materials and mined ores, using crafting, smelting and only those machines
 * that are not research-gated. Catches loops like "the lab needs power, power needs a circuit, the circuit needs
 * wire, wire needs the press, the press needs research".
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IndustryBootstrapTest {
    @GameTest(template = "empty")
    public static void researchLabAndPowerAreReachableWithoutResearch(GameTestHelper h) {
        expect(h, reachable(h, true), "before any research", "research_lab", "combustion_generator", "basic_circuit", "copper_wire",
                "electric_motor", "crusher", "iron_rod", "battery_bank");
    }

    @GameTest(template = "empty")
    public static void everyMachineIsReachableOnceResearched(GameTestHelper h) {
        java.util.List<String> machines = new java.util.ArrayList<>();
        MachinesModule.MACHINES.forEach(m -> machines.add(BuiltInRegistries.ITEM.getKey(m.item().get()).getPath()));
        machines.addAll(java.util.List.of("steel_ingot", "plate_press_die", "wire_press_die", "rod_press_die", "solar_panel",
                "advanced_circuit", "fluid_tank", "pressurized_tank", "cryogenic_tank", "copper_cable", "hv_cable", "fluid_pipe", "gas_pipe"));
        expect(h, reachable(h, false), "with research", machines.toArray(String[]::new));
    }

    /**
     * Liquid oxygen on Earth: analysing Earth samples alone must lead to electrolysis and cryogenics, and then water ->
     * electrolyzer -> oxygen -> liquefier -> LOX with only machines those technologies unlock.
     */
    @GameTest(template = "empty")
    public static void loxIsReachableThroughEarthResearch(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        var techs = com.pizzaparker07.spacebreaker.core.tech.Tech.all(access);
        Set<ResourceLocation> known = new HashSet<>();
        boolean grew = true;
        while (grew) {
            grew = false;
            for (var e : techs.entrySet()) {
                var node = e.getValue();
                if (!known.contains(e.getKey()) && node.samples().contains(com.pizzaparker07.spacebreaker.content.research.ResearchTree.EARTH)
                        && known.containsAll(node.requires())) {
                    known.add(e.getKey());
                    grew = true;
                }
            }
        }
        for (String t : java.util.List.of("electrolysis", "cryogenics")) {
            h.assertTrue(known.contains(Spacebreaker.id(t)), "Earth samples cannot reveal " + t + "; known from Earth: " + known);
        }
        Set<Item> have = reachable(h, false);
        h.assertTrue(have.contains(MachinesModule.ELECTROLYZER.item().get()) && have.contains(MachinesModule.LIQUEFIER.item().get()),
                "electrolyzer and liquefier must be craftable");
        for (var m : java.util.List.of(MachinesModule.ELECTROLYZER, MachinesModule.LIQUEFIER)) {
            var req = TechRequirement.forUnlock(access, BuiltInRegistries.BLOCK.getKey(m.block().get()));
            h.assertTrue(known.containsAll(req.techs()), BuiltInRegistries.BLOCK.getKey(m.block().get()) + " needs " + req.techs());
        }
        // fluid chain: water from a bucket; follow electrolysis and liquefaction recipes to LOX
        Set<net.minecraft.world.level.material.Fluid> fluids = new HashSet<>(java.util.List.of(net.minecraft.world.level.material.Fluids.WATER));
        boolean more = true;
        while (more) {
            more = false;
            for (RecipeHolder<?> holder : h.getLevel().getRecipeManager().getRecipes()) {
                if (!(holder.value() instanceof ProcessingRecipe pr) || pr.fluidInput.isEmpty() || !pr.ingredients.isEmpty()
                        || !(pr.kind() == MachinesModule.ELECTROLYSIS || pr.kind() == MachinesModule.LIQUEFACTION)) {
                    continue;
                }
                if (fluids.stream().noneMatch(f -> pr.fluidInput.get().ingredient().test(new net.neoforged.neoforge.fluids.FluidStack(f, 1000)))) {
                    continue;
                }
                for (var out : pr.fluidResults) {
                    more |= fluids.add(out.getFluid());
                }
            }
        }
        h.assertTrue(fluids.stream().anyMatch(f -> f.is(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.TAG_LOX)),
                "no recipe path from water to LOX: " + fluids);
        h.succeed();
    }

    private static void expect(GameTestHelper h, Set<Item> have, String when, String... goals) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (String goal : goals) {
            if (!have.contains(BuiltInRegistries.ITEM.get(Spacebreaker.id(goal)))) {
                missing.add(goal);
            }
        }
        h.assertTrue(missing.isEmpty(), "cannot be made " + when + ": " + missing);
        h.succeed();
    }

    /** Fixed point over crafting, smelting and machine recipes; gated = machines listed in a tech's unlocks do not run. */
    private static Set<Item> reachable(GameTestHelper h, boolean gated) {
        var access = h.getLevel().registryAccess();
        var recipes = h.getLevel().getRecipeManager().getRecipes();
        Set<Item> have = new HashSet<>();
        BuiltInRegistries.ITEM.forEach(i -> {
            if (BuiltInRegistries.ITEM.getKey(i).getNamespace().equals("minecraft")) {
                have.add(i);
            }
        });
        for (TagKey<Item> mined : java.util.List.of(Tags.Items.ORES, Tags.Items.RAW_MATERIALS)) {
            BuiltInRegistries.ITEM.getTag(mined).ifPresent(set -> set.forEach(hd -> have.add(hd.value())));
        }
        Map<ProcessingRecipe.Kind, MachineEntry<?, ?>> machineFor = new HashMap<>();
        machineFor.put(MachinesModule.CRUSHING, MachinesModule.CRUSHER);
        machineFor.put(MachinesModule.ARC_SMELTING, MachinesModule.ARC_FURNACE);
        machineFor.put(MachinesModule.PRESSING, MachinesModule.PRESS);
        machineFor.put(MachinesModule.FABRICATING, MachinesModule.FABRICATOR);
        machineFor.put(MachinesModule.REFINING, MachinesModule.REFINERY);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (RecipeHolder<?> holder : recipes) {
                Recipe<?> r = holder.value();
                Item result;
                if (r instanceof CraftingRecipe || r instanceof AbstractCookingRecipe) {
                    result = r.getResultItem(access).getItem();
                    if (have.contains(result) || !all(r.getIngredients(), have)) {
                        continue;
                    }
                } else if (r instanceof ProcessingRecipe pr && machineFor.containsKey(pr.kind()) && !pr.results.isEmpty()) {
                    MachineEntry<?, ?> m = machineFor.get(pr.kind());
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(m.block().get());
                    if (!have.contains(m.item().get()) || (gated && !TechRequirement.forUnlock(access, id).isEmpty())) {
                        continue;
                    }
                    // fluids: the refinery's crude oil comes from a pumpjack, which must be reachable too
                    if (pr.fluidInput.isPresent() && !(pr.kind() == MachinesModule.REFINING && have.contains(MachinesModule.PUMPJACK.item().get()))) {
                        continue;
                    }
                    if (pr.tool.isPresent() && !any(pr.tool.get(), have)) {
                        continue;
                    }
                    if (!pr.ingredients.stream().allMatch(s -> any(s.ingredient(), have))) {
                        continue;
                    }
                    result = pr.results.get(0).getItem();
                    if (have.contains(result)) {
                        continue;
                    }
                } else {
                    continue;
                }
                have.add(result);
                changed = true;
            }
        }
        return have;
    }

    @GameTest(template = "empty")
    public static void handWireKeepsAndWearsTheShears(GameTestHelper h) {
        var holder = h.getLevel().getRecipeManager().byKey(Spacebreaker.id("copper_wire_by_hand"))
                .orElseThrow(() -> new AssertionError("copper_wire_by_hand missing"));
        var recipe = (net.minecraft.world.item.crafting.CraftingRecipe) holder.value();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
                new net.minecraft.world.item.ItemStack(com.pizzaparker07.spacebreaker.content.earth.EarthModule.COPPER.plate.get()),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHEARS)));
        h.assertTrue(recipe.matches(input, h.getLevel()), "plate + shears should match");
        var out = recipe.assemble(input, h.getLevel().registryAccess());
        h.assertTrue(out.getCount() == 3 && out.is(com.pizzaparker07.spacebreaker.content.earth.EarthModule.COPPER.wire.get()), "3 copper wire");
        var rest = recipe.getRemainingItems(input);
        h.assertTrue(rest.get(1).is(net.minecraft.world.item.Items.SHEARS) && rest.get(1).getDamageValue() == 1, "shears stay, 1 damage: " + rest.get(1));
        h.assertTrue(rest.get(0).isEmpty(), "the plate is used up");
        h.succeed();
    }

    private static boolean all(java.util.List<Ingredient> ingredients, Set<Item> have) {
        return ingredients.stream().allMatch(i -> i.isEmpty() || any(i, have));
    }

    private static boolean any(Ingredient ingredient, Set<Item> have) {
        for (var stack : ingredient.getItems()) {
            if (have.contains(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    private IndustryBootstrapTest() {
    }
}
