package com.pizzaparker07.spacebreaker.datagen;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.Modules;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Runs every module's translations, loot tables and tags. Assets (models, textures) come from tools/. */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID)
public final class DataGenerators {
    private DataGenerators() {
    }

    @SubscribeEvent
    static void gather(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        ExistingFileHelper helper = event.getExistingFileHelper();

        Lang lang = new Lang();
        Modules.ALL.forEach(m -> m.addTranslations(lang));
        gen.addProvider(event.includeClient(), new LangProvider(out, "en_us", lang, Lang.EN_US));
        gen.addProvider(event.includeClient(), new LangProvider(out, "tr_tr", lang, Lang.TR_TR));

        gen.addProvider(event.includeServer(), new LootTableProvider(out, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(SbBlockLoot::new, LootContextParamSets.BLOCK)), lookup));

        // Collect every module's tags once, then feed the two vanilla providers.
        Collector tags = new Collector();
        Modules.ALL.forEach(m -> m.addTags(tags));
        BlockTags blockTags = gen.addProvider(event.includeServer(), new BlockTags(out, lookup, helper, tags));
        gen.addProvider(event.includeServer(), new ItemTags(out, lookup, blockTags.contentsGetter(), helper, tags));
    }

    private static final class LangProvider extends LanguageProvider {
        private final Lang lang;
        private final int column;

        LangProvider(PackOutput output, String locale, Lang lang, int column) {
            super(output, Spacebreaker.MOD_ID, locale);
            this.lang = lang;
            this.column = column;
        }

        @Override
        protected void addTranslations() {
            lang.entries().forEach((key, values) -> add(key, values[column]));
        }
    }

    /** Records tag calls from modules so they can be replayed into the real providers. */
    private static final class Collector implements SbTags {
        final List<BiConsumer<BlockTags, Void>> block = new ArrayList<>();
        final List<BiConsumer<ItemTags, Void>> item = new ArrayList<>();

        @Override
        public void block(TagKey<Block> tag, Block... blocks) {
            block.add((p, v) -> p.tagOf(tag).add(blocks));
        }

        @Override
        @SafeVarargs
        public final void blockTag(TagKey<Block> tag, TagKey<Block>... children) {
            block.add((p, v) -> p.tagOf(tag).addTags(children));
        }

        @Override
        public void item(TagKey<Item> tag, Item... items) {
            item.add((p, v) -> p.tagOf(tag).add(items));
        }

        @Override
        @SafeVarargs
        public final void itemTag(TagKey<Item> tag, TagKey<Item>... children) {
            item.add((p, v) -> p.tagOf(tag).addTags(children));
        }

        @Override
        public void copy(TagKey<Block> blockTag, TagKey<Item> itemTag) {
            item.add((p, v) -> p.copyTag(blockTag, itemTag));
        }
    }

    private static final class BlockTags extends BlockTagsProvider {
        private final Collector tags;

        BlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper, Collector tags) {
            super(output, lookup, Spacebreaker.MOD_ID, helper);
            this.tags = tags;
        }

        IntrinsicTagAppender<Block> tagOf(TagKey<Block> key) {
            return tag(key);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tags.block.forEach(c -> c.accept(this, null));
        }
    }

    private static final class ItemTags extends ItemTagsProvider {
        private final Collector tags;

        ItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, CompletableFuture<TagLookup<Block>> blocks,
                 ExistingFileHelper helper, Collector tags) {
            super(output, lookup, blocks, Spacebreaker.MOD_ID, helper);
            this.tags = tags;
        }

        IntrinsicTagAppender<Item> tagOf(TagKey<Item> key) {
            return tag(key);
        }

        void copyTag(TagKey<Block> block, TagKey<Item> item) {
            copy(block, item);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tags.item.forEach(c -> c.accept(this, null));
        }
    }
}
