package com.pizzaparker07.spacebreaker.core.tech;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Gating API: "this needs technology X". Other areas use it for recipes, the fabricator/assembler,
 * machine upgrades or item use. Two ways to express a requirement:
 *
 * <ol>
 *   <li><b>Explicit</b> — {@code TechRequirement.of(Spacebreaker.id("liquid_fuel_engines"))}, or in JSON
 *       {@code "tech": "spacebreaker:liquid_fuel_engines"} / {@code "tech": ["a:x", "b:y"]} via {@link #CODEC}.
 *       Call {@link #test(Player)} (strict) or {@link #check(Player)} (creative players pass, others get an
 *       action-bar message).</li>
 *   <li><b>Data-driven</b> — list the item / recipe / feature id in a technology's {@code unlocks} array and call
 *       {@link #isUnlocked(Player, ResourceLocation)} or {@link #canUse(Player, ItemStack)}. Ids that no technology
 *       lists are always unlocked, so nothing is gated by accident.</li>
 * </ol>
 * All checks are server authoritative; the client keeps a synced copy for screens and tooltips.
 */
public record TechRequirement(List<ResourceLocation> techs) {
    public static final TechRequirement NONE = new TechRequirement(List.of());

    /** Accepts a single id or a list of ids. */
    public static final Codec<TechRequirement> CODEC = Codec.withAlternative(
            ExtraCodecs.nonEmptyList(ResourceLocation.CODEC.listOf()),
            ResourceLocation.CODEC.xmap(List::of, l -> l.get(0))
    ).xmap(TechRequirement::new, TechRequirement::techs);

    public static TechRequirement of(ResourceLocation... techs) {
        return new TechRequirement(List.of(techs));
    }

    /** Strict: the player knows every listed technology. */
    public boolean test(Player player) {
        for (ResourceLocation t : techs) {
            if (!Tech.knows(player, t)) {
                return false;
            }
        }
        return true;
    }

    /** Lenient: creative players pass; otherwise shows "Requires: ..." on the action bar when it fails. */
    public boolean check(Player player) {
        if (player.isCreative() || test(player)) {
            return true;
        }
        player.displayClientMessage(missingMessage(player), true);
        return false;
    }

    public List<ResourceLocation> missing(Player player) {
        return techs.stream().filter(t -> !Tech.knows(player, t)).toList();
    }

    public Component missingMessage(Player player) {
        MutableComponent names = Component.empty();
        List<ResourceLocation> missing = missing(player);
        for (int i = 0; i < missing.size(); i++) {
            if (i > 0) {
                names.append(", ");
            }
            names.append(Component.translatable(TechNode.nameKey(missing.get(i))));
        }
        return Component.translatable("message.spacebreaker.requires_tech", names).withStyle(ChatFormatting.RED);
    }

    // ------------------------------------------------------------------ data-driven ("unlocks")

    private static final Map<Registry<TechNode>, Map<ResourceLocation, List<ResourceLocation>>> INDEX = new WeakHashMap<>();

    /** Technologies whose {@code unlocks} list contains {@code thing} (empty = not gated). */
    public static TechRequirement forUnlock(RegistryAccess access, ResourceLocation thing) {
        Registry<TechNode> reg = access.registry(Tech.REGISTRY).orElse(null);
        if (reg == null) {
            return NONE;
        }
        Map<ResourceLocation, List<ResourceLocation>> index;
        synchronized (INDEX) {
            index = INDEX.computeIfAbsent(reg, r -> {
                Map<ResourceLocation, List<ResourceLocation>> m = new HashMap<>();
                r.entrySet().forEach(e -> e.getValue().unlocks().forEach(u ->
                        m.computeIfAbsent(u, k -> new ArrayList<>()).add(e.getKey().location())));
                return m;
            });
        }
        List<ResourceLocation> techs = index.get(thing);
        return techs == null ? NONE : new TechRequirement(List.copyOf(techs));
    }

    /** True if no technology gates {@code thing}, or the player knows at least one technology that unlocks it. */
    public static boolean isUnlocked(Player player, ResourceLocation thing) {
        TechRequirement req = forUnlock(player.level().registryAccess(), thing);
        if (req.techs.isEmpty()) {
            return true;
        }
        for (ResourceLocation t : req.techs) {
            if (Tech.knows(player, t)) {
                return true;
            }
        }
        return false;
    }

    /** {@link #isUnlocked} for an item stack's registry id; creative players always pass. */
    public static boolean canUse(Player player, ItemStack stack) {
        return player.isCreative() || isUnlocked(player, BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public boolean isEmpty() {
        return techs.isEmpty();
    }
}
