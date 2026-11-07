package com.pizzaparker07.spacebreaker.content.suit;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Titanium tools for prospecting other worlds: between iron and diamond in hardness (they mine what iron mines,
 * a little faster), much tougher than iron and lighter than diamond (quicker to swing). Titanium keeps its
 * toughness in deep cold, so nothing about them changes on icy moons.
 */
public final class TitaniumTools {
    /** Repair material: titanium ingots from the arc furnace (ilmenite / rutile). */
    public static final TagKey<Item> TITANIUM_INGOTS = TagKey.create(Registries.ITEM,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/titanium"));

    /** Iron: 250 uses, speed 6, +2 damage. Diamond: 1561, 8, +3. */
    public static final Tier TITANIUM = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 1100, 7.0F, 2.5F, 16,
            () -> Ingredient.of(TITANIUM_INGOTS));

    static DeferredItem<PickaxeItem> PICKAXE;
    static DeferredItem<AxeItem> AXE;
    static DeferredItem<ShovelItem> SHOVEL;
    static DeferredItem<HoeItem> HOE;
    static DeferredItem<SwordItem> SWORD;

    private TitaniumTools() {
    }

    /** Registers the five tools; attack speeds a touch quicker than iron (lighter metal). */
    static void register(DeferredRegister.Items items) {
        PICKAXE = items.registerItem("titanium_pickaxe", p -> new PickaxeItem(TITANIUM, p.attributes(PickaxeItem.createAttributes(TITANIUM, 1.0F, -2.7F))));
        AXE = items.registerItem("titanium_axe", p -> new AxeItem(TITANIUM, p.attributes(AxeItem.createAttributes(TITANIUM, 5.5F, -3.0F))));
        SHOVEL = items.registerItem("titanium_shovel", p -> new ShovelItem(TITANIUM, p.attributes(ShovelItem.createAttributes(TITANIUM, 1.5F, -2.9F))));
        HOE = items.registerItem("titanium_hoe", p -> new HoeItem(TITANIUM, p.attributes(HoeItem.createAttributes(TITANIUM, -2.5F, -0.5F))));
        SWORD = items.registerItem("titanium_sword", p -> new SwordItem(TITANIUM, p.attributes(SwordItem.createAttributes(TITANIUM, 3, -2.3F))));
    }

    public static List<DeferredItem<? extends Item>> all() {
        return List.of(PICKAXE, AXE, SHOVEL, HOE, SWORD);
    }
}
