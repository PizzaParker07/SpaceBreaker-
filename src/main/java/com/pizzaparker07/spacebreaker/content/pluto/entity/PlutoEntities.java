package com.pizzaparker07.spacebreaker.content.pluto.entity;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.pluto.PlutoData;
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

/** Creatures of Pluto and Charon. */
public final class PlutoEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<NitrogenSlider>> NITROGEN_SLIDER = ENTITIES.register("nitrogen_slider",
            () -> EntityType.Builder.of(NitrogenSlider::new, MobCategory.CREATURE).sized(0.9F, 0.6F).eyeHeight(0.45F).clientTrackingRange(10)
                    .build("nitrogen_slider"));
    public static final DeferredHolder<EntityType<?>, EntityType<FrostTortoise>> FROST_TORTOISE = ENTITIES.register("frost_tortoise",
            () -> EntityType.Builder.of(FrostTortoise::new, MobCategory.CREATURE).sized(1.0F, 0.75F).eyeHeight(0.5F).clientTrackingRange(10)
                    .build("frost_tortoise"));
    private static final List<DeferredItem<DeferredSpawnEggItem>> EGGS = new ArrayList<>();
    private static DeferredItem<DeferredSpawnEggItem> sliderEgg;
    private static DeferredItem<DeferredSpawnEggItem> tortoiseEgg;

    private PlutoEntities() {
    }

    public static void register(IEventBus modBus, DeferredRegister.Items items) {
        sliderEgg = items.register("nitrogen_slider_spawn_egg", () -> new DeferredSpawnEggItem(NITROGEN_SLIDER, 0xD8D2C8, 0x8C5A44, new Item.Properties()));
        tortoiseEgg = items.register("frost_tortoise_spawn_egg", () -> new DeferredSpawnEggItem(FROST_TORTOISE, 0x8E8C8A, 0x6E2E22, new Item.Properties()));
        EGGS.add(sliderEgg);
        EGGS.add(tortoiseEgg);
        ENTITIES.register(modBus);
        modBus.addListener(EntityAttributeCreationEvent.class, e -> {
            e.put(NITROGEN_SLIDER.get(), NitrogenSlider.attributes().build());
            e.put(FROST_TORTOISE.get(), FrostTortoise.attributes().build());
        });
        modBus.addListener(RegisterSpawnPlacementsEvent.class, e -> {
            e.register(NITROGEN_SLIDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NitrogenSlider::checkSpawn,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            e.register(FROST_TORTOISE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, FrostTortoise::checkSpawn,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(PlutoData.PLUTO_PLAINS, NITROGEN_SLIDER, MobCategory.CREATURE, 10, 2, 4));
        OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(PlutoData.PLUTO_MOUNTAINS, NITROGEN_SLIDER, MobCategory.CREATURE, 4, 1, 2));
        OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(PlutoData.CHARON_PLAINS, FROST_TORTOISE, MobCategory.CREATURE, 8, 1, 3));
        OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(PlutoData.CHARON_POLAR, FROST_TORTOISE, MobCategory.CREATURE, 10, 2, 3));
    }

    public static List<DeferredItem<DeferredSpawnEggItem>> eggs() {
        return EGGS;
    }

    public static void translations(Lang lang) {
        lang.add("entity.spacebreaker.nitrogen_slider", "Nitrogen Slider", "Azot Kaydırıcısı");
        lang.item(sliderEgg, "Nitrogen Slider Spawn Egg", "Azot Kaydırıcısı Çağırma Yumurtası");
        lang.add("entity.spacebreaker.frost_tortoise", "Frost Tortoise", "Kırağı Kaplumbağası");
        lang.item(tortoiseEgg, "Frost Tortoise Spawn Egg", "Kırağı Kaplumbağası Çağırma Yumurtası");
    }
}
