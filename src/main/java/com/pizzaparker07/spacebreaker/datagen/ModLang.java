package com.pizzaparker07.spacebreaker.datagen;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.registry.ModBlocks;
import com.pizzaparker07.spacebreaker.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Every player-facing string, in English and Turkish side by side, so a missing translation
 * is impossible by construction. More languages can be added as extra columns later.
 */
public final class ModLang {
    /** key -> [en_us, tr_tr] */
    static final Map<String, String[]> ENTRIES = new LinkedHashMap<>();

    static {
        tab("main", "Spacebreaker", "Spacebreaker");

        block(ModBlocks.BAUXITE_ORE, "Bauxite Ore", "Boksit Cevheri");
        block(ModBlocks.DEEPSLATE_BAUXITE_ORE, "Deepslate Bauxite Ore", "Derin Kayrak Boksit Cevheri");
        block(ModBlocks.RAW_BAUXITE_BLOCK, "Block of Raw Bauxite", "Ham Boksit Bloğu");
        block(ModBlocks.LUNAR_REGOLITH, "Lunar Regolith", "Ay Regoliti");
        block(ModBlocks.MARE_BASALT, "Mare Basalt", "Mare Bazaltı");
        block(ModBlocks.ILMENITE_ORE, "Ilmenite Ore", "İlmenit Cevheri");
        block(ModBlocks.SOLAR_PANEL, "Solar Tracker", "Güneş Takipli Panel");

        item(ModItems.RAW_BAUXITE, "Raw Bauxite", "Ham Boksit");
        item(ModItems.ALUMINUM_INGOT, "Aluminum Ingot", "Alüminyum Külçe");
        item(ModItems.ALUMINUM_PLATE, "Aluminum Plate", "Alüminyum Levha");
        item(ModItems.OXYGEN_TANK, "Oxygen Tank", "Oksijen Tüpü");
        item(ModItems.EVA_HELMET, "EVA Helmet", "EVA Kaskı");

        add("tooltip.spacebreaker.oxygen", "Oxygen: %s / %s", "Oksijen: %s / %s");
        add("tooltip.spacebreaker.sun_visor.up", "Sun visor: raised", "Güneş vizörü: kalkık");
        add("tooltip.spacebreaker.sun_visor.down", "Sun visor: lowered", "Güneş vizörü: indirilmiş");
        add("tooltip.spacebreaker.sun_visor.hint", "Sneak + use to toggle the sun visor", "Güneş vizörünü indirip kaldırmak için eğilip kullan");
        add("tooltip.spacebreaker.solar_panel", "Follows the sun. Peak output: %s FE/t", "Güneşi takip eder. En yüksek üretim: %s FE/t");
    }

    private static void tab(String id, String en, String tr) {
        add("itemGroup." + Spacebreaker.MOD_ID + "." + id, en, tr);
    }

    private static void block(Supplier<? extends net.minecraft.world.level.block.Block> block, String en, String tr) {
        add(block.get().getDescriptionId(), en, tr);
    }

    private static void item(Supplier<? extends net.minecraft.world.item.Item> item, String en, String tr) {
        add(item.get().getDescriptionId(), en, tr);
    }

    static void add(String key, String en, String tr) {
        if (ENTRIES.put(key, new String[]{en, tr}) != null) {
            throw new IllegalStateException("Duplicate translation key " + key);
        }
    }

    static final class Provider extends LanguageProvider {
        private final int column;

        Provider(PackOutput output, String locale, int column) {
            super(output, Spacebreaker.MOD_ID, locale);
            this.column = column;
        }

        @Override
        protected void addTranslations() {
            ENTRIES.forEach((key, values) -> add(key, values[column]));
        }
    }

    private ModLang() {
    }
}
