package com.pizzaparker07.spacebreaker.content.dungeons;

import com.mojang.serialization.Codec;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/** Shared dungeon framework (see {@link Dungeons}) and Earth's first dungeon, the abandoned observatory. */
public final class DungeonsModule implements SbModule {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Spacebreaker.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, DungeonDiscoveredTrigger> DISCOVERED_TRIGGER =
            TRIGGERS.register("dungeon_discovered", DungeonDiscoveredTrigger::new);

    /** Dungeon instances this player has entered (instance keys). */
    public static final Supplier<AttachmentType<Set<String>>> DISCOVERED = ATTACHMENTS.register("discovered_dungeons",
            () -> AttachmentType.<Set<String>>builder(() -> new HashSet<>())
                    .serialize(Codec.STRING.listOf().xmap(HashSet::new, ArrayList::new)).copyOnDeath().build());

    @Override
    public String id() {
        return "dungeons";
    }

    @Override
    public void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
        ATTACHMENTS.register(modBus);
        modBus.addListener(Dungeons::registerRegistry);
        NeoForge.EVENT_BUS.addListener(DungeonTracker::onPlayerTick);
    }

    /** Test / command entry point: run discovery for a player standing in a dungeon. @return true if inside one */
    public static boolean discover(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        return Dungeons.find(level, player.blockPosition()).map(f -> {
            DungeonTracker.enter(level, player, f, DungeonTracker.instanceKey(level, f.id(), f.start()));
            return true;
        }).orElse(false);
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.add(DungeonDef.nameKey(Dungeons.EARTH_OBSERVATORY), "Abandoned Observatory", "Terk Edilmiş Gözlemevi");
        lang.add("message.spacebreaker.dungeon_discovered", "Discovered: %s", "Keşfedildi: %s");
    }
}
