package com.pizzaparker07.spacebreaker.content.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loot function {@code spacebreaker:random_tech_disk}: turns the stack into a tech disk for a random technology
 * matching the filters. Use it on {@code spacebreaker:tech_disk} entries in dungeon loot tables:
 * <pre>
 * { "function": "spacebreaker:random_tech_disk",
 *   "techs": ["spacebreaker:basic_rocketry"],      // explicit pool (optional)
 *   "categories": ["propulsion"],                  // or filter by branch (optional)
 *   "min_tier": 1, "max_tier": 1,                  // and by tier (optional)
 *   "samples": ["spacebreaker:moon"] }             // only techs related to these sample kinds (optional)
 * </pre>
 * If nothing matches, the disk stays blank-free: it falls back to any tier-1 technology.
 */
public class TechDiskLootFunction extends LootItemConditionalFunction {
    public static final MapCodec<TechDiskLootFunction> CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i).and(i.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("techs", List.of()).forGetter(f -> f.techs),
            Codec.STRING.listOf().optionalFieldOf("categories", List.of()).forGetter(f -> f.categories),
            Codec.INT.optionalFieldOf("min_tier", 1).forGetter(f -> f.minTier),
            Codec.INT.optionalFieldOf("max_tier", 99).forGetter(f -> f.maxTier),
            ResourceLocation.CODEC.listOf().optionalFieldOf("samples", List.of()).forGetter(f -> f.samples)
    )).apply(i, TechDiskLootFunction::new));

    private final List<ResourceLocation> techs;
    private final List<String> categories;
    private final int minTier;
    private final int maxTier;
    private final List<ResourceLocation> samples;

    public TechDiskLootFunction(List<LootItemCondition> conditions, List<ResourceLocation> techs, List<String> categories,
                                int minTier, int maxTier, List<ResourceLocation> samples) {
        super(conditions);
        this.techs = techs;
        this.categories = categories;
        this.minTier = minTier;
        this.maxTier = maxTier;
        this.samples = samples;
    }

    @Override
    public LootItemFunctionType<TechDiskLootFunction> getType() {
        return ResearchModule.RANDOM_TECH_DISK.get();
    }

    /** The technologies this function can roll in the given registry state. */
    public List<ResourceLocation> candidates(Map<ResourceLocation, TechNode> all) {
        List<ResourceLocation> out = new ArrayList<>();
        for (Map.Entry<ResourceLocation, TechNode> e : all.entrySet()) {
            TechNode n = e.getValue();
            if (!techs.isEmpty() && !techs.contains(e.getKey())) {
                continue;
            }
            if (!categories.isEmpty() && !categories.contains(n.category())) {
                continue;
            }
            if (n.tier() < minTier || n.tier() > maxTier) {
                continue;
            }
            if (!samples.isEmpty() && n.samples().stream().noneMatch(samples::contains)) {
                continue;
            }
            out.add(e.getKey());
        }
        if (out.isEmpty()) {
            all.forEach((id, n) -> {
                if (n.tier() <= 1) {
                    out.add(id);
                }
            });
        }
        return out;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        List<ResourceLocation> options = candidates(Tech.all(context.getLevel().registryAccess()));
        if (options.isEmpty()) {
            return stack;
        }
        ResourceLocation pick = options.get(context.getRandom().nextInt(options.size()));
        ItemStack disk = Research.disk(pick);
        disk.setCount(1);
        return disk;
    }

    public static Builder<?> randomDisk(List<ResourceLocation> techs, List<String> categories, int minTier, int maxTier) {
        return simpleBuilder(c -> new TechDiskLootFunction(c, techs, categories, minTier, maxTier, List.of()));
    }
}
