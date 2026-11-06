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
        lang.add("gui.spacebreaker.status.incomplete", "Structure blocked", "Yapı engellenmiş");
        lang.add("message.spacebreaker.pipe_extract_on", "Pipe now pulls from this side", "Boru artık bu taraftan çekiyor");
        lang.add("message.spacebreaker.pipe_extract_off", "Pipe no longer pulls from this side", "Boru artık bu taraftan çekmiyor");
        lang.add("tooltip.spacebreaker.cable", "Carries up to %s FE/t", "En fazla %s FE/t taşır");
        lang.add("tooltip.spacebreaker.pipe", "Carries up to %s mB/t. Sneak-use an end to pull from a block", "En fazla %s mB/t taşır. Bir bloktan çekmek için eğilip ucuna tıkla");
        lang.add("tooltip.spacebreaker.generator", "%s FE/t. Needs breathable air", "%s FE/t. Solunabilir hava gerekir");
    }

    private static void name(Lang lang, MachineEntry<?, ?> m, String en, String tr) {
        lang.block(m.block(), en, tr);
    }
}
