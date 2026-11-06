package com.pizzaparker07.spacebreaker.datagen;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.registry.ModBlocks;
import com.pizzaparker07.spacebreaker.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public final class ModTags {
    public static final TagKey<Block> ORES_BAUXITE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/bauxite"));
    public static final TagKey<Block> ORES_ILMENITE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/ilmenite"));
    public static final TagKey<Item> ITEM_ORES_BAUXITE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/bauxite"));
    public static final TagKey<Item> ITEM_ORES_ILMENITE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/ilmenite"));
    public static final TagKey<Item> RAW_BAUXITE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "raw_materials/bauxite"));
    public static final TagKey<Item> INGOTS_ALUMINUM = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/aluminum"));
    public static final TagKey<Item> PLATES_ALUMINUM = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "plates/aluminum"));

    static final class Blocks extends BlockTagsProvider {
        Blocks(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
            super(output, lookup, Spacebreaker.MOD_ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.BAUXITE_ORE.get(), ModBlocks.DEEPSLATE_BAUXITE_ORE.get(),
                    ModBlocks.RAW_BAUXITE_BLOCK.get(), ModBlocks.MARE_BASALT.get(), ModBlocks.ILMENITE_ORE.get(), ModBlocks.SOLAR_PANEL.get());
            tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.LUNAR_REGOLITH.get());
            tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.BAUXITE_ORE.get(), ModBlocks.DEEPSLATE_BAUXITE_ORE.get(), ModBlocks.RAW_BAUXITE_BLOCK.get());
            tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.ILMENITE_ORE.get());
            tag(ORES_BAUXITE).add(ModBlocks.BAUXITE_ORE.get(), ModBlocks.DEEPSLATE_BAUXITE_ORE.get());
            tag(ORES_ILMENITE).add(ModBlocks.ILMENITE_ORE.get());
            tag(Tags.Blocks.ORES).addTags(ORES_BAUXITE, ORES_ILMENITE);
        }
    }

    static final class Items extends ItemTagsProvider {
        Items(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, CompletableFuture<TagLookup<Block>> blocks, ExistingFileHelper helper) {
            super(output, lookup, blocks, Spacebreaker.MOD_ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            copy(ORES_BAUXITE, ITEM_ORES_BAUXITE);
            copy(ORES_ILMENITE, ITEM_ORES_ILMENITE);
            tag(Tags.Items.ORES).addTags(ITEM_ORES_BAUXITE, ITEM_ORES_ILMENITE);
            tag(RAW_BAUXITE).add(ModItems.RAW_BAUXITE.get());
            tag(Tags.Items.RAW_MATERIALS).addTag(RAW_BAUXITE);
            tag(INGOTS_ALUMINUM).add(ModItems.ALUMINUM_INGOT.get());
            tag(Tags.Items.INGOTS).addTag(INGOTS_ALUMINUM);
            tag(PLATES_ALUMINUM).add(ModItems.ALUMINUM_PLATE.get());
            tag(ItemTags.HEAD_ARMOR).add(ModItems.EVA_HELMET.get());
        }
    }

    private ModTags() {
    }
}
