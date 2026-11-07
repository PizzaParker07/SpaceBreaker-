package com.pizzaparker07.spacebreaker.testmod.qa;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.machine.CombustionGeneratorBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Infinite-resource audit over every recipe:
 * <ul>
 *   <li><b>energy</b>: a machine recipe whose outputs burn in the combustion generator for more FE than the recipe costs
 *       (machine energy plus the burn value of its inputs) is a free-power loop;</li>
 *   <li><b>multiplication</b>: a cycle of two or three recipes that turns one item (or fluid) back into more of itself is a
 *       duplication loop; items and fluids are kept apart so units always match.</li>
 * </ul>
 * Writes {@code run-gametest/qa/loops.md}; fails on any loop not listed in {@link #KNOWN}.
 */
@GameTestHolder(Spacebreaker.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LoopAuditTests {
    /** Accepted loops (with the reason), by the key the report prints. Empty: none known. */
    static final Set<String> KNOWN = Set.of();

    /** One step of a cycle: from one resource to another through a recipe, with the amounts. */
    record Edge(String from, String to, double in, double out, String recipe, List<String> others) {
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void noFreeEnergyOrDuplicationLoops(GameTestHelper h) {
        var level = h.getLevel();
        var access = level.registryAccess();
        List<String> energy = new ArrayList<>();
        Map<String, List<Edge>> graph = new HashMap<>();
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            Recipe<?> r = holder.value();
            String id = holder.id().toString();
            Map<String, Double> in = new LinkedHashMap<>();
            Map<String, Double> out = new LinkedHashMap<>();
            if (r instanceof ProcessingRecipe pr) {
                pr.ingredients.forEach(s -> add(in, s.ingredient(), s.count()));
                pr.fluidInput.ifPresent(f -> addFluids(in, f));
                pr.fluidInput2.ifPresent(f -> addFluids(in, f));
                pr.results.forEach(s -> out.merge(key(s.getItem()), (double) s.getCount(), Double::sum));
                pr.bonus.ifPresent(s -> out.merge(key(s.getItem()), s.getCount() * (double) pr.bonusChance, Double::sum));
                pr.fluidResults.forEach(s -> out.merge(key(s.getFluid()), (double) s.getAmount(), Double::sum));
                // energy balance
                double cost = (double) pr.energy * pr.time;
                double inBurn = 0;
                for (var s : pr.ingredients) {
                    inBurn += s.count() * minBurn(s.ingredient());
                }
                double outBurn = 0;
                for (ItemStack s : pr.results) {
                    outBurn += s.getCount() * burn(s);
                }
                if (outBurn > cost + inBurn && outBurn > 0) {
                    energy.add("energy:" + id + " | outputs burn for " + Math.round(outBurn) + " FE, recipe costs "
                            + Math.round(cost) + " FE + inputs worth " + Math.round(inBurn) + " FE");
                }
            } else {
                ItemStack result;
                try {
                    result = r.getResultItem(access);
                } catch (RuntimeException e) {
                    continue;
                }
                if (result.isEmpty() || r.getIngredients().isEmpty()) {
                    continue;
                }
                for (Ingredient i : r.getIngredients()) {
                    if (!i.isEmpty()) {
                        add(in, i, 1);
                    }
                }
                out.merge(key(result.getItem()), (double) result.getCount(), Double::sum);
            }
            for (var a : in.entrySet()) {
                for (var b : out.entrySet()) {
                    if (a.getKey().equals(b.getKey())) {
                        continue;
                    }
                    List<String> others = new ArrayList<>();
                    in.keySet().forEach(k -> {
                        if (!k.equals(a.getKey()) && !k.startsWith("alt:")) {
                            others.add(k);
                        }
                    });
                    graph.computeIfAbsent(a.getKey(), k -> new ArrayList<>())
                            .add(new Edge(a.getKey(), b.getKey(), a.getValue(), b.getValue(), id, others));
                }
            }
        }
        checked = 0;
        // cycles of length 2 and 3 that end with more than they started with
        Set<String> loops = new TreeSet<>();
        for (String start : graph.keySet()) {
            for (Edge e1 : graph.get(start)) {
                for (Edge e2 : graph.getOrDefault(e1.to(), List.of())) {
                    if (e2.to().equals(start) && kindOf(start).equals(kindOf(e1.to()))) {
                        check(loops, List.of(e1, e2));
                    }
                    for (Edge e3 : graph.getOrDefault(e2.to(), List.of())) {
                        if (e3.to().equals(start) && !e2.to().equals(start) && kindOf(start).equals(kindOf(e1.to()))
                                && kindOf(start).equals(kindOf(e2.to()))) {
                            check(loops, List.of(e1, e2, e3));
                        }
                    }
                }
            }
        }
        int edges = graph.values().stream().mapToInt(List::size).sum();
        StringBuilder md = new StringBuilder("## Infinite-resource audit\n\n");
        md.append("Recipe graph: ").append(graph.size()).append(" resources, ").append(edges).append(" input→output edges, ")
                .append(checked).append(" closed cycles of 2–3 recipes checked.\n\n");
        md.append("Free-energy recipes (outputs burn for more than they cost): ").append(energy.isEmpty() ? "none" : "").append('\n');
        energy.forEach(l -> md.append("- ").append(l).append('\n'));
        md.append("\nMultiplication cycles (2–3 recipes ending with more of the starting item/fluid): ").append(loops.isEmpty() ? "none" : "").append('\n');
        loops.forEach(l -> md.append("- ").append(l).append('\n'));
        ProgressionReport.write("loops.md", md.toString());
        List<String> fresh = new ArrayList<>();
        for (String l : energy) {
            if (!KNOWN.contains(l.substring(0, l.indexOf(" | ")))) {
                fresh.add(l);
            }
        }
        for (String l : loops) {
            if (!KNOWN.contains(l.substring(0, l.indexOf(" | ")))) {
                fresh.add(l);
            }
        }
        h.assertTrue(fresh.isEmpty(), "resource loops: " + fresh);
        h.succeed();
    }

    private static int checked;

    private static void check(Set<String> loops, List<Edge> cycle) {
        checked++;
        double gain = 1;
        for (Edge e : cycle) {
            gain *= e.out() / e.in();
        }
        if (gain <= 1.0001) {
            return;
        }
        // canonical key: the recipes, sorted, so each cycle is reported once
        TreeSet<String> recipes = new TreeSet<>();
        cycle.forEach(e -> recipes.add(e.recipe()));
        StringBuilder path = new StringBuilder();
        Set<String> extra = new LinkedHashSet<>();
        for (Edge e : cycle) {
            path.append(e.from()).append(" ×").append(fmt(e.in())).append(" → [").append(e.recipe()).append("] → ×")
                    .append(fmt(e.out())).append(' ');
            extra.addAll(e.others());
        }
        loops.add("loop:" + String.join("+", recipes) + " | gain ×" + fmt(gain) + ": " + path.toString().trim()
                + (extra.isEmpty() ? "" : "; also consumes " + extra));
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format(java.util.Locale.ROOT, "%.2f", d);
    }

    private static String kindOf(String key) {
        return key.substring(0, key.indexOf(':'));
    }

    private static void add(Map<String, Double> in, Ingredient ingredient, double count) {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) {
            return;
        }
        for (ItemStack s : items) {
            in.merge(key(s.getItem()), count, Double::sum);
        }
    }

    private static void addFluids(Map<String, Double> in, net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient f) {
        for (var s : f.getFluids()) {
            in.merge(key(s.getFluid()), (double) f.amount(), Double::sum);
        }
    }

    private static String key(Item i) {
        return "item:" + BuiltInRegistries.ITEM.getKey(i);
    }

    private static String key(Fluid f) {
        Fluid src = f instanceof FlowingFluid ff ? ff.getSource() : f;
        return "fluid:" + BuiltInRegistries.FLUID.getKey(src);
    }

    private static double burn(ItemStack s) {
        return (double) s.getBurnTime(RecipeType.SMELTING) * CombustionGeneratorBlockEntity.OUTPUT;
    }

    private static double minBurn(Ingredient i) {
        double min = Double.MAX_VALUE;
        for (ItemStack s : i.getItems()) {
            min = Math.min(min, burn(s));
        }
        return min == Double.MAX_VALUE ? 0 : min;
    }

    private LoopAuditTests() {
    }
}
