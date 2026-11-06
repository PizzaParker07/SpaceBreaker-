package com.pizzaparker07.spacebreaker.content.neptune.entity;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.neptune.NeptuneData;
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

/** Creatures of Triton. */
public final class NeptuneEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<PlumeBeetle>> PLUME_BEETLE = ENTITIES.register("plume_beetle",
            () -> EntityType.Builder.of(PlumeBeetle::new, MobCategory.CREATURE).sized(0.8F, 0.55F).eyeHeight(0.35F).clientTrackingRange(10)
                    .build("plume_beetle"));
    private static final List<DeferredItem<DeferredSpawnEggItem>> EGGS = new ArrayList<>();
    private static DeferredItem<DeferredSpawnEggItem> beetleEgg;

    private NeptuneEntities() {
    }

    public static void register(IEventBus modBus, DeferredRegister.Items items) {
        beetleEgg = items.register("plume_beetle_spawn_egg", () -> new DeferredSpawnEggItem(PLUME_BEETLE, 0x1E1C1D, 0xC79AA2, new Item.Properties()));
        EGGS.add(beetleEgg);
        ENTITIES.register(modBus);
        modBus.addListener(EntityAttributeCreationEvent.class, e -> e.put(PLUME_BEETLE.get(), PlumeBeetle.attributes().build()));
        modBus.addListener(RegisterSpawnPlacementsEvent.class, e -> e.register(PLUME_BEETLE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, PlumeBeetle::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(NeptuneData.TRITON_POLAR, PLUME_BEETLE, MobCategory.CREATURE, 14, 2, 4));
        OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(NeptuneData.TRITON_CANTALOUPE, PLUME_BEETLE, MobCategory.CREATURE, 4, 1, 2));
    }

    public static List<DeferredItem<DeferredSpawnEggItem>> eggs() {
        return EGGS;
    }

    public static void translations(Lang lang) {
        lang.add("entity.spacebreaker.plume_beetle", "Plume Beetle", "Gayzer Böceği");
        lang.item(beetleEgg, "Plume Beetle Spawn Egg", "Gayzer Böceği Çağırma Yumurtası");
    }
}
