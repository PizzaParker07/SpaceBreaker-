package com.pizzaparker07.spacebreaker.registry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.item.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.item.SpaceSuitItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Spacebreaker.MOD_ID);

    // ---- Materials
    public static final DeferredItem<Item> RAW_BAUXITE = ITEMS.registerSimpleItem("raw_bauxite");
    public static final DeferredItem<Item> ALUMINUM_INGOT = ITEMS.registerSimpleItem("aluminum_ingot");
    public static final DeferredItem<Item> ALUMINUM_PLATE = ITEMS.registerSimpleItem("aluminum_plate");

    // ---- Equipment
    public static final DeferredItem<OxygenTankItem> OXYGEN_TANK = ITEMS.registerItem("oxygen_tank",
            p -> new OxygenTankItem(p, OxygenTankItem.SMALL_CAPACITY), new Item.Properties().stacksTo(1));
    public static final DeferredItem<SpaceSuitItem> EVA_HELMET = ITEMS.registerItem("eva_helmet",
            p -> new SpaceSuitItem(ModArmorMaterials.EVA_SUIT, ArmorItem.Type.HELMET, p),
            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(20)).rarity(Rarity.UNCOMMON));

    // ---- Block items
    public static final DeferredItem<?> BAUXITE_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.BAUXITE_ORE);
    public static final DeferredItem<?> DEEPSLATE_BAUXITE_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_BAUXITE_ORE);
    public static final DeferredItem<?> RAW_BAUXITE_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.RAW_BAUXITE_BLOCK);
    public static final DeferredItem<?> LUNAR_REGOLITH = ITEMS.registerSimpleBlockItem(ModBlocks.LUNAR_REGOLITH);
    public static final DeferredItem<?> MARE_BASALT = ITEMS.registerSimpleBlockItem(ModBlocks.MARE_BASALT);
    public static final DeferredItem<?> ILMENITE_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.ILMENITE_ORE);
    public static final DeferredItem<?> SOLAR_PANEL = ITEMS.registerSimpleBlockItem(ModBlocks.SOLAR_PANEL);

    private ModItems() {
    }
}
