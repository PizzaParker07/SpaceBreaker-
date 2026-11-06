package com.pizzaparker07.spacebreaker.content.dungeons;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Watches players once a second: entering a dungeon triggers discovery, guardians and hooks. */
final class DungeonTracker {
    private static final Map<UUID, String> INSIDE = new HashMap<>();

    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || (player.tickCount + player.getId()) % 20 != 0 || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Optional<Dungeons.Found> found = Dungeons.find(level, player.blockPosition());
        if (found.isEmpty()) {
            INSIDE.remove(player.getUUID());
            return;
        }
        Dungeons.Found f = found.get();
        String key = instanceKey(level, f.id(), f.start());
        if (key.equals(INSIDE.put(player.getUUID(), key))) {
            return; // still inside the same one
        }
        enter(level, player, f, key);
    }

    static String instanceKey(ServerLevel level, ResourceLocation id, StructureStart start) {
        return level.dimension().location() + "|" + id + "|" + start.getChunkPos().x + "," + start.getChunkPos().z;
    }

    /** Runs the discovery logic for a player inside a dungeon instance (also used by GameTests). */
    static void enter(ServerLevel level, ServerPlayer player, Dungeons.Found f, String key) {
        Set<String> mine = player.getData(DungeonsModule.DISCOVERED);
        if (!mine.contains(key)) {
            Set<String> copy = new HashSet<>(mine);
            copy.add(key);
            player.setData(DungeonsModule.DISCOVERED, copy);
            player.displayClientMessage(Component.translatable("message.spacebreaker.dungeon_discovered",
                    Component.translatable(DungeonDef.nameKey(f.id())).withStyle(ChatFormatting.GOLD)), true);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 1F, 0.7F);
        }
        DungeonsModule.DISCOVERED_TRIGGER.get().trigger(player, f.id(), f.def().world());
        if (DungeonSavedData.get(level).markDiscovered(key)) {
            spawnGuardians(level, f.def(), f.start());
            DungeonHooks.fireFirst(f.id(), level, f.start(), player, f.def());
        }
        DungeonHooks.fireEnter(f.id(), level, f.start(), player, f.def());
    }

    /** Spawns the JSON-defined guardians around the structure centre (not in peaceful). @return how many spawned */
    static int spawnGuardians(ServerLevel level, DungeonDef def, StructureStart start) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || def.guardians().isEmpty()) {
            return 0;
        }
        BoundingBox box = start.getBoundingBox();
        BlockPos centre = box.getCenter();
        int spawned = 0;
        for (DungeonDef.Guardian g : def.guardians()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(g.entity()).orElse(null);
            if (type == null) {
                continue;
            }
            for (int i = 0; i < g.count(); i++) {
                for (int attempt = 0; attempt < 10; attempt++) {
                    int r = Math.max(1, def.guardianRadius());
                    int x = centre.getX() + level.random.nextInt(2 * r + 1) - r;
                    int z = centre.getZ() + level.random.nextInt(2 * r + 1) - r;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos pos = new BlockPos(x, y, z);
                    Entity e = type.create(level);
                    if (e == null) {
                        break;
                    }
                    e.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360F, 0F);
                    if (!level.noCollision(e)) {
                        e.discard();
                        continue;
                    }
                    if (e instanceof Mob mob) {
                        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
                        mob.setPersistenceRequired();
                        g.mainHand().flatMap(BuiltInRegistries.ITEM::getOptional)
                                .ifPresent(item -> mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item)));
                    }
                    level.addFreshEntity(e);
                    spawned++;
                    break;
                }
            }
        }
        return spawned;
    }

    private DungeonTracker() {
    }
}
