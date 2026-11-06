package com.pizzaparker07.spacebreaker.content.mercury;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.MarsData;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/** Mercury's creature. */
public final class MercuryEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<TerminatorCrawler>> TERMINATOR_CRAWLER = ENTITIES.register("terminator_crawler",
            () -> EntityType.Builder.of(TerminatorCrawler::new, MobCategory.MONSTER).sized(1.0F, 0.6F).eyeHeight(0.4F).clientTrackingRange(8)
                    .build("terminator_crawler"));
    private static DeferredItem<DeferredSpawnEggItem> egg;

    private MercuryEntities() {
    }

    static void register(IEventBus modBus, DeferredRegister.Items items) {
        egg = items.register("terminator_crawler_spawn_egg", () -> new DeferredSpawnEggItem(TERMINATOR_CRAWLER, 0x3B332E, 0xFFB04A, new Item.Properties()));
        ENTITIES.register(modBus);
        modBus.addListener(EntityAttributeCreationEvent.class, e -> e.put(TERMINATOR_CRAWLER.get(), TerminatorCrawler.attributes().build()));
        modBus.addListener(RegisterSpawnPlacementsEvent.class, e -> e.register(TERMINATOR_CRAWLER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, TerminatorCrawler::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        for (var biome : MercuryData.BIOMES) {
            MarsData.SPAWNS.add(new MarsData.BiomeSpawns(biome, s -> s.addSpawn(MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(TERMINATOR_CRAWLER.get(), biome == MercuryData.POLAR ? 20 : 60, 1, 2))));
        }
    }

    static List<DeferredItem<DeferredSpawnEggItem>> eggs() {
        return List.of(egg);
    }

    static void translations(Lang lang) {
        lang.add(TERMINATOR_CRAWLER.get().getDescriptionId(), "Terminator Crawler", "Alacakaranlık Sürüngeni");
        lang.item(egg, "Terminator Crawler Spawn Egg", "Alacakaranlık Sürüngeni Çağırma Yumurtası");
    }
}
