package com.pizzaparker07.spacebreaker.content.research;

import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Side-neutral cache of what the local client knows (filled by the sync packet). Safe to reference from common code
 * such as tooltips; on a dedicated server it simply stays empty.
 */
public final class ResearchClientData {
    private static volatile Set<ResourceLocation> known = Set.of();

    public static void setKnown(Set<ResourceLocation> techs) {
        known = Set.copyOf(new HashSet<>(techs));
    }

    public static Set<ResourceLocation> known() {
        return known;
    }

    public static boolean isKnownOnClient(ResourceLocation tech) {
        return known.contains(tech);
    }

    /** Branch of a technology: from the synced registry when available, else from the built-in tree. */
    public static @Nullable String categoryOf(ResourceLocation tech, @Nullable Item.TooltipContext context) {
        if (context != null) {
            HolderLookup.Provider regs = context.registries();
            if (regs != null) {
                var lookup = regs.lookup(Tech.REGISTRY);
                if (lookup.isPresent()) {
                    var holder = lookup.get().get(net.minecraft.resources.ResourceKey.create(Tech.REGISTRY, tech));
                    if (holder.isPresent()) {
                        return holder.get().value().category();
                    }
                }
            }
        }
        ResearchTree.Def def = ResearchTree.all().get(tech);
        if (def != null) {
            TechNode node = def.node();
            return node.category();
        }
        return null;
    }

    private ResearchClientData() {
    }
}
