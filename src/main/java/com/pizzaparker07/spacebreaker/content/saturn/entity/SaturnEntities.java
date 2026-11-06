package com.pizzaparker07.spacebreaker.content.saturn.entity;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.saturn.SaturnData;
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

/** Creatures of Saturn's moons. */
public final class SaturnEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<MethaneSkater>> METHANE_SKATER = ENTITIES.register("methane_skater",
            () -> EntityType.Builder.of(MethaneSkater::new, MobCategory.CREATURE).sized(1.2F, 0.6F).eyeHeight(0.45F).clientTrackingRange(10)
                    .build("methane_skater"));
    public static final DeferredHolder<EntityType<?>, EntityType<GeyserDrifter>> GEYSER_DRIFTER = ENTITIES.register("geyser_drifter",
            () -> EntityType.Builder.of(GeyserDrifter::new, MobCategory.AMBIENT).sized(0.7F, 0.7F).eyeHeight(0.4F).clientTrackingRange(10)
                    .build("geyser_drifter"));
    private static final List<DeferredItem<DeferredSpawnEggItem>> EGGS = new ArrayList<>();
    private static DeferredItem<DeferredSpawnEggItem> skaterEgg;
    private static DeferredItem<DeferredSpawnEggItem> drifterEgg;

    private SaturnEntities() {
    }

    public static void register(IEventBus modBus, DeferredRegister.Items items) {
        skaterEgg = items.register("methane_skater_spawn_egg", () -> new DeferredSpawnEggItem(METHANE_SKATER, 0x5A3A1E, 0xD9A55A, new Item.Properties()));
        drifterEgg = items.register("geyser_drifter_spawn_egg", () -> new DeferredSpawnEggItem(GEYSER_DRIFTER, 0xCFE8F2, 0x6FB8E0, new Item.Properties()));
        EGGS.add(skaterEgg);
        EGGS.add(drifterEgg);
        ENTITIES.register(modBus);
        modBus.addListener(EntityAttributeCreationEvent.class, e -> {
            e.put(METHANE_SKATER.get(), MethaneSkater.attributes().build());
            e.put(GEYSER_DRIFTER.get(), GeyserDrifter.attributes().build());
        });
        modBus.addListener(RegisterSpawnPlacementsEvent.class, e -> {
            e.register(METHANE_SKATER.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, MethaneSkater::checkSpawn,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            e.register(GEYSER_DRIFTER.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, GeyserDrifter::checkSpawn,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        for (var biome : SaturnData.TITAN_BIOMES) {
            OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(biome, METHANE_SKATER, MobCategory.CREATURE, biome == SaturnData.TITAN_LAKES ? 30 : 8, 2, 4));
        }
        OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(SaturnData.ENCELADUS_POLAR, GEYSER_DRIFTER, MobCategory.AMBIENT, 20, 2, 5));
    }

    public static List<DeferredItem<DeferredSpawnEggItem>> eggs() {
        return EGGS;
    }

    public static void translations(Lang lang) {
        lang.add("entity.spacebreaker.methane_skater", "Methane Skater", "Metan Kayıcısı");
        lang.item(skaterEgg, "Methane Skater Spawn Egg", "Metan Kayıcısı Çağırma Yumurtası");
        lang.add("entity.spacebreaker.geyser_drifter", "Geyser Drifter", "Gayzer Süzülücüsü");
        lang.item(drifterEgg, "Geyser Drifter Spawn Egg", "Gayzer Süzülücüsü Çağırma Yumurtası");
    }
}
