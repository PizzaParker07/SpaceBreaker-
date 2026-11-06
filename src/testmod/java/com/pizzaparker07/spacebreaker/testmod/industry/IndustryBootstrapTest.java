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
        var access = h.getLevel().registryAccess();
        var recipes = h.getLevel().getRecipeManager().getRecipes();
        Set<Item> have = new HashSet<>();
        // the world: every vanilla item, plus everything mined from ores
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
                } else if (r instanceof ProcessingRecipe pr && machineFor.containsKey(pr.kind()) && pr.fluidInput.isEmpty() && !pr.results.isEmpty()) {
                    MachineEntry<?, ?> m = machineFor.get(pr.kind());
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(m.block().get());
                    if (!have.contains(m.item().get()) || !TechRequirement.forUnlock(access, id).isEmpty()) {
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
        for (String goal : new String[]{"research_lab", "combustion_generator", "basic_circuit", "copper_wire"}) {
            Item item = BuiltInRegistries.ITEM.get(Spacebreaker.id(goal));
            h.assertTrue(have.contains(item), "spacebreaker:" + goal + " cannot be made before any research");
        }
        h.succeed();
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
        h.assertTrue(out.getCount() == 2 && out.is(com.pizzaparker07.spacebreaker.content.earth.EarthModule.COPPER.wire.get()), "2 copper wire");
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
