package com.pizzaparker07.spacebreaker.core.module;

import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.neoforged.bus.api.IEventBus;

/**
 * A self-contained feature area (Earth materials, Moon, machines, suit, rocketry, ...).
 * Each module owns its registries, translations, loot and tags, so several people can work on
 * different modules in parallel without touching the same files. Register new modules in
 * {@link Modules#ALL}.
 */
public interface SbModule {
    /** Short id used in logs, e.g. "moon". */
    String id();

    /** Register DeferredRegisters and mod-bus listeners. Called from the mod constructor. */
    default void register(IEventBus modBus) {
    }

    /** English and Turkish strings for everything this module adds. */
    default void addTranslations(Lang lang) {
    }

    /** Block loot tables (datagen). */
    default void addBlockLoot(SbBlockLoot loot) {
    }

    /** Block and item tags (datagen). */
    default void addTags(SbTags tags) {
    }
}
