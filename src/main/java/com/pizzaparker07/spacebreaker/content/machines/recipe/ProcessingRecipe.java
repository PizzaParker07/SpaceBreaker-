package com.pizzaparker07.spacebreaker.content.machines.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * One recipe class for every Spacebreaker processing machine (crushing, arc smelting, pressing, fabricating,
 * refining, electrolysis, liquefaction). Each machine has its own recipe type; other areas add recipes of a
 * type simply by shipping JSON (or with {@link ProcessingRecipeBuilder} in datagen).
 *
 * <pre>
 * {
 *   "type": "spacebreaker:fabricating",
 *   "ingredients": [{"tag": "c:plates/titanium", "count": 4}, {"item": "spacebreaker:advanced_circuit"}],
 *   "tool": {"tag": "spacebreaker:press_dies"},          // optional, not consumed
 *   "fluid_input": {"tag": "c:water", "amount": 1000},  // optional
 *   "fluid_input_2": {"tag": "c:hydrogen", "amount": 2000}, // optional second fluid (two-input machines)
 *   "results": [{"id": "spacebreaker:...", "count": 1}],
 *   "bonus": {"id": "...", "count": 1}, "bonus_chance": 0.5,   // optional
 *   "fluid_results": [{"id": "spacebreaker:hydrogen", "amount": 1000}],
 *   "time": 200, "energy": 40,                            // ticks, FE per tick
 *   "priority": 0                                         // optional: when several recipes match, the highest wins
 * }
 * </pre>
 */
public class ProcessingRecipe implements Recipe<MachineInput> {
    private final Kind kind;
    public final List<SizedIngredient> ingredients;
    public final Optional<Ingredient> tool;
    public final Optional<SizedFluidIngredient> fluidInput;
    public final Optional<SizedFluidIngredient> fluidInput2;
    public final List<ItemStack> results;
    public final Optional<ItemStack> bonus;
    public final float bonusChance;
    public final List<FluidStack> fluidResults;
    public final int time;
    public final int energy;
    /** When several recipes match the same input, the highest priority wins (default 0). */
    public int priority;

    public ProcessingRecipe(Kind kind, List<SizedIngredient> ingredients, Optional<Ingredient> tool, Optional<SizedFluidIngredient> fluidInput,
                            List<ItemStack> results, Optional<ItemStack> bonus, float bonusChance, List<FluidStack> fluidResults, int time, int energy) {
        this(kind, ingredients, tool, fluidInput, Optional.empty(), results, bonus, bonusChance, fluidResults, time, energy);
    }

    public ProcessingRecipe(Kind kind, List<SizedIngredient> ingredients, Optional<Ingredient> tool, Optional<SizedFluidIngredient> fluidInput,
                            Optional<SizedFluidIngredient> fluidInput2, List<ItemStack> results, Optional<ItemStack> bonus, float bonusChance,
                            List<FluidStack> fluidResults, int time, int energy) {
        this.kind = kind;
        this.fluidInput2 = fluidInput2;
        this.ingredients = ingredients;
        this.tool = tool;
        this.fluidInput = fluidInput;
        this.results = results;
        this.bonus = bonus;
        this.bonusChance = bonusChance;
        this.fluidResults = fluidResults;
        this.time = time;
        this.energy = energy;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public boolean matches(MachineInput input, Level level) {
        if (tool.isPresent() && !tool.get().test(input.tool())) {
            return false;
        }
        if (fluidInput2.isPresent()) {
            boolean straight = fluidInput.map(f -> f.test(input.fluid())).orElse(true) && fluidInput2.get().test(input.fluid2());
            boolean swapped = fluidInput.map(f -> f.test(input.fluid2())).orElse(true) && fluidInput2.get().test(input.fluid());
            if (!straight && !swapped) {
                return false;
            }
        } else if (fluidInput.isPresent() && !fluidInput.get().test(input.fluid()) && !fluidInput.get().test(input.fluid2())) {
            return false;
        }
        if (fluidInput.isEmpty() && fluidInput2.isEmpty() && ingredients.isEmpty()) {
            return false;
        }
        return assign(input.items()) != null;
    }

    /** Maps each ingredient to a distinct input slot holding enough items, or null if impossible. */
    public int[] assign(List<ItemStack> items) {
        int[] slotFor = new int[ingredients.size()];
        boolean[] used = new boolean[items.size()];
        return assign(items, 0, slotFor, used) ? slotFor : null;
    }

    private boolean assign(List<ItemStack> items, int i, int[] slotFor, boolean[] used) {
        if (i == ingredients.size()) {
            for (int s = 0; s < items.size(); s++) {
                if (!used[s] && !items.get(s).isEmpty()) {
                    return false; // shapeless: no extra items allowed
                }
            }
            return true;
        }
        SizedIngredient ing = ingredients.get(i);
        for (int s = 0; s < items.size(); s++) {
            if (!used[s] && ing.test(items.get(s))) {
                used[s] = true;
                slotFor[i] = s;
                if (assign(items, i + 1, slotFor, used)) {
                    return true;
                }
                used[s] = false;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(MachineInput input, HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        ingredients.forEach(i -> list.add(i.ingredient()));
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return kind.serializer().get();
    }

    @Override
    public RecipeType<?> getType() {
        return kind.type().get();
    }

    /** A recipe type together with its serializer. */
    public record Kind(String name, Supplier<RecipeType<ProcessingRecipe>> type, Supplier<RecipeSerializer<ProcessingRecipe>> serializer) {
    }

    private static ProcessingRecipe withPriority(ProcessingRecipe r, int priority) {
        r.priority = priority;
        return r;
    }

    public static final class Serializer implements RecipeSerializer<ProcessingRecipe> {
        private final MapCodec<ProcessingRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> stream;

        public Serializer(Supplier<Kind> kind) {
            this.codec = RecordCodecBuilder.mapCodec(i -> i.group(
                    SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(r -> r.ingredients),
                    Ingredient.CODEC_NONEMPTY.optionalFieldOf("tool").forGetter(r -> r.tool),
                    SizedFluidIngredient.FLAT_CODEC.optionalFieldOf("fluid_input").forGetter(r -> r.fluidInput),
                    SizedFluidIngredient.FLAT_CODEC.optionalFieldOf("fluid_input_2").forGetter(r -> r.fluidInput2),
                    ItemStack.CODEC.listOf().optionalFieldOf("results", List.of()).forGetter(r -> r.results),
                    ItemStack.CODEC.optionalFieldOf("bonus").forGetter(r -> r.bonus),
                    Codec.FLOAT.optionalFieldOf("bonus_chance", 0F).forGetter(r -> r.bonusChance),
                    FluidStack.CODEC.listOf().optionalFieldOf("fluid_results", List.of()).forGetter(r -> r.fluidResults),
                    Codec.INT.optionalFieldOf("time", 200).forGetter(r -> r.time),
                    Codec.INT.optionalFieldOf("energy", 20).forGetter(r -> r.energy),
                    Codec.INT.optionalFieldOf("priority", 0).forGetter(r -> r.priority)
            ).apply(i, (a, b, c, c2, d, e, f, g, h, j, p) -> {
                ProcessingRecipe r = new ProcessingRecipe(kind.get(), a, b, c, c2, d, e, f, g, h, j);
                r.priority = p;
                return r;
            }));
            this.stream = StreamCodec.of((buf, r) -> {
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.ingredients);
                ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC).encode(buf, r.tool);
                ByteBufCodecs.optional(SizedFluidIngredient.STREAM_CODEC).encode(buf, r.fluidInput);
                ByteBufCodecs.optional(SizedFluidIngredient.STREAM_CODEC).encode(buf, r.fluidInput2);
                ItemStack.LIST_STREAM_CODEC.encode(buf, r.results);
                ByteBufCodecs.optional(ItemStack.STREAM_CODEC).encode(buf, r.bonus);
                buf.writeFloat(r.bonusChance);
                FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.fluidResults);
                buf.writeVarInt(r.time);
                buf.writeVarInt(r.energy);
                buf.writeVarInt(r.priority);
            }, buf -> withPriority(new ProcessingRecipe(kind.get(),
                    SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC).decode(buf),
                    ByteBufCodecs.optional(SizedFluidIngredient.STREAM_CODEC).decode(buf),
                    ByteBufCodecs.optional(SizedFluidIngredient.STREAM_CODEC).decode(buf),
                    ItemStack.LIST_STREAM_CODEC.decode(buf),
                    ByteBufCodecs.optional(ItemStack.STREAM_CODEC).decode(buf),
                    buf.readFloat(),
                    new ArrayList<>(FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf)),
                    buf.readVarInt(), buf.readVarInt()), buf.readVarInt()));
        }

        @Override
        public MapCodec<ProcessingRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> streamCodec() {
            return stream;
        }
    }
}
