package com.pizzaparker07.spacebreaker.content.research;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.tech.TechNode;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Phase 1–3 technology tree, defined once with its English and Turkish texts. Datagen writes it to
 * {@code data/spacebreaker/spacebreaker/tech/*.json} (data packs can add or override nodes there) and to the
 * language files. Prerequisites may cross branches.
 */
public final class ResearchTree {
    public static final String PROPULSION = "propulsion";
    public static final String LIFE_SUPPORT = "life_support";
    public static final String INDUSTRY = "industry";
    public static final String ASTRONOMY = "astronomy";
    public static final String CONSTRUCTION = "construction";
    public static final String EXPLORATION = "exploration";
    /** Branch order in the research screen. */
    public static final List<String> CATEGORIES = List.of(PROPULSION, LIFE_SUPPORT, INDUSTRY, ASTRONOMY, CONSTRUCTION, EXPLORATION);

    /** Sample kinds: celestial body ids (a field sample from that world) or experiment ids. */
    public static final ResourceLocation EARTH = Spacebreaker.id("earth");
    public static final ResourceLocation MOON = Spacebreaker.id("moon");
    public static final ResourceLocation MARS = Spacebreaker.id("mars");
    public static final ResourceLocation VENUS = Spacebreaker.id("venus");
    public static final ResourceLocation MERCURY = Spacebreaker.id("mercury");
    public static final ResourceLocation PHOBOS = Spacebreaker.id("phobos");
    public static final ResourceLocation CERES = Spacebreaker.id("ceres");
    public static final ResourceLocation VESTA = Spacebreaker.id("vesta");
    public static final ResourceLocation IO = Spacebreaker.id("io");
    public static final ResourceLocation EUROPA = Spacebreaker.id("europa");
    public static final ResourceLocation GANYMEDE = Spacebreaker.id("ganymede");
    public static final ResourceLocation CALLISTO = Spacebreaker.id("callisto");
    public static final ResourceLocation TITAN = Spacebreaker.id("titan");
    public static final ResourceLocation ENCELADUS = Spacebreaker.id("enceladus");
    public static final ResourceLocation METEORITE = Spacebreaker.id("meteorite");
    public static final ResourceLocation SUBORBITAL = Spacebreaker.id("suborbital_flight");
    public static final ResourceLocation ORBIT = Spacebreaker.id("orbital_flight");
    /** Flight records from uncrewed missions (rocketry: observation satellites and landing probes). */
    public static final ResourceLocation ORBITAL_SURVEY = Spacebreaker.id("orbital_survey");
    public static final ResourceLocation PROBE_LANDING = Spacebreaker.id("probe_landing");

    public record Def(ResourceLocation id, TechNode node, String en, String tr, String enDesc, String trDesc) {
    }

    private static final Map<ResourceLocation, Def> DEFS = new LinkedHashMap<>();

    public static Map<ResourceLocation, Def> all() {
        return DEFS;
    }

    public static ResourceLocation id(String path) {
        return Spacebreaker.id(path);
    }

    // ---------------------------------------------------------------- builder

    private static final class B {
        final String path;
        final String category;
        final int tier;
        final List<ResourceLocation> requires = new ArrayList<>();
        final List<ResourceLocation> unlocks = new ArrayList<>();
        final List<ResourceLocation> samples = new ArrayList<>();
        String icon;
        String en, tr, enDesc, trDesc;

        B(String path, String category, int tier) {
            this.path = path;
            this.category = category;
            this.tier = tier;
        }

        B req(String... paths) {
            for (String p : paths) {
                requires.add(id(p));
            }
            return this;
        }

        B icon(String item) {
            this.icon = item;
            return this;
        }

        B unlocks(String... ids) {
            for (String s : ids) {
                unlocks.add(ResourceLocation.parse(s.contains(":") ? s : "spacebreaker:" + s));
            }
            return this;
        }

        B samples(ResourceLocation... kinds) {
            samples.addAll(List.of(kinds));
            return this;
        }

        B en(String name, String desc) {
            en = name;
            enDesc = desc;
            return this;
        }

        void tr(String name, String desc) {
            tr = name;
            trDesc = desc;
            ResourceLocation rl = id(path);
            TechNode node = new TechNode(category, tier, List.copyOf(requires), Optional.ofNullable(icon).map(ResourceLocation::parse),
                    List.copyOf(unlocks), 0, 0, List.copyOf(samples));
            if (DEFS.put(rl, new Def(rl, node, en, tr, enDesc, trDesc)) != null) {
                throw new IllegalStateException("duplicate tech " + rl);
            }
        }
    }

    private static B t(String path, String category, int tier) {
        return new B(path, category, tier);
    }

    static {
        // ============================================================ PROPULSION
        t("basic_rocketry", PROPULSION, 1).icon("minecraft:firework_rocket").unlocks("rocket_assembler", "nose_cone", "fins", "crew_capsule", "emergency_beacon")
                .en("Basic Rocketry", "Action and reaction: why a rocket flies, and how thrust, mass and burn time fit together.")
                .tr("Temel Roketçilik", "Etki ve tepki: bir roket neden uçar; itki, kütle ve yanma süresi birbirine nasıl bağlıdır.");
        t("solid_rocket_motors", PROPULSION, 1).req("basic_rocketry").icon("minecraft:gunpowder")
                .samples(EARTH)
                .en("Solid Rocket Motors", "Cast propellant grains: simple, powerful, and impossible to switch off once lit.")
                .tr("Katı Yakıtlı Motorlar", "Dökme yakıt blokları: basit, güçlü ve bir kez ateşlenince durdurulamaz.");
        t("liquid_fuel_engines", PROPULSION, 1).req("basic_rocketry", "fuel_refining").icon("spacebreaker:kerosene_engine")
                .samples(EARTH)
                .unlocks("kerosene_engine", "fuel_tank_small", "fuel_tank_medium", "fuel_tank_large")
                .en("Liquid-Fuel Engines", "Pump RP-1 and liquid oxygen into a combustion chamber for throttleable, restartable thrust.")
                .tr("Sıvı Yakıtlı Motorlar", "RP-1 ve sıvı oksijeni yanma odasına pompala: kısılabilen, yeniden ateşlenebilen itki.");
        t("staging", PROPULSION, 1).req("liquid_fuel_engines").icon("spacebreaker:decoupler").unlocks("decoupler", "interstage")
                .samples(EARTH)
                .en("Staging", "Drop empty tanks on the way up. The rocket equation rewards every kilogram left behind.")
                .tr("Kademelendirme", "Boşalan tankları yolda bırak. Roket denklemi geride bırakılan her kilogramı ödüllendirir.");
        t("vacuum_engines", PROPULSION, 1).req("staging").icon("spacebreaker:vacuum_engine").unlocks("vacuum_engine")
                .samples(SUBORBITAL)
                .en("Vacuum Engines", "Long nozzles that expand exhaust fully in space for a much higher specific impulse.")
                .tr("Vakum Motorları", "Egzozu uzayda tamamen genleştiren uzun lüleler: çok daha yüksek özgül itki.");
        t("guidance_systems", PROPULSION, 1).req("basic_rocketry", "electronics").icon("spacebreaker:rcs_thrusters")
                .samples(EARTH)
                .unlocks("rcs_thrusters", "grid_fins")
                .en("Guidance Systems", "Gyroscopes, a flight computer and small thrusters to hold attitude and fly a gravity turn.")
                .tr("Güdüm Sistemleri", "Jiroskoplar, uçuş bilgisayarı ve küçük iticilerle duruşu koru, yerçekimi dönüşü uçur.");
        t("landing_systems", PROPULSION, 1).req("vacuum_engines", "guidance_systems").icon("spacebreaker:landing_legs")
                .unlocks("landing_legs", "parachute", "heat_shield").samples(ORBIT)
                .en("Landing Systems", "Heat shields, parachutes and landing legs: getting down is as hard as getting up.")
                .tr("İniş Sistemleri", "Isı kalkanı, paraşüt ve iniş bacakları: inmek de çıkmak kadar zordur.");
        t("methane_engines", PROPULSION, 2).req("vacuum_engines", "sabatier_process").icon("spacebreaker:methalox_engine").unlocks("methalox_engine", "methalox_vacuum_engine")
                .samples(MARS)
                .en("Methane Engines", "Clean-burning methalox engines, fuelled by propellant made on Mars itself.")
                .tr("Metan Motorları", "Temiz yanan metan-oksijen motorları; yakıtını Mars'ta üretebilirsin.");
        t("hydrogen_engines", PROPULSION, 2).req("vacuum_engines", "cryogenics").icon("spacebreaker:hydrolox_engine").unlocks("hydrolox_engine", "hydrolox_vacuum_engine")
                .samples(ORBIT)
                .en("Hydrogen Engines", "Liquid hydrogen gives the highest chemical specific impulse, if you can keep it cold.")
                .tr("Hidrojen Motorları", "Sıvı hidrojen en yüksek kimyasal özgül itkiyi verir; yeter ki onu soğuk tutabilesin.");
        t("ion_propulsion", PROPULSION, 2).req("hydrogen_engines", "advanced_electronics").icon("minecraft:amethyst_shard")
                .samples(METEORITE)
                .en("Ion Propulsion", "A whisper of thrust for months on end: electric fields accelerate xenon ions to huge speeds.")
                .tr("İyon İtkisi", "Fısıltı kadar zayıf ama aylarca süren itki: elektrik alanı ksenon iyonlarını çok büyük hızlara çıkarır.");
        t("nuclear_thermal_propulsion", PROPULSION, 3).req("hydrogen_engines", "nuclear_fission").icon("minecraft:glowstone_dust")
                .samples(MARS)
                .en("Nuclear Thermal Propulsion", "A reactor heats hydrogen directly: twice the efficiency of any chemical engine.")
                .tr("Nükleer Termal İtki", "Bir reaktör hidrojeni doğrudan ısıtır: her kimyasal motorun iki katı verim.");
        t("fusion_drive", PROPULSION, 3).req("nuclear_thermal_propulsion", "fusion_power").icon("minecraft:nether_star")
                .samples(GANYMEDE)
                .en("Fusion Drive", "Magnetically confined fusion exhaust: the road to the outer planets in weeks, not years.")
                .tr("Füzyon Sürücüsü", "Manyetik alanla hapsedilmiş füzyon egzozu: dış gezegenlere yıllarda değil, haftalarda.");

        // ============================================================ LIFE SUPPORT
        t("pressure_suits", LIFE_SUPPORT, 1).icon("spacebreaker:eva_helmet").unlocks("eva_helmet", "eva_chestplate", "eva_leggings", "eva_boots").samples(EARTH)
                .en("Pressure Suits", "A personal spacecraft: a sealed garment that keeps you at one third of an atmosphere.")
                .tr("Basınçlı Giysiler", "Kişisel bir uzay aracı: seni üçte bir atmosfer basınçta tutan sızdırmaz giysi.");
        t("oxygen_storage", LIFE_SUPPORT, 1).req("pressure_suits", "aluminium_smelting").icon("spacebreaker:oxygen_tank")
                .samples(EARTH)
                .unlocks("oxygen_tank", "large_oxygen_tank", "oxygen_filler")
                .en("Oxygen Storage", "High-pressure aluminium tanks and a filling station: five minutes of air per tank.")
                .tr("Oksijen Depolama", "Yüksek basınçlı alüminyum tüpler ve dolum istasyonu: tüp başına beş dakika hava.");
        t("thermal_regulation", LIFE_SUPPORT, 1).req("pressure_suits").icon("spacebreaker:gold_foil_insulation")
                .samples(EARTH)
                .unlocks("gold_foil_insulation", "orange_foam_insulation")
                .en("Thermal Regulation", "Water-cooled undergarments and reflective layers for +120 °C sun and −150 °C shade.")
                .tr("Isı Düzenleme", "Su soğutmalı iç giysi ve yansıtıcı katmanlar: güneşte +120 °C, gölgede −150 °C.");
        t("airlocks", LIFE_SUPPORT, 1).req("oxygen_storage", "pressurised_modules").icon("spacebreaker:airlock_door")
                .samples(EARTH)
                .unlocks("airlock_door")
                .en("Airlocks", "Two doors and a pump: step outside without letting the whole base breathe out.")
                .tr("Hava Kilitleri", "İki kapı ve bir pompa: bütün üssün havasını boşaltmadan dışarı çık.");
        t("oxygen_distribution", LIFE_SUPPORT, 1).req("airlocks", "electrolysis").icon("spacebreaker:oxygen_distributor")
                .unlocks("oxygen_distributor").samples(MOON)
                .en("Oxygen Distribution", "Fill sealed rooms with breathable air and keep it there.")
                .tr("Oksijen Dağıtımı", "Kapalı odaları solunabilir havayla doldur ve havayı orada tut.");
        t("solar_shielding", LIFE_SUPPORT, 2).req("thermal_regulation", "radio_communication").icon("minecraft:gold_block").samples(MERCURY)
                .en("Solar Shielding", "Gold-film sunshades and white radiators: on Mercury the Sun is seven times brighter and the ground reaches 430 °C.")
                .tr("Güneş Kalkanı", "Altın filmli güneşlikler ve beyaz radyatörler: Merkür'de Güneş yedi kat parlaktır ve zemin 430 °C'ye ulaşır.");
        t("eva_mobility", LIFE_SUPPORT, 2).req("thermal_regulation", "guidance_systems").icon("spacebreaker:maneuvering_unit")
                .samples(ORBIT)
                .unlocks("maneuvering_unit", "magnetic_boots")
                .en("EVA Mobility", "A backpack manoeuvring unit: cold-gas thrusters for moving freely in zero gravity.")
                .tr("EVA Hareketliliği", "Sırt tipi manevra ünitesi: sıfır yerçekiminde serbestçe dolaşmak için soğuk gaz iticileri.");
        t("suit_mk2", LIFE_SUPPORT, 2).req("eva_mobility", "advanced_electronics").icon("minecraft:iron_chestplate")
                .samples(MARS)
                .en("EVA Suit Mk2", "Dust-proof joints, a longer life-support pack and a smarter helmet display.")
                .tr("EVA Giysisi Mk2", "Toz geçirmez eklemler, daha uzun yaşam desteği ve daha akıllı kask göstergesi.");
        t("water_recycling", LIFE_SUPPORT, 2).req("oxygen_distribution").icon("minecraft:water_bucket")
                .samples(MOON)
                .en("Water Recycling", "Recover more than nine litres of every ten: humidity, wash water and the rest.")
                .tr("Su Geri Kazanımı", "Her on litrenin dokuzundan fazlasını geri kazan: nem, yıkama suyu ve geri kalanı.");
        t("radiation_shielding", LIFE_SUPPORT, 3).req("suit_mk2", "nuclear_fission").icon("minecraft:netherite_scrap")
                .samples(IO, EUROPA, GANYMEDE)
                .en("Radiation Shielding", "Layers of water, polyethylene and lead against solar storms and cosmic rays.")
                .tr("Radyasyon Kalkanı", "Güneş fırtınalarına ve kozmik ışınlara karşı su, polietilen ve kurşun katmanları.");
        t("suit_mk3", LIFE_SUPPORT, 3).req("radiation_shielding").icon("minecraft:diamond_chestplate")
                .samples(GANYMEDE)
                .en("EVA Suit Mk3", "A hard-shell suit for the Jovian moons: shielded, heated and built for long walks.")
                .tr("EVA Giysisi Mk3", "Jüpiter'in uyduları için sert kabuklu giysi: kalkanlı, ısıtmalı, uzun yürüyüşlere hazır.");

        // ============================================================ INDUSTRY
        t("aluminium_smelting", INDUSTRY, 1).icon("spacebreaker:aluminum_ingot").unlocks("aluminum_ingot").samples(EARTH)
                .en("Aluminium Smelting", "Refine bauxite into alumina and reduce it to the light metal every rocket is made of.")
                .tr("Alüminyum Ergitme", "Boksiti aluminaya arıt ve her roketin yapıldığı hafif metale indirge.");
        t("metal_pressing", INDUSTRY, 1).req("aluminium_smelting").icon("spacebreaker:press").unlocks("press", "plate_press_die", "wire_press_die", "rod_press_die").samples(EARTH)
                .en("Metal Pressing", "Roll ingots into thin, stiff plates for tanks, panels and hulls.")
                .tr("Metal Presleme", "Külçeleri tanklar, paneller ve gövdeler için ince, sağlam levhalara dönüştür.");
        t("solar_power", INDUSTRY, 1).icon("spacebreaker:solar_panel").unlocks("solar_panel").samples(EARTH)
                .en("Solar Power", "Silicon cells on a sun-tracking mount: free energy whenever the sky is clear.")
                .tr("Güneş Enerjisi", "Güneşi takip eden yuvada silisyum hücreler: gökyüzü açıkken bedava enerji.");
        t("electronics", INDUSTRY, 1).req("metal_pressing").icon("spacebreaker:basic_circuit").unlocks("basic_circuit", "electric_motor")
                .samples(EARTH)
                .en("Electronics", "Circuit boards, motors and sensors: the nervous system of every machine.")
                .tr("Elektronik", "Devre kartları, motorlar ve sensörler: her makinenin sinir sistemi.");
        t("fuel_refining", INDUSTRY, 1).req("electronics").icon("spacebreaker:rp1_bucket").unlocks("pumpjack", "refinery")
                .samples(EARTH)
                .en("Fuel Refining", "Pump crude oil and distil it into RP-1 rocket kerosene and plastics.")
                .tr("Yakıt Rafinasyonu", "Ham petrolü pompala ve RP-1 roket kerosenine ve plastiğe damıt.");
        t("metallurgy", INDUSTRY, 1).req("aluminium_smelting").icon("spacebreaker:steel_ingot")
                .unlocks("arc_furnace", "steel_ingot", "aluminum_lithium_ingot", "nickel_superalloy_ingot").samples(EARTH)
                .en("Metallurgy", "Electric arc furnaces melt metals together: steel for towers, aluminium-lithium for tanks, superalloys for engines.")
                .tr("Metalurji", "Elektrik ark fırınları metalleri birlikte eritir: kuleler için çelik, tanklar için alüminyum-lityum, motorlar için süper alaşım.");
        t("precision_manufacturing", INDUSTRY, 1).req("electronics", "metallurgy").icon("spacebreaker:fabricator")
                .unlocks("fabricator", "solar_cell", "ablative_shield_plate").samples(EARTH)
                .en("Precision Manufacturing", "A powered fabricator for the parts a hammer cannot make: cells, insulation and shield plates.")
                .tr("Hassas İmalat", "Çekicin yapamayacağı parçalar için enerjiyle çalışan üretim tezgâhı: hücreler, yalıtım ve kalkan levhaları.");
        t("electrolysis", INDUSTRY, 1).req("solar_power", "electronics").icon("spacebreaker:electrolyzer").unlocks("electrolyzer")
                .samples(EARTH)
                .en("Electrolysis", "Split water into hydrogen and oxygen with electricity.")
                .tr("Elektroliz", "Elektrikle suyu hidrojen ve oksijene ayır.");
        t("cryogenics", INDUSTRY, 2).req("electrolysis").icon("spacebreaker:cryogenic_tank").unlocks("cryogenic_liquefier", "cryogenic_canister")
                .samples(EARTH)
                .en("Cryogenics", "Liquefy oxygen at −183 °C and hydrogen at −253 °C, and store them without boil-off.")
                .tr("Kriyojenik", "Oksijeni −183 °C'de, hidrojeni −253 °C'de sıvılaştır ve buharlaşmadan sakla.");
        t("sabatier_process", INDUSTRY, 2).req("electrolysis").icon("spacebreaker:sabatier_reactor").unlocks("sabatier_reactor", "atmosphere_processor")
                .samples(MARS)
                .en("Sabatier Process", "Carbon dioxide plus hydrogen over a nickel catalyst gives methane and water.")
                .tr("Sabatier Süreci", "Nikel katalizör üzerinde karbondioksit ve hidrojen: metan ve su.");
        t("isru", INDUSTRY, 2).req("electrolysis", "lunar_surveying").icon("spacebreaker:regolith_heater").unlocks("regolith_heater", "ice_melter", "regolith_excavator")
                .samples(MOON)
                .en("In-Situ Resources", "Live off the land: oxygen from ilmenite, water from polar ice.")
                .tr("Yerinde Kaynak Kullanımı", "Ne gerekiyorsa indiğin yerde üret: ilmenitten oksijen, kutup buzundan su.");
        t("radioisotope_power", INDUSTRY, 2).req("advanced_electronics").icon("spacebreaker:rtg").unlocks("rtg", "rtg_fuel_pellet")
                .samples(EUROPA)
                .en("Radioisotope Power", "Plutonium-238 decay heat turned into power: steady electricity where the Sun is too weak.")
                .tr("Radyoizotop Enerjisi", "Plütonyum-238'in bozunma ısısından elektrik: Güneşin zayıf kaldığı yerde kesintisiz enerji.");
        t("advanced_electronics", INDUSTRY, 2).req("electronics").icon("spacebreaker:advanced_circuit").unlocks("advanced_circuit", "silicon_wafer", "solar_cell")
                .samples(METEORITE)
                .en("Advanced Electronics", "Radiation-hardened processors and silicon wafers grown in clean rooms.")
                .tr("İleri Elektronik", "Radyasyona dayanıklı işlemciler ve temiz odalarda büyütülmüş silisyum plakalar.");
        t("nuclear_fission", INDUSTRY, 3).req("advanced_electronics", "cryogenics").icon("minecraft:glowstone")
                .samples(MOON)
                .en("Nuclear Fission", "Radioisotope generators and compact reactors where sunlight is too weak.")
                .tr("Nükleer Fisyon", "Güneş ışığının yetersiz kaldığı yerler için radyoizotop jeneratörleri ve küçük reaktörler.");
        t("fusion_power", INDUSTRY, 3).req("nuclear_fission").icon("minecraft:beacon")
                .samples(MOON)
                .en("Fusion Power", "Helium-3 and deuterium in a magnetic bottle: the power source of the stars.")
                .tr("Füzyon Enerjisi", "Manyetik şişede helyum-3 ve döteryum: yıldızların enerji kaynağı.");

        // ============================================================ ASTRONOMY
        t("observational_astronomy", ASTRONOMY, 1).icon("minecraft:spyglass").samples(EARTH)
                .en("Observational Astronomy", "Lenses, mirrors and patience: chart the sky before you try to reach it.")
                .tr("Gözlemsel Astronomi", "Mercekler, aynalar ve sabır: ulaşmaya çalışmadan önce gökyüzünün haritasını çıkar.");
        t("orbital_mechanics", ASTRONOMY, 1).req("observational_astronomy", "basic_rocketry").icon("minecraft:clock")
                .samples(SUBORBITAL)
                .en("Orbital Mechanics", "Orbit is falling and missing the ground: about 7.8 km/s sideways at low altitude.")
                .tr("Yörünge Mekaniği", "Yörüngede olmak, düşerken yeri ıskalamaktır: alçak irtifada yana doğru yaklaşık 7,8 km/s.");
        t("radio_communication", ASTRONOMY, 1).req("observational_astronomy", "electronics").icon("minecraft:lightning_rod")
                .samples(EARTH)
                .en("Radio Communication", "Antennas and transmitters to talk to spacecraft beyond the horizon.")
                .tr("Radyo İletişimi", "Ufkun ötesindeki uzay araçlarıyla konuşmak için antenler ve vericiler.");
        t("satellites", ASTRONOMY, 2).req("orbital_mechanics", "radio_communication").icon("minecraft:daylight_detector").unlocks("observation_satellite", "comm_satellite")
                .samples(ORBIT)
                .en("Satellites", "Put eyes and relays in orbit: mapping, weather and communication satellites.")
                .tr("Uydular", "Yörüngeye göz ve aktarıcı yerleştir: haritalama, hava durumu ve iletişim uyduları.");
        t("spectroscopy", ASTRONOMY, 2).req("observational_astronomy", "advanced_electronics").icon("minecraft:prismarine_crystals")
                .samples(METEORITE, ORBITAL_SURVEY)
                .en("Spectroscopy", "Split starlight into a rainbow and read what distant worlds are made of.")
                .tr("Spektroskopi", "Yıldız ışığını gökkuşağına ayır ve uzak dünyaların neyden yapıldığını oku.");
        t("interplanetary_transfers", ASTRONOMY, 2).req("orbital_mechanics").icon("minecraft:map")
                .samples(MOON)
                .en("Interplanetary Transfers", "Hohmann transfers and launch windows: when to leave, and how much Δv it costs.")
                .tr("Gezegenler Arası Transfer", "Hohmann transferleri ve fırlatma pencereleri: ne zaman yola çıkılır, kaç Δv tutar.");
        t("space_probes", ASTRONOMY, 2).req("satellites", "interplanetary_transfers").icon("spacebreaker:space_probe").unlocks("space_probe")
                .samples(MOON, ORBIT, MARS)
                .en("Space Probes", "Uncrewed explorers that fly ahead and send back pictures of worlds no one has seen.")
                .tr("Uzay Sondaları", "Önden giden ve kimsenin görmediği dünyaların görüntülerini gönderen insansız kâşifler.");
        t("gravity_assists", ASTRONOMY, 3).req("interplanetary_transfers", "space_probes").icon("minecraft:heart_of_the_sea")
                .samples(CALLISTO, ORBITAL_SURVEY)
                .en("Gravity Assists", "Steal a little of a planet's orbital speed by flying close behind it.")
                .tr("Kütleçekim Yardımı", "Bir gezegenin hemen arkasından geç ve onun yörünge hızından biraz kap.");
        t("deep_space_network", ASTRONOMY, 3).req("space_probes", "nuclear_fission").icon("minecraft:conduit")
                .samples(CALLISTO, ORBITAL_SURVEY)
                .en("Deep Space Network", "Giant dishes that can hear a probe's faint signal from beyond Saturn.")
                .tr("Derin Uzay Ağı", "Bir sondanın Satürn'ün ötesinden gelen zayıf sinyalini duyabilen dev çanaklar.");

        // ============================================================ CONSTRUCTION
        t("launch_infrastructure", CONSTRUCTION, 1).req("basic_rocketry").icon("spacebreaker:launch_pad")
                .samples(EARTH)
                .unlocks("launch_pad", "flame_trench", "service_tower", "crew_access_arm", "fuel_umbilical", "launch_control_console")
                .en("Launch Infrastructure", "Pad, flame trench and service tower: a safe place to light a rocket.")
                .tr("Fırlatma Altyapısı", "Rampa, alev kanalı ve servis kulesi: bir roketi ateşlemek için güvenli bir yer.");
        t("pressurised_modules", CONSTRUCTION, 1).req("metal_pressing").icon("spacebreaker:station_hull").unlocks("station_hull", "station_window")
                .samples(EARTH)
                .en("Pressurised Modules", "Sealed aluminium hulls, windows and doors that hold an atmosphere.")
                .tr("Basınçlı Modüller", "Atmosferi içinde tutan sızdırmaz alüminyum gövdeler, pencereler ve kapılar.");
        t("regolith_sintering", CONSTRUCTION, 1).req("pressurised_modules").icon("spacebreaker:sintered_regolith_bricks")
                .unlocks("sintered_regolith_bricks").samples(MOON)
                .en("Regolith Sintering", "Bake lunar dust into hard bricks: radiation shielding you do not have to launch.")
                .tr("Regolit Sinterleme", "Ay tozunu sert tuğlalara pişir: fırlatman gerekmeyen radyasyon kalkanı.");
        t("heavy_launch", CONSTRUCTION, 2).req("launch_infrastructure", "staging").icon("spacebreaker:service_tower").unlocks("crew_capsule_large")
                .samples(ORBIT)
                .en("Heavy Launch", "Reinforced pads and bigger towers for super-heavy, three-stage rockets.")
                .tr("Ağır Fırlatma", "Süper ağır, üç kademeli roketler için güçlendirilmiş rampalar ve daha büyük kuleler.");
        t("space_stations", CONSTRUCTION, 2).req("pressurised_modules", "orbital_mechanics").icon("spacebreaker:docking_port").unlocks("station_kit", "docking_port", "habitat_kit", "station_bunk", "fuel_depot", "solar_array", "cargo_module")
                .samples(ORBIT)
                .en("Space Stations", "Dock modules together in orbit: a home, a workshop and a fuel depot above the sky.")
                .tr("Uzay İstasyonları", "Modülleri yörüngede birleştir: gökyüzünün üstünde bir ev, atölye ve yakıt deposu.");
        t("cloud_platforms", CONSTRUCTION, 2).req("space_stations").icon("spacebreaker:composite_panel").unlocks("composite_panel", "aerogel", "aerostat_kit")
                .samples(VENUS)
                .en("Cloud Platforms", "Breathable air is a lifting gas on Venus: float a base 50 km above the furnace.")
                .tr("Bulut Platformları", "Venüs'te solunabilir hava bir kaldırma gazıdır: üssü fırının 50 km üstünde yüzdür.");
        t("orbital_shipyard", CONSTRUCTION, 3).req("space_stations", "heavy_launch", "fusion_power").icon("minecraft:respawn_anchor")
                .samples(ORBIT)
                .en("Orbital Shipyard", "Assemble ships too big to ever lift off a planet.")
                .tr("Yörünge Tersanesi", "Hiçbir gezegenden kalkamayacak kadar büyük gemileri yörüngede birleştir.");

        // ============================================================ EXPLORATION
        t("field_geology", EXPLORATION, 1).icon("minecraft:brush").unlocks("sample_kit").samples(EARTH)
                .en("Field Geology", "Read rocks like a book: collect, label and analyse samples from every world.")
                .tr("Saha Jeolojisi", "Kayaları kitap gibi oku: her dünyadan numune topla, etiketle ve incele.");
        t("lunar_surveying", EXPLORATION, 1).req("field_geology", "landing_systems").icon("spacebreaker:mare_basalt")
                .samples(MOON)
                .en("Lunar Surveying", "Map the maria and highlands; find ilmenite, ice and the safest landing sites.")
                .tr("Ay Araştırması", "Denizleri ve yaylaları haritala; ilmeniti, buzu ve en güvenli iniş yerlerini bul.");
        t("rovers", EXPLORATION, 2).req("lunar_surveying", "electronics").icon("spacebreaker:rover").unlocks("rover")
                .samples(MOON, PROBE_LANDING)
                .en("Rovers", "Electric vehicles with wire-mesh wheels for driving across dusty plains.")
                .tr("Gezici Araçlar", "Tozlu ovalarda sürmek için tel örgü tekerlekli elektrikli araçlar.");
        t("martian_geology", EXPLORATION, 2).req("field_geology", "interplanetary_transfers").icon("minecraft:red_sand")
                .samples(MARS)
                .en("Martian Geology", "Ancient riverbeds, hematite spherules and perchlorate soils tell the story of Mars.")
                .tr("Mars Jeolojisi", "Eski nehir yatakları, hematit kürecikleri ve perkloratlı topraklar Mars'ın hikâyesini anlatır.");
        t("astrobiology", EXPLORATION, 2).req("martian_geology", "spectroscopy").icon("minecraft:sculk_sensor")
                .samples(MARS, VENUS, PROBE_LANDING)
                .en("Astrobiology", "Where could life survive out there, and how would we recognise it?")
                .tr("Astrobiyoloji", "Orada yaşam nerede hayatta kalabilir ve onu nasıl tanırız?");
        t("asteroid_mining", EXPLORATION, 3).req("rovers", "ion_propulsion").icon("minecraft:raw_iron")
                .samples(METEORITE, CERES, VESTA)
                .en("Asteroid Mining", "Nickel-iron and platinum-group metals, waiting in the belt between Mars and Jupiter.")
                .tr("Asteroit Madenciliği", "Mars ile Jüpiter arasındaki kuşakta bekleyen nikel-demir ve platin grubu metaller.");
        t("titan_expedition", EXPLORATION, 3).req("astrobiology", "cryogenics").icon("minecraft:kelp").samples(TITAN)
                .en("Titan Expedition", "Thick nitrogen air and lakes of liquid methane at −180 °C: the only other world with rain, rivers and seas.")
                .tr("Titan Seferi", "Kalın azot havası ve −180 °C'de sıvı metan gölleri: yağmuru, nehirleri ve denizleri olan tek diğer dünya.");
        t("phobos_survey", EXPLORATION, 2).req("martian_geology").icon("minecraft:tuff").samples(PHOBOS)
                .en("Phobos Survey", "A captured asteroid or a chip of Mars? Grooved, dusty and so small you could jump off it.")
                .tr("Phobos Araştırması", "Yakalanmış bir asteroit mi, yoksa Mars'tan kopmuş bir parça mı? Oluklu, tozlu ve üstünden zıplayıp kaçabileceğin kadar küçük.");
        t("icy_moons", EXPLORATION, 3).req("astrobiology", "suit_mk3").icon("minecraft:packed_ice")
                .samples(EUROPA, ENCELADUS, GANYMEDE, CALLISTO)
                .en("Icy Moons", "Drill through kilometres of ice to the hidden oceans of Europa and Enceladus.")
                .tr("Buzlu Uydular", "Europa ve Enceladus'un gizli okyanuslarına ulaşmak için kilometrelerce buzu del.");
    }

    public static void addTranslations(Lang lang) {
        for (Def d : DEFS.values()) {
            lang.add(TechNode.nameKey(d.id()), d.en(), d.tr());
            lang.add(TechNode.descriptionKey(d.id()), d.enDesc(), d.trDesc());
        }
        lang.add(TechNode.categoryKey(PROPULSION), "Propulsion", "İtki");
        lang.add(TechNode.categoryKey(LIFE_SUPPORT), "Life Support", "Yaşam Desteği");
        lang.add(TechNode.categoryKey(INDUSTRY), "Industry", "Sanayi");
        lang.add(TechNode.categoryKey(ASTRONOMY), "Astronomy", "Astronomi");
        lang.add(TechNode.categoryKey(CONSTRUCTION), "Construction", "İnşaat");
        lang.add(TechNode.categoryKey(EXPLORATION), "Exploration", "Keşif");
    }

    /** Colour of a branch (frames, disk label, screen lines). */
    public static int categoryColor(String category) {
        return switch (category) {
            case PROPULSION -> 0xD8673A;   // exhaust orange
            case LIFE_SUPPORT -> 0x4FB3C4; // oxygen cyan
            case INDUSTRY -> 0xC9A13B;     // brass
            case ASTRONOMY -> 0x7C6BD6;    // night violet
            case CONSTRUCTION -> 0x8F9AA6; // aluminium grey
            case EXPLORATION -> 0x6FA84A;  // field green
            default -> 0xA0A0A0;
        };
    }

    private ResearchTree() {
    }
}
