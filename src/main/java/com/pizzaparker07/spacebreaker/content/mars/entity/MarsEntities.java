package com.pizzaparker07.spacebreaker.content.mars.entity;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.MarsData;
import com.pizzaparker07.spacebreaker.content.mars.MarsWeather;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/** Creatures and guardians of Mars and its moons. */
public final class MarsEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Spacebreaker.MOD_ID);
    private static final List<DeferredItem<DeferredSpawnEggItem>> EGGS = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<RustBeetle>> RUST_BEETLE = ENTITIES.register("rust_beetle",
            () -> EntityType.Builder.of(RustBeetle::new, MobCategory.CREATURE).sized(0.75F, 0.5F).eyeHeight(0.3F).clientTrackingRange(8)
                    .build("rust_beetle"));
    public static final DeferredHolder<EntityType<?>, EntityType<DustDevil>> DUST_DEVIL = ENTITIES.register("dust_devil",
            () -> EntityType.Builder.of(DustDevil::new, MobCategory.MONSTER).sized(1.1F, 2.6F).eyeHeight(1.6F).clientTrackingRange(10)
                    .build("dust_devil"));
    public static final DeferredHolder<EntityType<?>, EntityType<CanyonLurker>> CANYON_LURKER = ENTITIES.register("canyon_lurker",
            () -> EntityType.Builder.of(CanyonLurker::new, MobCategory.MONSTER).sized(1.2F, 0.75F).eyeHeight(0.55F).clientTrackingRange(8)
                    .build("canyon_lurker"));
    public static final DeferredHolder<EntityType<?>, EntityType<Stonegrazer>> STONEGRAZER = ENTITIES.register("stonegrazer",
            () -> EntityType.Builder.of(Stonegrazer::new, MobCategory.CREATURE).sized(1.6F, 1.5F).eyeHeight(1.1F).clientTrackingRange(10)
                    .build("stonegrazer"));
    public static final DeferredHolder<EntityType<?>, EntityType<PolarWorm>> POLAR_WORM = ENTITIES.register("polar_worm",
            () -> EntityType.Builder.of(PolarWorm::new, MobCategory.CREATURE).sized(0.7F, 0.5F).eyeHeight(0.35F).clientTrackingRange(8)
                    .build("polar_worm"));

    private MarsEntities() {
    }

    public static void register(IEventBus modBus, DeferredRegister.Items items) {
        egg(items, "rust_beetle_spawn_egg", RUST_BEETLE, 0x8A4224, 0x221D1D);
        egg(items, "dust_devil_spawn_egg", DUST_DEVIL, 0xA8744E, 0xD4A57C);
        egg(items, "canyon_lurker_spawn_egg", CANYON_LURKER, 0x7E4B33, 0xF5C040);
        egg(items, "stonegrazer_spawn_egg", STONEGRAZER, 0x716157, 0x80533C);
        egg(items, "polar_worm_spawn_egg", POLAR_WORM, 0xB4BDC6, 0x45303A);
        ENTITIES.register(modBus);
        modBus.addListener(EntityAttributeCreationEvent.class, e -> {
            e.put(RUST_BEETLE.get(), RustBeetle.attributes().build());
            e.put(DUST_DEVIL.get(), DustDevil.attributes().build());
            e.put(CANYON_LURKER.get(), CanyonLurker.attributes().build());
            e.put(STONEGRAZER.get(), Stonegrazer.attributes().build());
            e.put(POLAR_WORM.get(), PolarWorm.attributes().build());
        });
        modBus.addListener(RegisterSpawnPlacementsEvent.class, e -> {
            var op = RegisterSpawnPlacementsEvent.Operation.REPLACE;
            var hm = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
            e.register(RUST_BEETLE.get(), SpawnPlacementTypes.ON_GROUND, hm, RustBeetle::checkSpawn, op);
            e.register(CANYON_LURKER.get(), SpawnPlacementTypes.ON_GROUND, hm, CanyonLurker::checkSpawn, op);
            e.register(STONEGRAZER.get(), SpawnPlacementTypes.ON_GROUND, hm, Stonegrazer::checkSpawn, op);
            e.register(POLAR_WORM.get(), SpawnPlacementTypes.ON_GROUND, hm, PolarWorm::checkSpawn, op);
            e.register(DUST_DEVIL.get(), SpawnPlacementTypes.ON_GROUND, hm, (t, l, r, p, rnd) -> r != MobSpawnType.NATURAL, op);
        });
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, e -> {
            if (e.getLevel() instanceof ServerLevel level) {
                stormSpawns(level);
            }
        });
        spawns();
    }

    private static void spawns() {
        var s = MarsData.SPAWNS;
        s.add(new MarsData.BiomeSpawns(MarsData.HIGHLANDS, b -> {
            b.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(RUST_BEETLE.get(), 10, 2, 4));
            b.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(STONEGRAZER.get(), 4, 2, 3));
        }));
        s.add(new MarsData.BiomeSpawns(MarsData.PLAINS, b -> {
            b.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(RUST_BEETLE.get(), 14, 2, 5));
            b.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(STONEGRAZER.get(), 6, 2, 4));
        }));
        s.add(new MarsData.BiomeSpawns(MarsData.VALLEY, b -> {
            b.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(STONEGRAZER.get(), 10, 3, 5));
            b.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(RUST_BEETLE.get(), 8, 2, 4));
        }));
        s.add(new MarsData.BiomeSpawns(MarsData.CRATER, b -> {
            b.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(CANYON_LURKER.get(), 30, 1, 1));
            b.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(RUST_BEETLE.get(), 8, 2, 3));
        }));
        s.add(new MarsData.BiomeSpawns(MarsData.CANYON, b -> {
            b.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(CANYON_LURKER.get(), 80, 1, 2));
            b.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(STONEGRAZER.get(), 4, 1, 2));
        }));
        s.add(new MarsData.BiomeSpawns(MarsData.DUNES, b ->
                b.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(RUST_BEETLE.get(), 6, 1, 3))));
        s.add(new MarsData.BiomeSpawns(MarsData.VOLCANO, b ->
                b.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(RUST_BEETLE.get(), 6, 1, 3))));
        s.add(new MarsData.BiomeSpawns(MarsData.POLAR, b ->
                b.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(POLAR_WORM.get(), 6, 1, 2))));
        s.add(new MarsData.BiomeSpawns(MarsData.ICE_CAP, b ->
                b.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(POLAR_WORM.get(), 14, 1, 3))));
    }

    /** Dust devils form around players caught outside in a strong storm. */
    static void stormSpawns(ServerLevel level) {
        if (!MarsWeather.dusty(level) || level.getGameTime() % 80 != 0 || level.getDifficulty() == Difficulty.PEACEFUL
                || MarsWeather.stormStrength(level) < 0.45F || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || level.random.nextInt(3) != 0) {
                continue;
            }
            int near = level.getEntities(DUST_DEVIL.get(), player.getBoundingBox().inflate(48), e -> true).size();
            if (near >= 2) {
                continue;
            }
            double a = level.random.nextDouble() * Math.PI * 2;
            double d = 16 + level.random.nextDouble() * 16;
            int x = (int) (player.getX() + Math.cos(a) * d);
            int z = (int) (player.getZ() + Math.sin(a) * d);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (DustDevil.canSpawnAt(level, pos)) {
                DustDevil devil = DUST_DEVIL.get().create(level);
                if (devil != null) {
                    devil.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360F, 0F);
                    devil.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                    level.addFreshEntity(devil);
                }
            }
        }
    }

    private static void egg(DeferredRegister.Items items, String name, DeferredHolder<EntityType<?>, ? extends EntityType<? extends net.minecraft.world.entity.Mob>> type,
                            int a, int b) {
        EGGS.add(items.register(name, () -> new DeferredSpawnEggItem(type, a, b, new Item.Properties())));
    }

    public static List<DeferredItem<DeferredSpawnEggItem>> eggs() {
        return EGGS;
    }

    public static void translations(Lang lang) {
        lang.add(RUST_BEETLE.get().getDescriptionId(), "Rust Beetle", "Pas Böceği");
        lang.add(DUST_DEVIL.get().getDescriptionId(), "Dust Devil", "Toz Şeytanı");
        lang.add(CANYON_LURKER.get().getDescriptionId(), "Canyon Lurker", "Kanyon Pusucusu");
        lang.add(STONEGRAZER.get().getDescriptionId(), "Stonegrazer", "Taşyiyen");
        lang.add(POLAR_WORM.get().getDescriptionId(), "Polar Worm", "Kutup Kurtçuğu");
        String[][] eggs = {{"Rust Beetle", "Pas Böceği"}, {"Dust Devil", "Toz Şeytanı"}, {"Canyon Lurker", "Kanyon Pusucusu"},
                {"Stonegrazer", "Taşyiyen"}, {"Polar Worm", "Kutup Kurtçuğu"}};
        for (int i = 0; i < eggs.length; i++) {
            lang.item(EGGS.get(i), eggs[i][0] + " Spawn Egg", eggs[i][1] + " Çağırma Yumurtası");
        }
    }
}
