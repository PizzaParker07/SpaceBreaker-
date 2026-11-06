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

/** Mercury's creatures: the Terminator Crawler (dangerous) and the Hollow Snail (passive, gives sulfur). */
public final class MercuryEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<TerminatorCrawler>> TERMINATOR_CRAWLER = ENTITIES.register("terminator_crawler",
            () -> EntityType.Builder.of(TerminatorCrawler::new, MobCategory.MONSTER).sized(1.0F, 0.6F).eyeHeight(0.4F).clientTrackingRange(8)
                    .build("terminator_crawler"));
    public static final DeferredHolder<EntityType<?>, EntityType<HollowSnail>> HOLLOW_SNAIL = ENTITIES.register("hollow_snail",
            () -> EntityType.Builder.of(HollowSnail::new, MobCategory.CREATURE).sized(0.7F, 0.6F).eyeHeight(0.45F).clientTrackingRange(8)
                    .build("hollow_snail"));
    private static DeferredItem<DeferredSpawnEggItem> egg;
    private static DeferredItem<DeferredSpawnEggItem> snailEgg;

    private MercuryEntities() {
    }

    static void register(IEventBus modBus, DeferredRegister.Items items) {
        egg = items.register("terminator_crawler_spawn_egg", () -> new DeferredSpawnEggItem(TERMINATOR_CRAWLER, 0x3B332E, 0xFFB04A, new Item.Properties()));
        snailEgg = items.register("hollow_snail_spawn_egg", () -> new DeferredSpawnEggItem(HOLLOW_SNAIL, 0xC6CAD1, 0x65564B, new Item.Properties()));
        ENTITIES.register(modBus);
        modBus.addListener(EntityAttributeCreationEvent.class, e -> {
            e.put(TERMINATOR_CRAWLER.get(), TerminatorCrawler.attributes().build());
            e.put(HOLLOW_SNAIL.get(), HollowSnail.attributes().build());
        });
        modBus.addListener(RegisterSpawnPlacementsEvent.class, e -> {
            e.register(TERMINATOR_CRAWLER.get(), SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, TerminatorCrawler::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
            e.register(HOLLOW_SNAIL.get(), SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HollowSnail::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        for (var biome : MercuryData.BIOMES) {
            MarsData.SPAWNS.add(new MarsData.BiomeSpawns(biome, s -> s.addSpawn(MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(TERMINATOR_CRAWLER.get(), biome == MercuryData.POLAR ? 20 : 60, 1, 2))));
            if (biome != MercuryData.POLAR) {
                MarsData.SPAWNS.add(new MarsData.BiomeSpawns(biome, s -> s.addSpawn(MobCategory.CREATURE,
                        new MobSpawnSettings.SpawnerData(HOLLOW_SNAIL.get(), biome == MercuryData.CRATER ? 12 : 6, 2, 4))));
            }
        }
    }

    static List<DeferredItem<DeferredSpawnEggItem>> eggs() {
        return List.of(egg, snailEgg);
    }

    static void translations(Lang lang) {
        lang.add(TERMINATOR_CRAWLER.get().getDescriptionId(), "Terminator Crawler", "Alacakaranlık Sürüngeni");
        lang.item(egg, "Terminator Crawler Spawn Egg", "Alacakaranlık Sürüngeni Çağırma Yumurtası");
        lang.add(HOLLOW_SNAIL.get().getDescriptionId(), "Hollow Snail", "Oyuk Salyangozu");
        lang.item(snailEgg, "Hollow Snail Spawn Egg", "Oyuk Salyangozu Çağırma Yumurtası");
    }
}
