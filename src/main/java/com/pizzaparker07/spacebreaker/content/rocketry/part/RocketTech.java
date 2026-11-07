package com.pizzaparker07.spacebreaker.content.rocketry.part;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import com.pizzaparker07.spacebreaker.core.tech.TechRequirement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Which research a rocket part needs before it can go into the assembler. A technology that lists the
 * part's item id in its {@code unlocks} wins; otherwise this table applies (only if that technology exists,
 * so worlds without research data are never locked). Creative players are never gated.
 */
public final class RocketTech {
    private static final Map<String, String> PART_TECH = Map.ofEntries(
            Map.entry("nose_cone", "basic_rocketry"),
            Map.entry("crew_capsule", "basic_rocketry"),
            Map.entry("fins", "basic_rocketry"),
            Map.entry("kerosene_engine", "liquid_fuel_engines"),
            Map.entry("fuel_tank_small", "liquid_fuel_engines"),
            Map.entry("fuel_tank_medium", "liquid_fuel_engines"),
            Map.entry("fuel_tank_large", "liquid_fuel_engines"),
            Map.entry("decoupler", "staging"),
            Map.entry("interstage", "staging"),
            Map.entry("vacuum_engine", "vacuum_engines"),
            Map.entry("methalox_engine", "methane_engines"),
            Map.entry("methalox_vacuum_engine", "methane_engines"),
            Map.entry("hydrolox_engine", "hydrogen_engines"),
            Map.entry("hydrolox_vacuum_engine", "hydrogen_engines"),
            Map.entry("nuclear_thermal_engine", "nuclear_thermal_propulsion"),
            Map.entry("crew_capsule_large", "heavy_launch"),
            Map.entry("comm_satellite", "satellites"),
            Map.entry("observation_satellite", "satellites"),
            Map.entry("space_probe", "space_probes"),
            Map.entry("cargo_module", "space_stations"),
            Map.entry("landing_legs", "landing_systems"),
            Map.entry("parachute", "landing_systems"),
            Map.entry("heat_shield", "landing_systems"),
            Map.entry("rcs_thrusters", "guidance_systems"),
            Map.entry("grid_fins", "guidance_systems"));

    private RocketTech() {
    }

    /** The technology a part needs, or null when it is free (or the technology does not exist in this world). */
    @Nullable
    public static ResourceLocation techFor(Player player, ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        TechRequirement data = TechRequirement.forUnlock(player.level().registryAccess(), id);
        if (!data.isEmpty()) {
            return data.techs().get(0);
        }
        String tech = PART_TECH.get(id.getPath());
        if (tech == null || !id.getNamespace().equals(Spacebreaker.MOD_ID)) {
            return null;
        }
        ResourceLocation t = Spacebreaker.id(tech);
        return player.level().registryAccess().registry(Tech.REGISTRY).map(r -> r.containsKey(t)).orElse(false) ? t : null;
    }

    public static boolean unlocked(Player player, ItemStack stack) {
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!TechRequirement.forUnlock(player.level().registryAccess(), id).isEmpty()) {
            return TechRequirement.isUnlocked(player, id);
        }
        ResourceLocation tech = techFor(player, stack);
        return tech == null || Tech.knows(player, tech);
    }

    /** "Requires: <technology>" for tooltips and messages. */
    public static Component requires(ResourceLocation tech) {
        return Component.translatable("tooltip.spacebreaker.part.requires", Component.translatable(TechNode.nameKey(tech)));
    }
}
