package com.pizzaparker07.spacebreaker.compat.recipes;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.MachineEntry;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.suit.SpaceSuitItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.SuitModuleItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitUpgradeRecipe;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import com.pizzaparker07.spacebreaker.core.tech.TechRequirement;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Everything a recipe viewer (JEI, EMI, later REI) shows for Spacebreaker, independent of the viewer: the processing
 * categories with their machine, the recipes, energy/time text, the research a recipe needs, the suit-module
 * smithing combinations and the info pages for things that have no recipe (rocket assembly, atmosphere processor).
 * The viewer plugins in compat/jei and compat/emi only lay these out. Common code: also used by GameTests.
 */
public final class RecipeViewerData {
    /** One viewer category per processing recipe kind, with the machine that runs it (category icon + catalyst). */
    public record Category(String id, ProcessingRecipe.Kind kind, Supplier<? extends ItemLike> machine) {
        public ResourceLocation uid() {
            return Spacebreaker.id(id);
        }

        public String titleKey() {
            return "recipe_viewer.spacebreaker." + id;
        }
    }

    public static final List<Category> CATEGORIES = List.of(
            category(MachinesModule.CRUSHING, MachinesModule.CRUSHER),
            category(MachinesModule.ARC_SMELTING, MachinesModule.ARC_FURNACE),
            category(MachinesModule.PRESSING, MachinesModule.PRESS),
            category(MachinesModule.FABRICATING, MachinesModule.FABRICATOR),
            category(MachinesModule.REFINING, MachinesModule.REFINERY),
            category(MachinesModule.ELECTROLYSIS, MachinesModule.ELECTROLYZER),
            category(MachinesModule.LIQUEFACTION, MachinesModule.LIQUEFIER),
            category(MachinesModule.SABATIER, MachinesModule.SABATIER_REACTOR),
            category(MachinesModule.REGOLITH_PROCESSING, MachinesModule.REGOLITH_HEATER),
            category(MachinesModule.MELTING, MachinesModule.ICE_MELTER),
            category(MachinesModule.CHEMICAL_REACTION, MachinesModule.CHEMICAL_REACTOR));

    /** The arc furnace also smelts anything a furnace can (twice as fast): a catalyst for vanilla smelting. */
    public static final Supplier<? extends ItemLike> EXTRA_SMELTER = MachinesModule.ARC_FURNACE.item();

    public static final String SUIT_MODULES = "suit_modules";
    public static final String KEY_SUIT_MODULES = "recipe_viewer.spacebreaker." + SUIT_MODULES;
    public static final String KEY_ENERGY = "recipe_viewer.spacebreaker.energy";
    public static final String KEY_TIME = "recipe_viewer.spacebreaker.time";
    public static final String KEY_CHANCE = "recipe_viewer.spacebreaker.chance";
    public static final String KEY_TOOL = "recipe_viewer.spacebreaker.tool";
    public static final String KEY_INFO_ASSEMBLER = "recipe_viewer.spacebreaker.info.rocket_assembler";
    public static final String KEY_INFO_ATMOSPHERE = "recipe_viewer.spacebreaker.info.atmosphere_processor";
    /** "Research needed: %s" */
    public static final String KEY_REQUIRES = "recipe_viewer.spacebreaker.research";

    private RecipeViewerData() {
    }

    private static Category category(ProcessingRecipe.Kind kind, MachineEntry<?, ?> machine) {
        return new Category(kind.name(), kind, machine.item());
    }

    public static Optional<Category> category(ProcessingRecipe.Kind kind) {
        return CATEGORIES.stream().filter(c -> c.kind() == kind).findFirst();
    }

    /** All recipes of a category, sorted by id so the viewer's pages are stable. */
    public static List<RecipeHolder<ProcessingRecipe>> recipes(RecipeManager manager, Category category) {
        List<RecipeHolder<ProcessingRecipe>> list = new ArrayList<>(manager.getAllRecipesFor(category.kind().type().get()));
        list.sort((a, b) -> a.id().compareTo(b.id()));
        return list;
    }

    /** "40 FE/t · 8,000 FE" */
    public static Component energyLine(ProcessingRecipe recipe) {
        return Component.translatable(KEY_ENERGY, recipe.energy, String.format("%,d", (long) recipe.energy * recipe.time));
    }

    /** "10.0 s" */
    public static Component timeLine(ProcessingRecipe recipe) {
        return Component.translatable(KEY_TIME, String.format("%.1f", recipe.time / 20.0));
    }

    public static Component chanceLine(float chance) {
        return Component.translatable(KEY_CHANCE, Math.round(chance * 100)).withStyle(ChatFormatting.GOLD);
    }

    /**
     * The research a recipe needs: technologies whose {@code unlocks} list the recipe id, or else its first result
     * item (the gating API treats ids no technology lists as always unlocked).
     */
    public static TechRequirement requirement(RegistryAccess access, ResourceLocation recipeId, ItemStack result) {
        TechRequirement byRecipe = TechRequirement.forUnlock(access, recipeId);
        if (!byRecipe.isEmpty() || result.isEmpty()) {
            return byRecipe;
        }
        return TechRequirement.forUnlock(access, BuiltInRegistries.ITEM.getKey(result.getItem()));
    }

    public static TechRequirement requirement(RegistryAccess access, RecipeHolder<ProcessingRecipe> holder) {
        ProcessingRecipe r = holder.value();
        return requirement(access, holder.id(), r.results.isEmpty() ? ItemStack.EMPTY : r.results.get(0));
    }

    /** "Requires technology: Metallurgy, ..." or null when not gated. */
    public static Component requirementLine(TechRequirement req) {
        if (req.isEmpty()) {
            return null;
        }
        MutableComponent names = Component.empty();
        for (int i = 0; i < req.techs().size(); i++) {
            if (i > 0) {
                names.append(", ");
            }
            names.append(Component.translatable(TechNode.nameKey(req.techs().get(i))));
        }
        return Component.translatable(KEY_REQUIRES, names).withStyle(ChatFormatting.DARK_RED);
    }

    /** One suit-module fitting at the smithing table: template + piece + module = fitted piece. */
    public record SuitUpgradeDisplay(ItemStack template, ItemStack base, ItemStack module, ItemStack result) {
        public ResourceLocation id() {
            ResourceLocation b = BuiltInRegistries.ITEM.getKey(base.getItem());
            ResourceLocation m = BuiltInRegistries.ITEM.getKey(module.getItem());
            return Spacebreaker.id("suit_modules/" + b.getPath() + "/" + m.getPath());
        }
    }

    /** Every suit piece × every module that fits it (SuitUpgradeRecipe itself is one dynamic recipe). */
    public static List<SuitUpgradeDisplay> suitUpgrades() {
        List<Item> pieces = new ArrayList<>();
        List<Item> modules = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof SpaceSuitItem) {
                pieces.add(item);
            } else if (item instanceof SuitModuleItem) {
                modules.add(item);
            }
        }
        List<SuitUpgradeDisplay> out = new ArrayList<>();
        ItemStack template = new ItemStack(SuitModule.SUIT_UPGRADE_TEMPLATE.get());
        for (Item piece : pieces) {
            for (Item module : modules) {
                ItemStack base = new ItemStack(piece);
                ItemStack mod = new ItemStack(module);
                ItemStack result = SuitUpgradeRecipe.result(base, mod);
                if (result != null) {
                    out.add(new SuitUpgradeDisplay(template, base, mod, result));
                }
            }
        }
        return out;
    }

    /** Info pages for things without a recipe. */
    public record InfoPage(Supplier<? extends ItemLike> item, String key) {
    }

    public static final List<InfoPage> INFO_PAGES = List.of(
            new InfoPage(RocketryModule.ROCKET_ASSEMBLER_ITEM, KEY_INFO_ASSEMBLER),
            new InfoPage(MachinesModule.ATMOSPHERE_PROCESSOR.item(), KEY_INFO_ATMOSPHERE));
}
