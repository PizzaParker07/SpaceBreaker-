package com.pizzaparker07.spacebreaker.registry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Spacebreaker.MOD_ID);

    /** Mk1 EVA suit: vacuum rated, light protection — it is a pressure garment, not combat armour. */
    public static final Holder<ArmorMaterial> EVA_SUIT = MATERIALS.register("eva_suit", () -> new ArmorMaterial(
            Util.defense(1, 2, 3, 1, 3),
            9,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(ModItems.ALUMINUM_PLATE.get()),
            List.of(new ArmorMaterial.Layer(Spacebreaker.id("eva_suit"))),
            0.0F,
            0.0F));

    private static final class Util {
        static EnumMap<ArmorItem.Type, Integer> defense(int boots, int legs, int chest, int helmet, int body) {
            EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
            map.put(ArmorItem.Type.BOOTS, boots);
            map.put(ArmorItem.Type.LEGGINGS, legs);
            map.put(ArmorItem.Type.CHESTPLATE, chest);
            map.put(ArmorItem.Type.HELMET, helmet);
            map.put(ArmorItem.Type.BODY, body);
            return map;
        }
    }

    private ModArmorMaterials() {
    }
}
