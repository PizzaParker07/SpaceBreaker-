package com.pizzaparker07.spacebreaker.datagen;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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

        gen.addProvider(event.includeClient(), new ModLang.Provider(out, "en_us", 0));
        gen.addProvider(event.includeClient(), new ModLang.Provider(out, "tr_tr", 1));

        gen.addProvider(event.includeServer(), new LootTableProvider(out, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModLootTables::new, LootContextParamSets.BLOCK)), lookup));
        ModTags.Blocks blockTags = gen.addProvider(event.includeServer(), new ModTags.Blocks(out, lookup, helper));
        gen.addProvider(event.includeServer(), new ModTags.Items(out, lookup, blockTags.contentsGetter(), helper));
    }
}
