package com.pizzaparker07.spacebreaker.content.machines;

import com.pizzaparker07.spacebreaker.core.lang.Lang;

/** English and Turkish strings for machines, conduits and machine screens. */
final class MachinesLang {
    private MachinesLang() {
    }

    static void add(Lang lang) {
        name(lang, MachinesModule.COMBUSTION_GENERATOR, "Combustion Generator", "Yanmalı Jeneratör");
        name(lang, MachinesModule.BATTERY_BANK, "Battery Bank", "Batarya Bankası");
        name(lang, MachinesModule.CRUSHER, "Crusher", "Kırıcı");
        name(lang, MachinesModule.ARC_FURNACE, "Electric Arc Furnace", "Elektrik Ark Fırını");
        name(lang, MachinesModule.PRESS, "Press", "Pres");
        name(lang, MachinesModule.FABRICATOR, "Fabricator", "Parça Üretim Tezgâhı");
        name(lang, MachinesModule.ELECTROLYZER, "Electrolyzer", "Elektroliz Ünitesi");
        name(lang, MachinesModule.LIQUEFIER, "Cryogenic Liquefier", "Kriyojenik Sıvılaştırıcı");
        name(lang, MachinesModule.PUMPJACK, "Pumpjack", "Petrol Pompası");
        name(lang, MachinesModule.REFINERY, "Refinery", "Rafineri");
        name(lang, MachinesModule.ATMOSPHERE_PROCESSOR, "Atmosphere Processor", "Atmosfer İşlemcisi");
        name(lang, MachinesModule.SABATIER_REACTOR, "Sabatier Reactor", "Sabatier Reaktörü");
        name(lang, MachinesModule.REGOLITH_HEATER, "Regolith Heater", "Regolit Isıtıcı");
        name(lang, MachinesModule.ICE_MELTER, "Ice Melter", "Buz Eritici");
        name(lang, MachinesModule.CHEMICAL_REACTOR, "Chemical Reactor", "Kimyasal Reaktör");
        name(lang, MachinesModule.SINTERING_FURNACE, "Sintering Furnace", "Sinterleme Fırını");
        lang.add("tooltip.spacebreaker.sintering_furnace", "Fuses regolith into building blocks with electric heat, no air needed",
                "Regoliti elektrikle ısıtıp yapı bloğuna kaynaştırır, hava gerekmez");
        name(lang, MachinesModule.REGOLITH_EXCAVATOR, "Regolith Excavator", "Regolit Kazıcı");
        lang.add("tooltip.spacebreaker.regolith_excavator", "Digs a 5x5 patch below, 16 deep, and feeds the machines beside it",
                "Altındaki 5x5 alanı 16 blok derine kazar, yanındaki makinelere verir");
        lang.add("tooltip.spacebreaker.chemical_reactor", "One solid and two fluids in; e.g. sulfur + water + oxygen → sulfuric acid",
                "Bir katı ve iki sıvı girer; örn. kükürt + su + oksijen → sülfürik asit");
        lang.add("gui.spacebreaker.status.no_atmosphere", "No usable atmosphere", "Kullanılabilir atmosfer yok");
        lang.add("tooltip.spacebreaker.atmosphere_processor", "Separates the main useful gas of the local air; needs open air above",
                "Yerel havadaki başlıca yararlı gazı ayırır; üstü açık olmalı");
        lang.add("tooltip.spacebreaker.sabatier_reactor", "CO₂ + H₂ → methane + water", "CO₂ + H₂ → metan + su");
        lang.add("tooltip.spacebreaker.regolith_heater", "Molten regolith electrolysis: oxygen from rock", "Erimiş regolit elektrolizi: kayadan oksijen");
        lang.add("tooltip.spacebreaker.ice_melter", "Ice to clean water; dry ice to carbon dioxide", "Buzdan temiz su; kuru buzdan karbondioksit");
        com.pizzaparker07.spacebreaker.content.earth.EarthModule.tag(lang, IsruRecipes.REGOLITHS, "Regoliths", "Regolitler");
        com.pizzaparker07.spacebreaker.content.earth.EarthModule.tag(lang, IsruRecipes.ICES, "Ices", "Buzlar");
        com.pizzaparker07.spacebreaker.content.earth.EarthModule.tag(lang, IsruRecipes.DRY_ICES, "Dry Ices", "Kuru Buzlar");
        com.pizzaparker07.spacebreaker.content.earth.EarthModule.tag(lang, IsruRecipes.PERCHLORATE_SOILS, "Perchlorate Soils", "Perkloratlı Topraklar");
        lang.add("multiblock.spacebreaker.pumpjack", "Pumpjack", "Petrol Pompası");
        lang.add("multiblock.spacebreaker.refinery", "Refinery Tower", "Rafineri Kulesi");
        lang.block(MachinesModule.PART, "Machine Part", "Makine Parçası");
        lang.add("tooltip.spacebreaker.multiblock", "Needs free space around it (%s)", "Çevresinde boş alan gerekir (%s)");
        lang.block(MachinesModule.FLUID_TANK, "Fluid Tank", "Sıvı Tankı");
        lang.block(MachinesModule.PRESSURIZED_TANK, "Pressurized Gas Tank", "Basınçlı Gaz Tankı");
        lang.block(MachinesModule.CRYOGENIC_TANK, "Cryogenic Tank", "Kriyojenik Tank");
        lang.add("tooltip.spacebreaker.tank.fluid", "Holds %s mB of a liquid", "%s mB sıvı alır");
        lang.add("tooltip.spacebreaker.tank.pressurized", "Holds %s mB of a gas", "%s mB gaz alır");
        lang.add("tooltip.spacebreaker.tank.cryogenic", "Holds %s mB of a cryogenic liquid", "%s mB kriyojenik sıvı alır");
        lang.block(MachinesModule.COPPER_CABLE, "Copper Cable", "Bakır Kablo");
        lang.block(MachinesModule.HV_CABLE, "High-Voltage Cable", "Yüksek Gerilim Kablosu");
        lang.block(MachinesModule.FLUID_PIPE, "Fluid Pipe", "Sıvı Borusu");
        lang.block(MachinesModule.GAS_PIPE, "Gas Pipe", "Gaz Borusu");

        lang.add("gui.spacebreaker.energy", "Energy: %s / %s FE", "Enerji: %s / %s FE");
        lang.add("gui.spacebreaker.tank", "%s: %s / %s mB", "%s: %s / %s mB");
        lang.add("gui.spacebreaker.tank_empty", "Empty: 0 / %s mB", "Boş: 0 / %s mB");
        lang.add("gui.spacebreaker.progress", "Progress: %s%%", "İlerleme: %%%s");
        lang.add("gui.spacebreaker.status.no_power", "No power", "Enerji yok");
        lang.add("gui.spacebreaker.status.output_full", "Output full", "Çıkış dolu");
        lang.add("gui.spacebreaker.status.no_oxygen", "No oxygen: fire needs air", "Oksijen yok: yanma için hava gerekir");
        lang.add("gui.spacebreaker.status.no_oil", "No crude oil below", "Altında ham petrol yok");
        lang.add("gui.spacebreaker.status.no_fuel", "No fuel", "Yakıt yok");
        lang.add("gui.spacebreaker.status.locked", "Needs research", "Araştırma gerekli");
        lang.add("gui.spacebreaker.status.redstone", "Stopped by redstone", "Kızıltaşla durduruldu");
        lang.add("gui.spacebreaker.redstone.0", "Redstone: ignored", "Kızıltaş: yok sayılır");
        lang.add("gui.spacebreaker.redstone.1", "Redstone: runs with a signal", "Kızıltaş: sinyal varken çalışır");
        lang.add("gui.spacebreaker.redstone.2", "Redstone: runs without a signal", "Kızıltaş: sinyal yokken çalışır");
        lang.add("gui.spacebreaker.requires", "Requires: %s", "Gerekli: %s");
        lang.add("gui.spacebreaker.status.incomplete", "Structure blocked", "Yapı engellenmiş");
        lang.add("message.spacebreaker.pipe_extract_on", "Pipe now pulls from this side", "Boru artık bu taraftan çekiyor");
        lang.add("message.spacebreaker.pipe_filter_on", "This end now only passes %s", "Bu uç artık yalnız %s geçirir");
        lang.add("message.spacebreaker.pipe_filter_off", "Filter cleared", "Filtre kaldırıldı");
        lang.add("message.spacebreaker.pipe_extract_off", "Pipe no longer pulls from this side", "Boru artık bu taraftan çekmiyor");
        lang.add("tooltip.spacebreaker.cable", "Carries up to %s FE/t", "En fazla %s FE/t taşır");
        lang.add("tooltip.spacebreaker.pipe", "Carries up to %s mB/t. Sneak-use an end: pull from a block or an endless water source; with a filled container: let only that fluid through",
                "En fazla %s mB/t taşır. Eğilip ucuna tıkla: bloktan ya da sonsuz su kaynağından çeker; dolu bir kapla: yalnız o sıvıyı geçirir");
        lang.add("tooltip.spacebreaker.electrolyzer", "Hydrogen leaves on the left, oxygen on the right (seen from the front)",
                "Hidrojen soldan, oksijen sağdan çıkar (önden bakınca)");
        lang.add("tooltip.spacebreaker.generator", "%s FE/t. Needs breathable air", "%s FE/t. Solunabilir hava gerekir");
        lang.add("tooltip.spacebreaker.tank.contents", "Contains %s: %s mB (kept when broken)", "İçinde %s: %s mB (kırınca korunur)");
        name(lang, MachinesModule.RTG, "Radioisotope Generator (RTG)", "Radyoizotop Jeneratör (RTG)");
        lang.add("tooltip.spacebreaker.rtg", "Up to %s FE/t, day and night, anywhere. Shielded casing",
                "En fazla %s FE/t; gece gündüz, her yerde. Kalkanlı gövde");
        lang.item(MachinesModule.RTG_FUEL_PELLET, "RTG Fuel Pellet", "RTG Yakıt Peleti");
        lang.add("tooltip.spacebreaker.rtg_fuel_pellet.activity", "Activity: %s%% (%s FE/t in an RTG)", "Etkinlik: %%%s (RTG'de %s FE/t)");
        lang.add("tooltip.spacebreaker.rtg_fuel_pellet.half_life", "Half-life: %s days, also in storage", "Yarı ömür: %s gün, depoda da azalır");
        lang.add("tooltip.spacebreaker.rtg_fuel_pellet.radiation", "Radioactive: a small dose while carried", "Radyoaktif: taşınırken küçük doz verir");
    }

    private static void name(Lang lang, MachineEntry<?, ?> m, String en, String tr) {
        lang.block(m.block(), en, tr);
    }
}
