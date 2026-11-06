package com.pizzaparker07.spacebreaker.content.research.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.research.ResearchTree;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Research datagen: tech tree JSON ({@link ResearchTree}) and the main advancement tree ({@link ResearchAdvancements}). */
public final class ResearchDataGen {
    public static void gather(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        gen.addProvider(event.includeServer(), new TechTreeProvider(out));
        gen.addProvider(event.includeServer(), new AdvancementProvider(out, event.getLookupProvider(), List.of(new ResearchAdvancements())));
    }

    /** Writes data/spacebreaker/spacebreaker/tech/<id>.json for every node of the built-in tree. */
    static final class TechTreeProvider implements DataProvider {
        private final PackOutput.PathProvider paths;

        TechTreeProvider(PackOutput out) {
            this.paths = out.createPathProvider(PackOutput.Target.DATA_PACK, "spacebreaker/tech");
        }

        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
            List<CompletableFuture<?>> futures = new ArrayList<>();
            for (ResearchTree.Def def : ResearchTree.all().values()) {
                JsonElement json = TechNode.CODEC.encodeStart(JsonOps.INSTANCE, def.node()).getOrThrow();
                Path path = paths.json(def.id());
                futures.add(DataProvider.saveStable(cache, json, path));
            }
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        }

        @Override
        public String getName() {
            return Spacebreaker.MOD_ID + " tech tree";
        }
    }

    private ResearchDataGen() {
    }
}
