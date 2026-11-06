package com.pizzaparker07.spacebreaker.core.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What may stand at one position of a multiblock. Exactly one of:
 * <ul>
 *   <li>{@code {"block": "ns:id", "properties": {"facing": "north"}}} — a block, optionally with some state
 *       properties (directional properties are given for the structure facing north and rotate with it);</li>
 *   <li>{@code {"tag": "ns:tag"}} — any block of a block tag;</li>
 *   <li>{@code {"any_of": [spec, spec, ...]}} — any of several specs (the first one is shown in holograms).</li>
 * </ul>
 */
public record BlockSpec(Optional<ResourceLocation> block, Map<String, String> properties, Optional<TagKey<Block>> tag, List<BlockSpec> anyOf) {
    public static final BlockSpec AIR = new BlockSpec(Optional.of(ResourceLocation.withDefaultNamespace("air")), Map.of(), Optional.empty(), List.of());

    public static final Codec<BlockSpec> CODEC = Codec.recursive("BlockSpec", self -> RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("block").forGetter(BlockSpec::block),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("properties", Map.of()).forGetter(BlockSpec::properties),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("tag").forGetter(BlockSpec::tag),
            self.listOf().optionalFieldOf("any_of", List.of()).forGetter(BlockSpec::anyOf)
    ).apply(i, BlockSpec::new)));

    public static BlockSpec of(Block block) {
        return new BlockSpec(Optional.of(BuiltInRegistries.BLOCK.getKey(block)), Map.of(), Optional.empty(), List.of());
    }

    public boolean isAir() {
        return block.map(b -> b.getPath().equals("air") && b.getNamespace().equals("minecraft")).orElse(false);
    }

    /** Whether the block in the world satisfies this spec for a structure rotated by {@code rotation}. */
    public boolean test(BlockState actual, Rotation rotation) {
        if (!anyOf.isEmpty()) {
            for (BlockSpec s : anyOf) {
                if (s.test(actual, rotation)) {
                    return true;
                }
            }
            return false;
        }
        if (tag.isPresent()) {
            return actual.is(tag.get());
        }
        if (block.isEmpty()) {
            return true;
        }
        if (isAir()) {
            return actual.isAir();
        }
        Block b = BuiltInRegistries.BLOCK.get(block.get());
        if (!actual.is(b)) {
            return false;
        }
        if (properties.isEmpty()) {
            return true;
        }
        BlockState expected = withProperties(b.defaultBlockState()).rotate(rotation);
        for (String name : properties.keySet()) {
            Property<?> p = b.getStateDefinition().getProperty(name);
            if (p != null && !expected.getValue(p).equals(actual.getValue(p))) {
                return false;
            }
        }
        return true;
    }

    /** The state shown for this position (first option of an any_of, first block of a tag). */
    public BlockState display(Rotation rotation) {
        if (!anyOf.isEmpty()) {
            return anyOf.get(0).display(rotation);
        }
        if (tag.isPresent()) {
            return BuiltInRegistries.BLOCK.getTag(tag.get())
                    .flatMap(set -> set.stream().findFirst())
                    .map(h -> h.value().defaultBlockState())
                    .orElse(Blocks.BARRIER.defaultBlockState());
        }
        if (block.isEmpty()) {
            return Blocks.AIR.defaultBlockState();
        }
        Block b = BuiltInRegistries.BLOCK.get(block.get());
        return withProperties(b.defaultBlockState()).rotate(rotation);
    }

    /** The item a player places for this position (for material lists); AIR when there is none. */
    public Item item() {
        Item item = display(Rotation.NONE).getBlock().asItem();
        return item == null ? Items.AIR : item;
    }

    private BlockState withProperties(BlockState state) {
        for (Map.Entry<String, String> e : properties.entrySet()) {
            Property<?> p = state.getBlock().getStateDefinition().getProperty(e.getKey());
            if (p != null) {
                state = set(state, p, e.getValue());
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState set(BlockState state, Property<T> p, String value) {
        return p.getValue(value).map(v -> state.setValue(p, v)).orElse(state);
    }
}
