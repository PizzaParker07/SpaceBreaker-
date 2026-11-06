package com.pizzaparker07.spacebreaker.core.lang;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Collects every player-facing string in English and Turkish side by side, so a missing
 * translation is impossible by construction. More languages become extra columns later.
 */
public final class Lang {
    public static final int EN_US = 0;
    public static final int TR_TR = 1;

    private final Map<String, String[]> entries = new LinkedHashMap<>();

    public Lang add(String key, String en, String tr) {
        if (entries.put(key, new String[]{en, tr}) != null) {
            throw new IllegalStateException("Duplicate translation key " + key);
        }
        return this;
    }

    public Lang block(Supplier<? extends Block> block, String en, String tr) {
        return add(block.get().getDescriptionId(), en, tr);
    }

    public Lang item(Supplier<? extends Item> item, String en, String tr) {
        return add(item.get().getDescriptionId(), en, tr);
    }

    public Map<String, String[]> entries() {
        return entries;
    }
}
