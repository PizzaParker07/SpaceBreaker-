package com.pizzaparker07.spacebreaker.content.moon.entity;

import com.pizzaparker07.spacebreaker.Spacebreaker;
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

/** Moon creatures and guardians. */
public final class MoonEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Spacebreaker.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<RegolithTick>> REGOLITH_TICK = ENTITIES.register("regolith_tick",
            () -> EntityType.Builder.of(RegolithTick::new, MobCategory.MONSTER).sized(0.7F, 0.45F).eyeHeight(0.3F).clientTrackingRange(8)
                    .build("regolith_tick"));
    public static final DeferredHolder<EntityType<?>, EntityType<RegolithTick>> ELDER_REGOLITH_TICK = ENTITIES.register("elder_regolith_tick",
            () -> EntityType.Builder.of(RegolithTick::new, MobCategory.MONSTER).sized(1.3F, 0.8F).eyeHeight(0.55F).clientTrackingRange(8)
                    .build("elder_regolith_tick"));
    public static final DeferredHolder<EntityType<?>, EntityType<DustSwimmer>> DUST_SWIMMER = ENTITIES.register("dust_swimmer",
            () -> EntityType.Builder.of(DustSwimmer::new, MobCategory.CREATURE).sized(1.1F, 0.4F).eyeHeight(0.3F).clientTrackingRange(10)
                    .build("dust_swimmer"));
    public static final DeferredHolder<EntityType<?>, EntityType<SurveyDrone>> SURVEY_DRONE = ENTITIES.register("survey_drone",
            () -> EntityType.Builder.of(SurveyDrone::new, MobCategory.MONSTER).sized(0.8F, 0.8F).eyeHeight(0.45F).clientTrackingRange(10)
                    .build("survey_drone"));
    public static final DeferredHolder<EntityType<?>, EntityType<StunBolt>> STUN_BOLT = ENTITIES.register("stun_bolt",
            () -> EntityType.Builder.<StunBolt>of(StunBolt::new, MobCategory.MISC).sized(0.3125F, 0.3125F).clientTrackingRange(4).updateInterval(10)
                    .build("stun_bolt"));

    public static DeferredItem<DeferredSpawnEggItem> REGOLITH_TICK_EGG;
    public static DeferredItem<DeferredSpawnEggItem> ELDER_REGOLITH_TICK_EGG;
    public static DeferredItem<DeferredSpawnEggItem> DUST_SWIMMER_EGG;
    public static DeferredItem<DeferredSpawnEggItem> SURVEY_DRONE_EGG;

    private MoonEntities() {
    }

    /** Registers entity types and spawn eggs (into the Moon module's item register). */
    public static void register(IEventBus modBus, net.neoforged.neoforge.registries.DeferredRegister.Items items) {
        REGOLITH_TICK_EGG = items.register("regolith_tick_spawn_egg", () -> new DeferredSpawnEggItem(REGOLITH_TICK, 0x8B8882, 0x4A4640, new Item.Properties()));
        ELDER_REGOLITH_TICK_EGG = items.register("elder_regolith_tick_spawn_egg", () -> new DeferredSpawnEggItem(ELDER_REGOLITH_TICK, 0x4A4640, 0xB2BA62, new Item.Properties()));
        DUST_SWIMMER_EGG = items.register("dust_swimmer_spawn_egg", () -> new DeferredSpawnEggItem(DUST_SWIMMER, 0x9C9A94, 0x5F5E58, new Item.Properties()));
        SURVEY_DRONE_EGG = items.register("survey_drone_spawn_egg", () -> new DeferredSpawnEggItem(SURVEY_DRONE, 0xD8DCE0, 0xC08616, new Item.Properties()));
        ENTITIES.register(modBus);
        modBus.addListener(EntityAttributeCreationEvent.class, e -> {
            e.put(REGOLITH_TICK.get(), RegolithTick.attributes().build());
            e.put(ELDER_REGOLITH_TICK.get(), RegolithTick.elderAttributes().build());
            e.put(DUST_SWIMMER.get(), DustSwimmer.attributes().build());
            e.put(SURVEY_DRONE.get(), SurveyDrone.attributes().build());
        });
        modBus.addListener(RegisterSpawnPlacementsEvent.class, e -> {
            e.register(REGOLITH_TICK.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    RegolithTick::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
            e.register(ELDER_REGOLITH_TICK.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    RegolithTick::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
            e.register(DUST_SWIMMER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    DustSwimmer::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
    }

    public static List<DeferredItem<DeferredSpawnEggItem>> eggs() {
        return List.of(REGOLITH_TICK_EGG, ELDER_REGOLITH_TICK_EGG, DUST_SWIMMER_EGG, SURVEY_DRONE_EGG);
    }

    public static void translations(Lang lang) {
        lang.add(REGOLITH_TICK.get().getDescriptionId(), "Regolith Tick", "Regolit Kenesi");
        lang.add(ELDER_REGOLITH_TICK.get().getDescriptionId(), "Elder Regolith Tick", "Yaşlı Regolit Kenesi");
        lang.add(DUST_SWIMMER.get().getDescriptionId(), "Dust Swimmer", "Toz Yüzücüsü");
        lang.add(SURVEY_DRONE.get().getDescriptionId(), "Survey Drone", "Keşif Dronu");
        lang.add(STUN_BOLT.get().getDescriptionId(), "Stun Bolt", "Sersemletme Atışı");
        lang.item(REGOLITH_TICK_EGG, "Regolith Tick Spawn Egg", "Regolit Kenesi Çağırma Yumurtası");
        lang.item(ELDER_REGOLITH_TICK_EGG, "Elder Regolith Tick Spawn Egg", "Yaşlı Regolit Kenesi Çağırma Yumurtası");
        lang.item(DUST_SWIMMER_EGG, "Dust Swimmer Spawn Egg", "Toz Yüzücüsü Çağırma Yumurtası");
        lang.item(SURVEY_DRONE_EGG, "Survey Drone Spawn Egg", "Keşif Dronu Çağırma Yumurtası");
    }
}
