package com.pizzaparker07.spacebreaker.core.data;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Tag sink handed to modules during datagen. */
public interface SbTags {
    void block(TagKey<Block> tag, Block... blocks);

    void blockTag(TagKey<Block> tag, TagKey<Block>... children);

    void item(TagKey<Item> tag, Item... items);

    void itemTag(TagKey<Item> tag, TagKey<Item>... children);

    /** Mirror a block tag into an item tag (for block items). */
    void copy(TagKey<Block> blockTag, TagKey<Item> itemTag);
}
