package com.pizzaparker07.spacebreaker.content.belt.entity;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.BeltData;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/** Creatures of the asteroid belt. */
public final class BeltEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<RockMite>> ROCK_MITE = ENTITIES.register("rock_mite",
            () -> EntityType.Builder.of(RockMite::new, MobCategory.MONSTER).sized(0.8F, 0.45F).eyeHeight(0.3F).clientTrackingRange(8)
                    .build("rock_mite"));
    private static DeferredItem<DeferredSpawnEggItem> egg;

    private BeltEntities() {
    }

    public static void register(IEventBus modBus, DeferredRegister.Items items) {
        egg = items.register("rock_mite_spawn_egg", () -> new DeferredSpawnEggItem(ROCK_MITE, 0x2E2B29, 0xD8D2C4, new Item.Properties()));
        ENTITIES.register(modBus);
        modBus.addListener(EntityAttributeCreationEvent.class, e -> e.put(ROCK_MITE.get(), RockMite.attributes().build()));
        modBus.addListener(RegisterSpawnPlacementsEvent.class, e -> e.register(ROCK_MITE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RockMite::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        for (var biome : BeltData.CERES_BIOMES) {
            OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(biome, ROCK_MITE, MobCategory.MONSTER, biome == BeltData.CERES_POLAR ? 20 : 70, 2, 4));
        }
        for (var biome : BeltData.VESTA_BIOMES) {
            OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(biome, ROCK_MITE, MobCategory.MONSTER, 40, 1, 3));
        }
    }

    public static List<DeferredItem<DeferredSpawnEggItem>> eggs() {
        return List.of(egg);
    }

    public static void translations(Lang lang) {
        lang.add("entity.spacebreaker.rock_mite", "Rock Mite", "Kaya Akarı");
        lang.item(egg, "Rock Mite Spawn Egg", "Kaya Akarı Çağırma Yumurtası");
    }
}
