package com.pizzaparker07.spacebreaker.testmod.compat;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.compat.CompatModule;
import com.pizzaparker07.spacebreaker.compat.recipes.RecipeViewerData;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/** Recipe viewer data (shared by the JEI and EMI plugins), checked without either viewer installed. */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecipeViewerGameTests {
    @GameTest(template = "empty")
    public static void everyProcessingKindHasACategory(GameTestHelper helper) throws IllegalAccessException {
        Set<ProcessingRecipe.Kind> kinds = new HashSet<>();
        for (Field f : MachinesModule.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == ProcessingRecipe.Kind.class) {
                kinds.add((ProcessingRecipe.Kind) f.get(null));
            }
        }
        helper.assertTrue(kinds.size() >= 11, "expected the processing kinds of MachinesModule, found " + kinds.size());
        for (ProcessingRecipe.Kind kind : kinds) {
            helper.assertTrue(RecipeViewerData.category(kind).isPresent(), "no recipe viewer category for " + kind.name());
        }
        for (RecipeViewerData.Category c : RecipeViewerData.CATEGORIES) {
            helper.assertTrue(CompatModule.hasTitle(c.id()), "no title for category " + c.id());
            helper.assertFalse(c.machine().get().asItem() == Items.AIR, "category " + c.id() + " has no machine item");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyProcessingRecipeIsShown(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        int shown = 0;
        int gated = 0;
        for (RecipeViewerData.Category c : RecipeViewerData.CATEGORIES) {
            for (var holder : RecipeViewerData.recipes(manager, c)) {
                shown++;
                helper.assertTrue(holder.value().kind() == c.kind(), holder.id() + " is listed under " + c.id());
                if (!RecipeViewerData.requirement(helper.getLevel().registryAccess(), holder).isEmpty()) {
                    gated++;
                }
            }
        }
        long all = manager.getRecipes().stream().filter(h -> h.value() instanceof ProcessingRecipe).count();
        helper.assertTrue(shown == all, "viewer shows " + shown + " of " + all + " processing recipes");
        helper.assertTrue(shown > 20, "expected the industry recipes, found " + shown);
        helper.assertTrue(gated > 0, "no recipe shows a research requirement");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void suitModulesAndInfoPages(GameTestHelper helper) {
        var upgrades = RecipeViewerData.suitUpgrades();
        helper.assertTrue(upgrades.size() >= 4, "expected suit-module combinations, found " + upgrades.size());
        Set<String> ids = new HashSet<>();
        for (var d : upgrades) {
            helper.assertTrue(d.result().has(SuitModule.SUIT_UPGRADE.get()), d.id() + " result has no module");
            helper.assertTrue(ids.add(d.id().toString()), "duplicate id " + d.id());
        }
        for (var page : RecipeViewerData.INFO_PAGES) {
            helper.assertFalse(page.item().get().asItem() == Items.AIR, "info page without item: " + page.key());
        }
        helper.succeed();
    }
}
