package com.pizzaparker07.spacebreaker.compat;

import com.pizzaparker07.spacebreaker.compat.recipes.RecipeViewerData;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;

/** Strings of the optional integrations (recipe viewer categories and info pages). Registers nothing. */
public final class CompatModule implements SbModule {
    /** Category titles: English, Turkish. */
    private static final java.util.Map<String, String[]> TITLES = new java.util.TreeMap<>(java.util.Map.ofEntries(
            java.util.Map.entry("crushing", new String[]{"Crushing", "Kırma"}),
            java.util.Map.entry("arc_smelting", new String[]{"Arc Smelting", "Ark Ergitme"}),
            java.util.Map.entry("pressing", new String[]{"Pressing", "Presleme"}),
            java.util.Map.entry("fabricating", new String[]{"Fabricating", "İmalat"}),
            java.util.Map.entry("refining", new String[]{"Refining", "Rafinaj"}),
            java.util.Map.entry("electrolysis", new String[]{"Electrolysis", "Elektroliz"}),
            java.util.Map.entry("liquefaction", new String[]{"Cryogenic Liquefaction", "Kriyojenik Sıvılaştırma"}),
            java.util.Map.entry("sabatier", new String[]{"Sabatier Reaction", "Sabatier Tepkimesi"}),
            java.util.Map.entry("regolith_processing", new String[]{"Regolith Heating", "Regolit Isıtma"}),
            java.util.Map.entry("melting", new String[]{"Ice Melting", "Buz Eritme"}),
            java.util.Map.entry("chemical_reaction", new String[]{"Chemical Reaction", "Kimyasal Tepkime"}),
            java.util.Map.entry("sintering", new String[]{"Sintering", "Sinterleme"})));

    /** True when every recipe viewer category has a title (checked by a GameTest). */
    public static boolean hasTitle(String categoryId) {
        return TITLES.containsKey(categoryId);
    }

    @Override
    public String id() {
        return "compat";
    }

    @Override
    public void addTranslations(Lang lang) {
        String p = "recipe_viewer.spacebreaker.";
        TITLES.forEach((id, names) -> lang.add(p + id, names[0], names[1]));
        lang.add(RecipeViewerData.KEY_SUIT_MODULES, "Suit Modules", "Giysi Modülleri");
        // EMI reads category names from emi.category.<namespace>.<path>
        for (var c : RecipeViewerData.CATEGORIES) {
            String[] names = TITLES.get(c.id());
            lang.add("emi.category.spacebreaker." + c.id(), names[0], names[1]);
        }
        lang.add("emi.category.spacebreaker." + RecipeViewerData.SUIT_MODULES, "Suit Modules", "Giysi Modülleri");
        tagNames(lang);
        lang.add(RecipeViewerData.KEY_ENERGY, "%s FE/t · %s FE", "%s FE/t · %s FE");
        lang.add(RecipeViewerData.KEY_TIME, "%s s", "%s sn");
        lang.add(RecipeViewerData.KEY_CHANCE, "%s%% chance", "%%%s olasılık");
        lang.add(RecipeViewerData.KEY_REQUIRES, "Research needed: %s", "Gereken araştırma: %s");
        lang.add(RecipeViewerData.KEY_TOOL, "Tool: not consumed", "Alet: harcanmaz");
        lang.add(RecipeViewerData.KEY_INFO_ASSEMBLER,
                "Rockets are not crafted. Place parts (engines, tanks, capsule, fins, parachutes...) in the Rocket Assembler, "
                        + "check the design's delta-v and thrust-to-weight on its screen, then assemble the rocket onto a launch pad. "
                        + "Parts come from the Fabricator; better parts need research.",
                "Roketler üretim masasında yapılmaz. Parçaları (motorlar, tanklar, kapsül, kanatçıklar, paraşütler...) Roket Montaj "
                        + "Tezgâhı'na yerleştir, ekranında tasarımın delta-v ve itki/ağırlık değerlerini kontrol et, sonra roketi fırlatma "
                        + "rampasına monte et. Parçalar İmalatçı'dan gelir; daha iyi parçalar araştırma ister.");
        lang.add(RecipeViewerData.KEY_INFO_ATMOSPHERE,
                "Compresses the local atmosphere and separates its most useful gas: carbon dioxide on Mars and Venus, oxygen "
                        + "from Earth's air, methane on Titan. Output grows with the gas share and the air pressure; it does nothing "
                        + "in vacuum. Needs power.",
                "Yerel atmosferi sıkıştırır ve en yararlı gazını ayırır: Mars ve Venüs'te karbondioksit, Dünya havasından oksijen, "
                        + "Titan'da metan. Üretim gazın oranı ve hava basıncıyla artar; boşlukta çalışmaz. Enerji ister.");
    }

    /**
     * Names of item tags other areas use in recipes but did not name; recipe viewers show them on tag slots (EMI
     * warns about every unnamed one). Only keys nobody added: this module comes last in {@code Modules.ALL}.
     */
    private static final String[][] TAG_NAMES = {
            {"c:dusts/perchlorate", "Perchlorate Dusts", "Perklorat Tozları"},
            {"c:dusts/sulfur", "Sulfur Dusts", "Kükürt Tozları"},
            {"c:gems/gypsum", "Gypsum", "Alçıtaşı"},
            {"c:ices/water", "Water Ices", "Su Buzları"},
            {"c:ores/ammonia", "Ammonia Hydrate Ores", "Amonyak Hidrat Cevherleri"},
            {"c:ores/chromite", "Chromite Ores", "Kromit Cevherleri"},
            {"c:ores/graphite", "Graphite Ores", "Grafit Cevherleri"},
            {"c:ores/gypsum", "Gypsum Ores", "Alçıtaşı Cevherleri"},
            {"c:ores/hematite", "Hematite Ores", "Hematit Cevherleri"},
            {"c:ores/ilmenite", "Ilmenite Ores", "İlmenit Cevherleri"},
            {"c:ores/kreep", "KREEP Ores", "KREEP Cevherleri"},
            {"c:ores/magnetite", "Magnetite Ores", "Manyetit Cevherleri"},
            {"c:ores/methane_clathrate", "Methane Clathrate Ores", "Metan Klatrat Cevherleri"},
            {"c:ores/salt", "Salt Ores", "Tuz Cevherleri"},
            {"c:ores/sulfur", "Sulfur Ores", "Kükürt Cevherleri"},
            {"c:ores/tholin", "Tholin Ores", "Tolin Cevherleri"},
            {"c:raw_materials/hematite", "Raw Hematite", "Ham Hematit"},
            {"c:raw_materials/ilmenite", "Raw Ilmenite", "Ham İlmenit"},
            {"c:raw_materials/magnetite", "Raw Magnetite", "Ham Manyetit"},
            {"c:regoliths/solar_wind", "Solar-Wind Regoliths", "Güneş Rüzgârı Regolitleri"},
            {"spacebreaker:researchable", "Researchable Items", "Araştırılabilir Eşyalar"},
    };

    private static void tagNames(Lang lang) {
        for (String[] t : TAG_NAMES) {
            String key = "tag.item." + t[0].replace(':', '.').replace('/', '.');
            if (!lang.entries().containsKey(key)) {
                lang.add(key, t[1], t[2]);
            }
        }
    }
}
