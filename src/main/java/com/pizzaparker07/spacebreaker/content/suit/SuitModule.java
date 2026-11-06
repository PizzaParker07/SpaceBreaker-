package com.pizzaparker07.spacebreaker.content.suit;

import com.mojang.serialization.Codec;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/** EVA suits, oxygen tanks and life support equipment. */
public final class SuitModule implements SbModule {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Spacebreaker.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Spacebreaker.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Spacebreaker.MOD_ID);

    /** Stored oxygen, in breathing ticks (20 = one second of breathing). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> OXYGEN =
            COMPONENTS.registerComponentType("oxygen", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** Whether the gold sun visor of a helmet is lowered. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SUN_VISOR =
            COMPONENTS.registerComponentType("sun_visor", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Mk1 EVA suit: vacuum rated, light protection — a pressure garment, not combat armour. */
    public static final Holder<ArmorMaterial> EVA_SUIT = MATERIALS.register("eva_suit", () -> new ArmorMaterial(
            defense(1, 2, 3, 1, 3), 9, SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(EarthModule.ALUMINUM_PLATE.get()),
            List.of(new ArmorMaterial.Layer(Spacebreaker.id("eva_suit"))), 0.0F, 0.0F));

    public static final DeferredItem<OxygenTankItem> OXYGEN_TANK = ITEMS.registerItem("oxygen_tank",
            p -> new OxygenTankItem(p, OxygenTankItem.SMALL_CAPACITY), new Item.Properties().stacksTo(1));
    public static final DeferredItem<SpaceSuitItem> EVA_HELMET = ITEMS.registerItem("eva_helmet",
            p -> new SpaceSuitItem(EVA_SUIT, ArmorItem.Type.HELMET, p),
            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(20)).rarity(Rarity.UNCOMMON));

    @Override
    public String id() {
        return "suit";
    }

    @Override
    public void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        MATERIALS.register(modBus);
        ITEMS.register(modBus);
        SbTabs.EQUIPMENT.icon(EVA_HELMET).add(EVA_HELMET).addStack(() -> OxygenTankItem.filled(OXYGEN_TANK.get()));
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.add("itemGroup.spacebreaker.equipment", "Spacebreaker: Equipment", "Spacebreaker: Ekipman");
        lang.item(OXYGEN_TANK, "Oxygen Tank", "Oksijen Tüpü");
        lang.item(EVA_HELMET, "EVA Helmet", "EVA Kaskı");
        lang.add("tooltip.spacebreaker.oxygen", "Oxygen: %s / %s", "Oksijen: %s / %s");
        lang.add("tooltip.spacebreaker.sun_visor.up", "Sun visor: raised", "Güneş vizörü: kalkık");
        lang.add("tooltip.spacebreaker.sun_visor.down", "Sun visor: lowered", "Güneş vizörü: indirilmiş");
        lang.add("tooltip.spacebreaker.sun_visor.hint", "Sneak + use to toggle the sun visor", "Güneş vizörünü indirip kaldırmak için eğilip kullan");
    }

    @Override
    public void addTags(SbTags tags) {
        tags.item(ItemTags.HEAD_ARMOR, EVA_HELMET.get());
    }

    private static EnumMap<ArmorItem.Type, Integer> defense(int boots, int legs, int chest, int helmet, int body) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.BOOTS, boots);
        map.put(ArmorItem.Type.LEGGINGS, legs);
        map.put(ArmorItem.Type.CHESTPLATE, chest);
        map.put(ArmorItem.Type.HELMET, helmet);
        map.put(ArmorItem.Type.BODY, body);
        return map;
    }
}
