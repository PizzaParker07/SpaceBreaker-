package com.pizzaparker07.spacebreaker.content.jupiter.entity;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.belt.OuterWorlds;
import com.pizzaparker07.spacebreaker.content.jupiter.JupiterData;
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

/** Creatures of the Galilean moons. */
public final class JupiterEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<GlowSerpent>> GLOW_SERPENT = ENTITIES.register("glow_serpent",
            () -> EntityType.Builder.of(GlowSerpent::new, MobCategory.WATER_CREATURE).sized(0.6F, 0.4F).eyeHeight(0.2F).clientTrackingRange(8)
                    .build("glow_serpent"));
    public static final DeferredHolder<EntityType<?>, EntityType<SulfurCrab>> SULFUR_CRAB = ENTITIES.register("sulfur_crab",
            () -> EntityType.Builder.of(SulfurCrab::new, MobCategory.MONSTER).sized(1.0F, 0.6F).eyeHeight(0.45F).fireImmune().clientTrackingRange(8)
                    .build("sulfur_crab"));
    private static final List<DeferredItem<DeferredSpawnEggItem>> EGGS = new ArrayList<>();
    private static DeferredItem<DeferredSpawnEggItem> serpentEgg;
    private static DeferredItem<DeferredSpawnEggItem> crabEgg;

    private JupiterEntities() {
    }

    public static void register(IEventBus modBus, DeferredRegister.Items items) {
        serpentEgg = items.register("glow_serpent_spawn_egg", () -> new DeferredSpawnEggItem(GLOW_SERPENT, 0x1B2C3A, 0x5EF0E0, new Item.Properties()));
        crabEgg = items.register("sulfur_crab_spawn_egg", () -> new DeferredSpawnEggItem(SULFUR_CRAB, 0xC9A23A, 0x7A2E14, new Item.Properties()));
        EGGS.add(serpentEgg);
        EGGS.add(crabEgg);
        ENTITIES.register(modBus);
        modBus.addListener(EntityAttributeCreationEvent.class, e -> {
            e.put(GLOW_SERPENT.get(), GlowSerpent.attributes().build());
            e.put(SULFUR_CRAB.get(), SulfurCrab.attributes().build());
        });
        modBus.addListener(RegisterSpawnPlacementsEvent.class, e -> {
            e.register(GLOW_SERPENT.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR, GlowSerpent::checkSpawn,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            e.register(SULFUR_CRAB.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SulfurCrab::checkSpawn,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        for (var biome : JupiterData.EUROPA_BIOMES) {
            OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(biome, GLOW_SERPENT, MobCategory.WATER_CREATURE, 10, 2, 4));
        }
        for (var biome : JupiterData.IO_BIOMES) {
            OuterWorlds.SPAWNS.add(new OuterWorlds.Spawn(biome, SULFUR_CRAB, MobCategory.MONSTER, biome == JupiterData.IO_POLAR ? 10 : 50, 1, 3));
        }
    }

    public static List<DeferredItem<DeferredSpawnEggItem>> eggs() {
        return EGGS;
    }

    public static void translations(Lang lang) {
        lang.add("entity.spacebreaker.glow_serpent", "Under-Ice Glow Serpent", "Buzaltı Işık Yılanı");
        lang.item(serpentEgg, "Under-Ice Glow Serpent Spawn Egg", "Buzaltı Işık Yılanı Çağırma Yumurtası");
        lang.add("entity.spacebreaker.sulfur_crab", "Sulfur Crab", "Kükürt Yengeci");
        lang.item(crabEgg, "Sulfur Crab Spawn Egg", "Kükürt Yengeci Çağırma Yumurtası");
    }
}
