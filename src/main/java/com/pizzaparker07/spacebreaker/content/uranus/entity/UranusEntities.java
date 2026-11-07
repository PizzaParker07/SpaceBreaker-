package com.pizzaparker07.spacebreaker.content.uranus.entity;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.uranus.UranusData;
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

import java.util.ArrayList;
import java.util.List;

/** Creatures of Miranda. */
public final class UranusEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<CliffClinger>> CLIFF_CLINGER = ENTITIES.register("cliff_clinger",
            () -> EntityType.Builder.of(CliffClinger::new, MobCategory.CREATURE).sized(0.9F, 0.35F).eyeHeight(0.25F).clientTrackingRange(10)
                    .build("cliff_clinger"));
    public static final DeferredHolder<EntityType<?>, EntityType<FrostMoth>> FROST_MOTH = ENTITIES.register("frost_moth",
            () -> EntityType.Builder.of(FrostMoth::new, MobCategory.AMBIENT).sized(0.6F, 0.5F).eyeHeight(0.3F).clientTrackingRange(8)
                    .build("frost_moth"));
    private static final List<DeferredItem<DeferredSpawnEggItem>> EGGS = new ArrayList<>();
    private static DeferredItem<DeferredSpawnEggItem> clingerEgg;
    private static DeferredItem<DeferredSpawnEggItem> mothEgg;

    private UranusEntities() {
    }

    public static void register(IEventBus modBus, DeferredRegister.Items items) {
        clingerEgg = items.register("cliff_clinger_spawn_egg", () -> new DeferredSpawnEggItem(CLIFF_CLINGER, 0x5C5A57, 0xD6D8DA, new Item.Properties()));
        mothEgg = items.register("frost_moth_spawn_egg", () -> new DeferredSpawnEggItem(FROST_MOTH, 0xDADDE0, 0x8E8A84, new Item.Properties()));
        EGGS.add(clingerEgg);
        EGGS.add(mothEgg);
        ENTITIES.register(modBus);
        modBus.addListener(EntityAttributeCreationEvent.class, e -> {
            e.put(CLIFF_CLINGER.get(), CliffClinger.attributes().build());
            e.put(FROST_MOTH.get(), FrostMoth.attributes().build());
        });
        modBus.addListener(RegisterSpawnPlacementsEvent.class, e -> e.register(FROST_MOTH.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, FrostMoth::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        modBus.addListener(RegisterSpawnPlacementsEvent.class, e -> e.register(CLIFF_CLINGER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CliffClinger::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(UranusData.MIRANDA_PLAINS, CLIFF_CLINGER, MobCategory.CREATURE, 10, 2, 4));
        OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(UranusData.MIRANDA_CRATER, CLIFF_CLINGER, MobCategory.CREATURE, 6, 1, 3));
        OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(UranusData.ARIEL_PLAINS, FROST_MOTH, MobCategory.AMBIENT, 10, 2, 5));
        OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(UranusData.ARIEL_CRATER, FROST_MOTH, MobCategory.AMBIENT, 6, 1, 3));
    }

    public static List<DeferredItem<DeferredSpawnEggItem>> eggs() {
        return EGGS;
    }

    public static void translations(Lang lang) {
        lang.add("entity.spacebreaker.cliff_clinger", "Cliff Clinger", "Uçurum Yapışkanı");
        lang.item(clingerEgg, "Cliff Clinger Spawn Egg", "Uçurum Yapışkanı Çağırma Yumurtası");
        lang.add("entity.spacebreaker.frost_moth", "Frost Moth", "Kırağı Güvesi");
        lang.item(mothEgg, "Frost Moth Spawn Egg", "Kırağı Güvesi Çağırma Yumurtası");
    }
}
