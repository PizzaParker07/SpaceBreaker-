package com.pizzaparker07.spacebreaker.content.venus.entity;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.venus.VenusClimate;
import com.pizzaparker07.spacebreaker.content.venus.VenusModule;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
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

/** Creatures of the Venusian cloud deck. They live in the air, so they are spawned around players in the clouds. */
public final class VenusEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Spacebreaker.MOD_ID);
    private static final List<DeferredItem<DeferredSpawnEggItem>> EGGS = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<AcidRay>> ACID_RAY = ENTITIES.register("acid_ray",
            () -> EntityType.Builder.of(AcidRay::new, MobCategory.CREATURE).sized(2.2F, 0.5F).eyeHeight(0.3F).clientTrackingRange(10)
                    .build("acid_ray"));
    public static final DeferredHolder<EntityType<?>, EntityType<CloudJelly>> CLOUD_JELLY = ENTITIES.register("cloud_jelly",
            () -> EntityType.Builder.of(CloudJelly::new, MobCategory.CREATURE).sized(1.4F, 2.2F).eyeHeight(1.6F).clientTrackingRange(10)
                    .build("cloud_jelly"));

    public static final DeferredHolder<EntityType<?>, EntityType<DescentProbe>> DESCENT_PROBE = ENTITIES.register("descent_probe",
            () -> EntityType.Builder.<DescentProbe>of(DescentProbe::new, MobCategory.MISC).sized(0.6F, 0.6F).clientTrackingRange(16)
                    .updateInterval(2).build("descent_probe"));

    private VenusEntities() {
    }

    public static void register(IEventBus modBus, DeferredRegister.Items items) {
        EGGS.add(items.register("acid_ray_spawn_egg", () -> new DeferredSpawnEggItem(ACID_RAY, 0x8C8A3A, 0xD8D27A, new Item.Properties())));
        EGGS.add(items.register("cloud_jelly_spawn_egg", () -> new DeferredSpawnEggItem(CLOUD_JELLY, 0xE6DDB8, 0xB78DB0, new Item.Properties())));
        ENTITIES.register(modBus);
        modBus.addListener(EntityAttributeCreationEvent.class, e -> {
            e.put(ACID_RAY.get(), AcidRay.attributes().build());
            e.put(CLOUD_JELLY.get(), CloudJelly.attributes().build());
        });
        modBus.addListener(RegisterSpawnPlacementsEvent.class, e -> {
            e.register(ACID_RAY.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, (t, l, r, p, rnd) -> true,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            e.register(CLOUD_JELLY.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, (t, l, r, p, rnd) -> true,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, e -> {
            if (e.getLevel() instanceof ServerLevel level) {
                skySpawns(level);
            }
        });
    }

    /** Keeps a few jellies and rays drifting around players who are up in the clouds. */
    static void skySpawns(ServerLevel level) {
        if (level.dimension() != VenusModule.VENUS || level.getGameTime() % 100 != 0 || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.getY() < VenusClimate.CLOUD_BASE - 30) {
                continue;
            }
            var box = player.getBoundingBox().inflate(64, 40, 64);
            int jellies = level.getEntities(CLOUD_JELLY.get(), box, x -> true).size();
            int rays = level.getEntities(ACID_RAY.get(), box, x -> true).size();
            EntityType<? extends Mob> type = jellies < 4 ? CLOUD_JELLY.get() : rays < 2 ? ACID_RAY.get() : null;
            if (type == null || level.random.nextInt(3) != 0) {
                continue;
            }
            double a = level.random.nextDouble() * Math.PI * 2;
            double d = 24 + level.random.nextDouble() * 24;
            int x = (int) (player.getX() + Math.cos(a) * d);
            int z = (int) (player.getZ() + Math.sin(a) * d);
            int y = VenusClimate.CLOUD_BASE + 6 + level.random.nextInt(VenusClimate.CLOUD_TOP - VenusClimate.CLOUD_BASE);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isLoaded(pos) || !SkyFloater.openAir(level, pos, 2)) {
                continue;
            }
            Mob mob = type.create(level);
            if (mob != null) {
                mob.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360F, 0F);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
                level.addFreshEntity(mob);
            }
        }
    }

    public static List<DeferredItem<DeferredSpawnEggItem>> eggs() {
        return EGGS;
    }

    public static void translations(Lang lang) {
        lang.add(ACID_RAY.get().getDescriptionId(), "Acid Ray", "Asit Vatozu");
        lang.add(CLOUD_JELLY.get().getDescriptionId(), "Cloud Jelly", "Bulut Medüzü");
        lang.item(EGGS.get(0), "Acid Ray Spawn Egg", "Asit Vatozu Çağırma Yumurtası");
        lang.item(EGGS.get(1), "Cloud Jelly Spawn Egg", "Bulut Medüzü Çağırma Yumurtası");
    }
}
