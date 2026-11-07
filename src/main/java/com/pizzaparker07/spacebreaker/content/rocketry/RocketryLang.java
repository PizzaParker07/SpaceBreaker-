package com.pizzaparker07.spacebreaker.content.rocketry;

import com.pizzaparker07.spacebreaker.core.lang.Lang;

/** English and Turkish strings of the rocketry module. */
final class RocketryLang {
    private RocketryLang() {
    }

    static void add(Lang l) {
        l.add("itemGroup.spacebreaker.spaceflight", "Spacebreaker: Spaceflight", "Spacebreaker: Uzay Uçuşu");

        // parts
        l.item(RocketryModule.NOSE_CONE, "Nose Cone", "Burun Konisi");
        l.item(RocketryModule.CREW_CAPSULE, "Crew Capsule", "Mürettebat Kapsülü");
        l.item(RocketryModule.HEAT_SHIELD, "Ablative Heat Shield", "Ablatif Isı Kalkanı");
        l.item(RocketryModule.PARACHUTE, "Parachute Module", "Paraşüt Modülü");
        l.item(RocketryModule.FUEL_TANK_SMALL, "Small Fuel Tank", "Küçük Yakıt Tankı");
        l.item(RocketryModule.FUEL_TANK_MEDIUM, "Medium Fuel Tank", "Orta Yakıt Tankı");
        l.item(RocketryModule.FUEL_TANK_LARGE, "Large Fuel Tank", "Büyük Yakıt Tankı");
        l.item(RocketryModule.FUEL_TANK_SMALL_INSULATED, "Small Fuel Tank (Insulated)", "Küçük Yakıt Tankı (Yalıtımlı)");
        l.item(RocketryModule.FUEL_TANK_MEDIUM_INSULATED, "Medium Fuel Tank (Insulated)", "Orta Yakıt Tankı (Yalıtımlı)");
        l.item(RocketryModule.FUEL_TANK_LARGE_INSULATED, "Large Fuel Tank (Insulated)", "Büyük Yakıt Tankı (Yalıtımlı)");
        l.item(RocketryModule.MULTI_LAYER_INSULATION, "Multi-Layer Insulation", "Çok Katmanlı Yalıtım");
        l.add("tooltip.spacebreaker.part.insulated", "Multi-layer insulation: %s%% less boil-off", "Çok katmanlı yalıtım: %%%s daha az buharlaşma");
        l.item(RocketryModule.KEROSENE_ENGINE, "Kerosene Engine (Sea Level)", "Kerosen Motoru (Deniz Seviyesi)");
        l.item(RocketryModule.VACUUM_ENGINE, "Kerosene Engine (Vacuum)", "Kerosen Motoru (Vakum)");
        l.item(RocketryModule.METHALOX_ENGINE, "Methalox Engine (Sea Level)", "Metaloks Motoru (Deniz Seviyesi)");
        l.item(RocketryModule.METHALOX_VACUUM_ENGINE, "Methalox Engine (Vacuum)", "Metaloks Motoru (Vakum)");
        l.add("propellant.spacebreaker.kerolox", "RP-1/LOX", "RP-1/LOX");
        l.add("propellant.spacebreaker.methalox", "Methane/LOX", "Metan/LOX");
        l.add("propellant.spacebreaker.hydrolox", "Hydrogen/LOX", "Hidrojen/LOX");
        l.add("propellant.spacebreaker.nuclear", "Hydrogen (nuclear thermal)", "Hidrojen (nükleer termal)");
        l.item(RocketryModule.NUCLEAR_THERMAL_ENGINE, "Nuclear Thermal Engine (Vacuum)", "Nükleer Termal Motor (Vakum)");
        l.add("gui.spacebreaker.assembler.nuclear_tag", "☢ Nuclear", "☢ Nükleer");
        l.add("gui.spacebreaker.assembler.nuclear", "☢ Nuclear stage: radioactive while it burns. Fire it only well away from the pad and from people",
                "☢ Nükleer kademe: yanarken radyasyon yayar. Yalnız rampadan ve insanlardan uzakta ateşle");
        l.add("gui.spacebreaker.assembler.nuclear_first", "☢ Nuclear first stage: too weak to lift off, and it would irradiate the pad. Put it on an upper stage",
                "☢ Nükleer ilk kademe: kalkmaya gücü yetmez, rampayı da ışınlar. Üst kademeye koy");
        l.add("tooltip.spacebreaker.part.boil_off", "Cryogenic: %s%% of its propellant boils off per day on long trips and in orbit",
                "Kriyojenik: uzun yolculukta ve yörüngede yakıtının günde %%%s'i buharlaşır");
        l.add("tooltip.spacebreaker.part.hydrogen_note", "Real liquid hydrogen is very bulky; in the game a tank's capacity counts as mass, whatever it holds",
                "Gerçekte sıvı hidrojen çok hacimlidir; oyunda tank kapasitesi, içinde ne olursa olsun kütle olarak sayılır");
        l.add("tooltip.spacebreaker.part.propellant_nuclear", "In a nuclear thermal stage: LH2 %s mB, no LOX", "Nükleer termal kademede: LH2 %s mB, LOX yok");
        l.add("tooltip.spacebreaker.part.nuclear", "Radioactive while it burns: keep clear of the engine. Safe when cold; the crew rides behind a shadow shield",
                "Yanarken radyasyon yayar: motordan uzak dur. Soğukken güvenlidir; mürettebat gölge kalkanının arkasında kalır");
        l.item(RocketryModule.CREW_CAPSULE_LARGE, "Crew Capsule (3 seats)", "Mürettebat Kapsülü (3 kişilik)");
        l.block(RocketryModule.STATION_HULL, "Station Hull", "İstasyon Gövdesi");
        l.block(RocketryModule.STATION_WINDOW, "Station Window", "İstasyon Penceresi");
        l.block(RocketryModule.DOCKING_PORT, "Docking Port", "Yanaşma Portu");
        l.item(RocketryModule.STATION_KIT, "Station Construction Kit", "İstasyon Kurulum Kiti");
        l.item(RocketryModule.HABITAT_KIT, "Habitat Module Kit", "Yaşam Modülü Kiti");
        l.block(RocketryModule.STATION_BUNK, "Station Bunk", "İstasyon Ranzası");
        l.block(RocketryModule.FUEL_DEPOT, "Fuel Depot", "Yakıt Deposu");
        l.block(RocketryModule.LANDING_BEACON, "Landing Beacon", "İniş İşareti");
        l.block(RocketryModule.WARNING_LIGHT, "Warning Light", "Uyarı Işığı");
        com.pizzaparker07.spacebreaker.content.mars.BlockKit.familyNames(l, com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.ALUMINUM_PANEL,
                "Aluminium Panel", "Alüminyum Panel");
        com.pizzaparker07.spacebreaker.content.mars.BlockKit.familyNames(l, com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.TITANIUM_DECK,
                "Titanium Deck Plate", "Titanyum Levha");
        l.block(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PRESSURE_GLASS, "Pressure Glass", "Basınçlı Cam");
        l.block(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PORTHOLE, "Porthole", "Lombar");
        l.block(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE, "Grate Floor", "Izgara Zemin");
        l.block(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.SLIDING_DOOR, "Sliding Door", "Kayar Kapı");
        l.add("tooltip.spacebreaker.sliding_door", "Two facing each other up to 4 blocks apart make an airlock: one opens only once the other has shut",
                "Karşılıklı en fazla 4 blok arayla iki kapı hava kilidi olur: biri ancak diğeri kapandıktan sonra açılır");
        com.pizzaparker07.spacebreaker.content.mars.BlockKit.familyNames(l, com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.HAZARD_STRIPE,
                "Hazard Stripe", "Uyarı Şeridi");
        l.block(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL, "LED Panel Light", "LED Panel Lamba");
        l.block(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.FLOODLIGHT, "Floodlight", "Projektör");
        l.block(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.CABLE_DUCT, "Cable Duct Cover", "Kablo Kanalı Kapağı");
        l.block(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.PIPE_DUCT, "Pipe Duct Cover", "Boru Kanalı Kapağı");
        l.block(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.STEEL_LADDER, "Steel Ladder", "Çelik Merdiven");
        l.block(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.RAILING, "Safety Railing", "Korkuluk");
        l.block(com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.GRATE_PLATFORM, "Grate Platform", "Izgara Platform");
        l.add("message.spacebreaker.warn.dust_storm_launch", "Dust storm over the pad: poor visibility and gusts. Launch at your own risk, or wait it out",
                "Rampanın üstünde toz fırtınası: görüş kötü, rüzgâr sert. Kalkış senin sorumluluğunda; istersen fırtınanın geçmesini bekle");
        l.add("message.spacebreaker.landing_beacon.guided", "Landing beacon in range: coming down on the base", "İniş işareti alındı: üsse iniliyor");
        l.add("tooltip.spacebreaker.habitat_kit", "Use in empty space: unfolds a sealed 7×5×7 room with an airlock, windows, docking port, bunk, a solar array and a battery bank",
                "Boşlukta kullan: hava kilidi, pencereler, yanaşma portu, ranza, güneş paneli dizisi ve batarya bankasıyla kapalı 7×5×7 bir oda açar");
        l.add("tooltip.spacebreaker.habitat_kit.air", "Put an oxygen tank in the distributor to fill it with air",
                "Havayla doldurmak için dağıtıcıya bir oksijen tüpü tak");
        l.add("message.spacebreaker.station_bunk.set", "Respawn point set", "Yeniden doğma noktası ayarlandı");
        l.add("message.spacebreaker.fuel_depot.status", "Fuel depot: RP-1 %s mB, LOX %s mB, methane %s mB, hydrogen %s mB · %s FE · %s docking ports",
                "Yakıt deposu: RP-1 %s mB, LOX %s mB, metan %s mB, hidrojen %s mB · %s FE · %s yanaşma portu");
        l.add("tooltip.spacebreaker.station_kit", "Use in empty space: unfolds a 5×5 hull floor in front of you", "Boşlukta kullan: önüne 5×5 gövde zemini açar");
        l.add("message.spacebreaker.station_kit.blocked", "Something is in the way", "Önünde bir şey var");
        l.add("message.spacebreaker.docking.approach", "Station in sight: approaching the docking port…", "İstasyon göründü: yanaşma portuna yaklaşılıyor…");
        l.add("message.spacebreaker.docked", "Docked: you can step out into the station", "Yanaşıldı: istasyona çıkabilirsin");
        l.item(RocketryModule.COMM_SATELLITE, "Communications Relay Satellite", "İletişim Rölesi Uydusu");
        l.item(RocketryModule.CARGO_MODULE, "Cargo Module", "Kargo Modülü");
        l.add("container.spacebreaker.cargo_hold", "Cargo Hold", "Kargo Ambarı");
        l.add("message.spacebreaker.cargo.delivered", "Cargo delivered at %s: %s items unloaded", "Kargo %s konumuna teslim edildi: %s eşya boşaltıldı");
        l.add("message.spacebreaker.cargo.waiting", "Cargo rocket at %s: %s items wait in its hold (no chests by the port or beacon)",
                "Kargo roketi %s konumunda: %s eşya ambarda bekliyor (port ya da işaretin yanında sandık yok)");
        l.item(RocketryModule.STATION_CORE_MODULE, "Station Core Module", "İstasyon Çekirdek Modülü");
        l.item(RocketryModule.DOCKING_MODULE, "Docking Module", "Kenetlenme Modülü");
        l.item(RocketryModule.HABITATION_MODULE, "Habitation Module", "Yaşam Modülü");
        l.item(RocketryModule.AIRLOCK_MODULE, "Airlock Module", "Hava Kilidi Modülü");
        l.item(RocketryModule.SOLAR_WINGS, "Solar Wings", "Güneş Kanatları");
        l.add("tooltip.spacebreaker.station_module.habitation", "Living quarters: three sleeping pods, two supply lockers, a window band and a mouth at the far end",
                "Yaşam alanı: üç uyku kabini, iki malzeme dolabı, pencere bandı ve uçta bir ağız");
        l.add("tooltip.spacebreaker.station_module.airlock", "A small chamber with a door into the station and a door to space: step out without losing the station's air",
                "İstasyona ve uzaya açılan birer kapılı küçük oda: istasyonun havasını kaybetmeden dışarı çık");
        l.add("tooltip.spacebreaker.station_module.solar_wings", "Outside the hull: a rotary joint and a lattice boom carrying four solar arrays, a battery bank in the mouth powering the station (FE)",
                "Gövdenin dışında: döner eklem ve kafes kol üstünde dört güneş paneli dizisi, ağızda istasyona enerji (FE) veren batarya bankası");
        l.item(RocketryModule.SHIPYARD_MODULE, "Shipyard Module", "Tersane Modülü");
        l.add("tooltip.spacebreaker.station_module.shipyard", "An open 15 x 15 x 31 frame outside the hull: a berth for one spacecraft (up to 13 x 13 x 29), its docking port in the mouth, a fuel depot and floodlights",
                "Gövdenin dışında açık 15 x 15 x 31 çerçeve: bir uzay aracı için yuva (en fazla 13 x 13 x 29), ağızda kenetlenme portu, yakıt deposu ve projektörler");
        l.block(RocketryModule.STATION_HATCH, "Station Hatch", "İstasyon Kapağı");
        l.block(RocketryModule.STATION_CORE, "Station Core Console", "İstasyon Çekirdek Konsolu");
        l.add("container.spacebreaker.station_core", "Station core: modules waiting", "İstasyon çekirdeği: bekleyen modüller");
        l.add("tooltip.spacebreaker.part.station_module", "Station module: goes in the capsule slot; docked at a station it waits in the core console",
                "İstasyon modülü: kapsül yuvasına takılır; istasyona yanaşınca çekirdek konsolunda bekler");
        l.add("tooltip.spacebreaker.station_module.core", "The heart of a station: console, air, lights, a docking port on the roof and four mouths",
                "İstasyonun kalbi: konsol, hava, ışık, çatıda kenetlenme portu ve dört bağlantı ağzı");
        l.add("tooltip.spacebreaker.station_module.docking", "A short tunnel with a docking port at its end",
                "Ucunda kenetlenme portu olan kısa bir tünel");
        l.add("tooltip.spacebreaker.station_module.use", "Use on a mouth's hatch to attach it; a core used in open space in orbit starts a new station",
                "Bir ağzın kapağına kullanınca oraya bağlanır; yörüngede boşlukta kullanılan çekirdek yeni bir istasyon kurar");
        l.add("message.spacebreaker.station_module.blocked", "Something is in the way of the module", "Modülün yolunda bir şey var");
        l.add("message.spacebreaker.station_module.orbit_only", "A station core can only be unfolded in orbit", "İstasyon çekirdeği yalnızca yörüngede açılabilir");
        l.add("message.spacebreaker.station_module.core_built", "The station core is up: a new station in orbit", "İstasyon çekirdeği kuruldu: yörüngede yeni bir istasyon");
        l.add("message.spacebreaker.station_module.delivered", "%s delivered: it waits in the station core console", "%s teslim edildi: istasyon çekirdek konsolunda bekliyor");
        l.add("message.spacebreaker.station_module.no_console", "%s delivered, but no core console in reach: it floats by the port", "%s teslim edildi ama yakında çekirdek konsolu yok: portun yanında süzülüyor");
        l.add("message.spacebreaker.cargo.no_station", "Cargo rocket in %s: no station to dock at, waiting in orbit", "Kargo roketi %s içinde: yanaşacak istasyon yok, yörüngede bekliyor");
        l.item(RocketryModule.OBSERVATION_SATELLITE, "Observation Satellite", "Gözlem Uydusu");
        l.item(RocketryModule.SPACE_PROBE, "Lander Probe", "İniş Sondası");
        l.add("tooltip.spacebreaker.part.satellite", "Uncrewed satellite: goes in the capsule slot; launch it from the console", "İnsansız uydu: kapsül yuvasına takılır; konsoldan fırlatılır");
        l.add("message.spacebreaker.satellite.deployed", "%s released in %s", "%s, %s içine bırakıldı");
        l.add("message.spacebreaker.probe.landed", "The probe landed on %s: samples and data are on their way to you", "Sonda %s yüzeyine indi: numuneler ve veriler sana gönderiliyor");
        l.add("gui.spacebreaker.console.mission_target", "Target...", "Hedef...");
        l.add("gui.spacebreaker.console.one_way", "One-way trip (uncrewed)", "Tek yön (insansız)");
        l.add("gui.spacebreaker.console.route", "Route → %s · %s/27", "Sefer → %s · %s/27");
        l.add("gui.spacebreaker.console.route.off", "Route off", "Sefer kapalı");
        l.add("gui.spacebreaker.console.route.loading", "Route: loading", "Sefer: yükleniyor");
        l.add("gui.spacebreaker.console.route.wait_cargo", "Route: waiting for cargo", "Sefer: yük bekleniyor");
        l.add("gui.spacebreaker.console.route.wait_fuel", "Route: waiting for fuel", "Sefer: yakıt bekleniyor");
        l.add("gui.spacebreaker.console.route.flying", "Route: under way", "Sefer: yolda");
        l.add("gui.spacebreaker.console.mission", "Uncrewed mission: %s", "İnsansız görev: %s");
        l.add("gui.spacebreaker.console.mission_none", "Uncrewed: release in Earth orbit", "İnsansız: Dünya yörüngesinde bırak");
        l.add("gui.spacebreaker.star_map.planning", "Mission Planning", "Görev Planlama");
        l.add("gui.spacebreaker.star_map.set_target", "Set as mission target", "Görev hedefi yap");
        l.add("gui.spacebreaker.star_map.no_signal", "No signal: no communications relay around this world, mission control cannot be heard here.", "Sinyal yok: bu dünyanın çevresinde iletişim rölesi yok, görev kontrolü burada duyulmaz.");
        l.add("gui.spacebreaker.star_map.mapped", "Mapped from orbit ✔", "Yörüngeden haritalandı ✔");
        l.add("gui.spacebreaker.star_map.mapped_fuel", "Mapped from orbit ✔ · propellant can be made here", "Yörüngeden haritalandı ✔ · burada yakıt üretilebilir");
        l.add("hud.spacebreaker.no_signal", "No signal (no relay satellite)", "Sinyal yok (röle uydusu yok)");
        l.add("tooltip.spacebreaker.part.cargo", "Uncrewed cargo: goes in the capsule slot; docks at the station or lands on the beacon and unloads into the nearest chests",
                "İnsansız kargo: kapsül yuvasına takılır; istasyona yanaşır ya da iniş işaretine iner ve en yakın sandıklara boşaltır");
        l.add("tooltip.spacebreaker.part.cargo.route", "Between a station and a base it shuttles by itself: chests take deliveries, barrels hold what goes out",
                "İstasyon ile üs arasında kendi kendine gidip gelir: sandıklar gelen yükü alır, variller giden yükü tutar");
        l.add("message.spacebreaker.cargo.route_on", "Automatic cargo route on", "Otomatik kargo seferi açık");
        l.add("message.spacebreaker.cargo.route_off", "Automatic cargo route off", "Otomatik kargo seferi kapalı");
        l.add("message.spacebreaker.cargo.route_fuel", "Cargo route %s → %s: waiting for fuel", "Kargo seferi %s → %s: yakıt bekleniyor");
        l.add("tooltip.spacebreaker.part.probe", "Uncrewed probe: goes in the capsule slot; launch it from the console", "İnsansız sonda: kapsül yuvasına takılır; konsoldan fırlatılır");
        l.item(RocketryModule.HYDROLOX_ENGINE, "Hydrolox Engine (Sea Level)", "Hidroloks Motoru (Deniz Seviyesi)");
        l.item(RocketryModule.HYDROLOX_VACUUM_ENGINE, "Hydrolox Engine (Vacuum)", "Hidroloks Motoru (Vakum)");
        l.add("tooltip.spacebreaker.part.propellant_hydrolox", "In a hydrolox stage: LH2 %s mB, LOX %s mB", "Hidroloks kademesinde: LH2 %s mB, LOX %s mB");
        l.add("tooltip.spacebreaker.part.burns", "Burns %s", "Yakıt: %s");
        l.add("tooltip.spacebreaker.part.propellant_methalox", "In a methalox stage: CH4 %s mB, LOX %s mB", "Metaloks kademesinde: CH4 %s mB, LOX %s mB");
        l.item(RocketryModule.DECOUPLER, "Stage Decoupler", "Kademe Ayırıcı");
        l.item(RocketryModule.INTERSTAGE, "Interstage", "Ara Kademe");
        l.item(RocketryModule.FINS, "Fins", "Kanatçıklar");
        l.item(RocketryModule.GRID_FINS, "Grid Fins", "Izgara Kanatçıklar");
        l.item(RocketryModule.LANDING_LEGS, "Landing Legs", "İniş Bacakları");
        l.item(RocketryModule.RCS_THRUSTERS, "RCS Thrusters", "Manevra İticileri");
        l.item(RocketryModule.ROCKET, "Rocket", "Roket");

        l.add("tooltip.spacebreaker.part.requires", "Requires research: %s", "Araştırma gerekli: %s");
        l.add("tooltip.spacebreaker.part.mass", "Dry mass: %s", "Kuru kütle: %s");
        l.add("tooltip.spacebreaker.part.propellant", "Propellant: %s (RP-1 %s mB, LOX %s mB)", "Yakıt: %s (RP-1 %s mB, LOX %s mB)");
        l.add("tooltip.spacebreaker.part.thrust", "Thrust: %s kN", "İtki: %s kN");
        l.add("tooltip.spacebreaker.part.isp", "Isp: %s s sea level / %s s vacuum", "Özgül itki: %s s deniz seviyesi / %s s vakum");
        l.add("tooltip.spacebreaker.part.heat_shield.1", "Heat shield: rated for reentry from low orbit", "Isı kalkanı: alçak yörüngeden dönüş için");
        l.add("tooltip.spacebreaker.part.heat_shield.2", "Heat shield: rated for reentry from the Moon", "Isı kalkanı: Ay'dan dönüş için");
        l.add("tooltip.spacebreaker.part.seats", "Crew seats: %s", "Mürettebat koltuğu: %s");
        l.add("tooltip.spacebreaker.part.nose_cone", "Payload top: smooths the airflow", "Faydalı yük tepesi: hava akışını düzeltir");
        l.add("tooltip.spacebreaker.part.capsule", "Crew section: sits on the heat shield", "Mürettebat bölümü: ısı kalkanının üstüne oturur");
        l.add("tooltip.spacebreaker.part.heat_shield", "Mount under the capsule", "Kapsülün altına takılır");
        l.add("tooltip.spacebreaker.part.parachute", "Mount on top of the capsule for a soft landing", "Yumuşak iniş için kapsülün üstüne takılır");
        l.add("tooltip.spacebreaker.part.tank", "Holds RP-1 kerosene and liquid oxygen", "RP-1 kerosen ve sıvı oksijen taşır");
        l.add("tooltip.spacebreaker.part.engine", "One per stage, at the bottom", "Her kademeye bir tane, en alta");
        l.add("tooltip.spacebreaker.part.decoupler", "Drops the stage below when it runs dry", "Yakıtı biten alt kademeyi bırakır");
        l.add("tooltip.spacebreaker.part.interstage", "Drops the stage below; shields the engine above", "Alt kademeyi bırakır; üstteki motoru korur");
        l.add("tooltip.spacebreaker.part.fins", "Radial: less steering loss on ascent", "Yanal: kalkışta yönlendirme kaybını azaltır");
        l.add("tooltip.spacebreaker.part.grid_fins", "Radial: steering for the first stage", "Yanal: ilk kademe için yönlendirme");
        l.add("tooltip.spacebreaker.part.landing_legs", "Radial: needed to land on the Moon", "Yanal: Ay'a inmek için gerekli");
        l.add("tooltip.spacebreaker.part.rcs", "Radial: fine control for landings", "Yanal: inişlerde hassas kontrol");

        l.add("tooltip.spacebreaker.rocket.stages", "Stages: %s", "Kademe: %s");
        l.add("tooltip.spacebreaker.rocket.delta_v", "Δv: %s", "Δv: %s");
        l.add("tooltip.spacebreaker.rocket.mass", "Launch mass: %s", "Kalkış kütlesi: %s");
        l.add("tooltip.spacebreaker.rocket.height", "Height: %s blocks", "Yükseklik: %s blok");
        l.add("tooltip.spacebreaker.rocket.hint", "Use on the flame trench of a launch pad", "Fırlatma rampasının alev kanalına kullan");


        // blocks, items, entities
        l.block(RocketryModule.ROCKET_ASSEMBLER, "Rocket Assembler", "Roket Montaj Tezgâhı");
        l.block(RocketryModule.LAUNCH_PAD, "Launch Pad Plating", "Fırlatma Rampası Kaplaması");
        l.block(RocketryModule.FLAME_TRENCH, "Flame Trench Grate", "Alev Kanalı Izgarası");
        l.block(RocketryModule.SERVICE_TOWER, "Service Tower", "Servis Kulesi");
        l.block(RocketryModule.CREW_ARM, "Crew Access Arm", "Mürettebat Erişim Kolu");
        l.block(RocketryModule.FUEL_UMBILICAL, "Fuel Umbilical", "Yakıt Hattı");
        l.block(RocketryModule.LAUNCH_CONSOLE, "Launch Control Console", "Fırlatma Kontrol Konsolu");
        l.item(RocketryModule.PROPELLANT_CARTRIDGE, "Creative Propellant Cartridge", "Yaratıcı Yakıt Kartuşu");
        l.item(RocketryModule.EMERGENCY_BEACON, "Emergency Return Beacon", "Acil Dönüş İşareti");
        l.add("entity.spacebreaker.rocket", "Rocket", "Roket");
        l.add("entity.spacebreaker.spent_stage", "Spent Stage", "Ayrılmış Kademe");
        l.add("entity.spacebreaker.satellite", "Satellite", "Uydu");
        l.add("dimension.spacebreaker.earth_orbit", "Earth Orbit", "Dünya Yörüngesi");
        l.add("dimension.spacebreaker.moon_orbit", "Moon Orbit", "Ay Yörüngesi");
        l.add("dimension.spacebreaker.orbit", "Orbit", "Yörünge");
        l.add("container.spacebreaker.rocket_assembler", "Rocket Assembler", "Roket Montaj Tezgâhı");
        l.add("container.spacebreaker.launch_console", "Launch Control", "Fırlatma Kontrolü");
        l.add("key.categories.spacebreaker.spaceflight", "Spacebreaker: Spaceflight", "Spacebreaker: Uzay Uçuşu");
        l.add("key.spacebreaker.launch", "Launch / abort countdown", "Fırlat / geri sayımı durdur");
        l.add("key.spacebreaker.star_map", "Star map", "Yıldız haritası");
        l.add("key.spacebreaker.flight_details", "Flight details", "Uçuş ayrıntıları");
        l.add("tooltip.spacebreaker.propellant_cartridge", "Use on a rocket to fill every tank", "Tüm tankları doldurmak için rokete kullan");
        l.add("tooltip.spacebreaker.emergency_beacon", "Away from Earth: calls a capsule that brings you home", "Dünya dışında: seni eve getiren bir kapsül çağırır");

        // messages
        l.add("message.spacebreaker.rocket.loaded", "%s loaded into the rocket", "%s rokete yüklendi");
        l.add("message.spacebreaker.rocket.tank_full", "No room left for %s", "%s için yer kalmadı");
        l.add("message.spacebreaker.rocket.fuelled", "Tanks full: RP-1 and liquid oxygen loaded", "Tanklar dolu: RP-1 ve sıvı oksijen yüklendi");
        l.add("message.spacebreaker.rocket.no_seat", "This rocket has no crew capsule", "Bu roketin mürettebat kapsülü yok");
        l.add("message.spacebreaker.rocket.pick_up_hint", "Sneak and hit to take the rocket down", "Roketi sökmek için eğilip vur");
        l.add("message.spacebreaker.rocket.needs_pad", "Stand the rocket on the flame trench of a complete launch pad", "Roketi tamamlanmış bir rampanın alev kanalına koy");
        l.add("message.spacebreaker.rocket.pad_occupied", "There is already a rocket on this pad", "Bu rampada zaten bir roket var");
        l.add("message.spacebreaker.launch.busy", "The rocket is not ready", "Roket hazır değil");
        l.add("message.spacebreaker.launch.no_stage", "The rocket has no working stage", "Roketin çalışan bir kademesi yok");
        l.add("message.spacebreaker.launch.no_crew", "Nobody is aboard", "Roket boş, mürettebat yok");
        l.add("message.spacebreaker.launch.nowhere", "Launch from a surface; use the star map in orbit", "Yüzeyden fırlat; yörüngede yıldız haritasını kullan");
        l.add("message.spacebreaker.launch.no_pad", "Launch scrubbed: the rocket is not on a complete launch pad", "Fırlatma iptal: roket tamamlanmış bir rampada değil");
        l.add("message.spacebreaker.launch.twr", "Launch scrubbed: thrust-to-weight %s is below 1", "Fırlatma iptal: itki/ağırlık oranı %s, 1'in altında");
        l.add("message.spacebreaker.launch.delta_v", "Launch scrubbed: Δv %s m/s, orbit needs %s m/s (fuel up?)", "Fırlatma iptal: Δv %s m/s, yörünge için %s m/s gerekli (yakıt?)");
        l.add("message.spacebreaker.launch.aborted", "Countdown aborted", "Geri sayım durduruldu");
        l.add("message.spacebreaker.warn.no_return", "Warning: not enough propellant to come back home from there", "Uyarı: oradan eve dönmeye yetecek yakıt yok");
        l.add("message.spacebreaker.warn.heat_shield", "Warning: the heat shield is not rated for this reentry", "Uyarı: ısı kalkanı bu dönüş için yeterli değil");
        l.add("message.spacebreaker.warn.no_legs", "Warning: no landing legs, expect a hard landing", "Uyarı: iniş bacağı yok, sert iniş olacak");
        l.add("message.spacebreaker.warn.stranded", "Not enough propellant to get home: use an Emergency Return Beacon", "Eve dönecek yakıt yok: Acil Dönüş İşareti kullan");
        l.add("message.spacebreaker.travel.no_route", "No route from here", "Buradan bir rota yok");
        l.add("message.spacebreaker.travel.unavailable", "%s cannot be reached yet in this world", "%s bu dünyada henüz erişilebilir değil");
        l.add("message.spacebreaker.travel.delta_v", "Not enough Δv: %s m/s available, %s m/s needed", "Yetersiz Δv: %s m/s var, %s m/s gerekli");
        l.add("message.spacebreaker.ascent.failed", "Ascent failed: out of propellant. Capsule separated!", "Kalkış başarısız: yakıt bitti. Kapsül ayrıldı!");
        l.add("message.spacebreaker.orbit.arrived", "In %s. Open the star map to choose where to go", "%s içindesin. Nereye gideceğini seçmek için yıldız haritasını aç");
        l.add("message.spacebreaker.landing.hard", "Hard landing!", "Sert iniş!");
        l.add("message.spacebreaker.landing.ok", "Touchdown on %s", "%s yüzeyine iniş yapıldı");
        l.add("message.spacebreaker.beacon.not_needed", "The beacon only works away from Earth", "İşaret yalnızca Dünya dışında çalışır");
        l.add("message.spacebreaker.beacon.activated", "Emergency return: hold on, you are coming home", "Acil dönüş: sıkı tutun, eve dönüyorsun");
        l.add("message.spacebreaker.console.no_rocket", "No rocket on the pad", "Rampada roket yok");
        l.add("message.spacebreaker.pad.no_trench", "No flame trench here: lay a 3x3 of grates in a ring of pad plating", "Burada alev kanalı yok: rampa kaplaması halkasının içine 3x3 ızgara koy");
        l.add("message.spacebreaker.pad.title", "Launch pad check", "Fırlatma rampası kontrolü");
        l.add("message.spacebreaker.pad.trench", "Flame trench grates: %s / 9", "Alev kanalı ızgaraları: %s / 9");
        l.add("message.spacebreaker.pad.plating", "Pad plating around it: %s / 16", "Çevresindeki rampa kaplaması: %s / 16");
        l.add("message.spacebreaker.pad.tower", "Service tower: %s blocks high (%s+)", "Servis kulesi: %s blok yüksek (%s+)");
        l.add("message.spacebreaker.pad.arm", "Crew access arm on the tower", "Kulede mürettebat erişim kolu");
        l.add("message.spacebreaker.pad.umbilical", "Fuel umbilical on the tower", "Kulede yakıt hattı");
        l.add("message.spacebreaker.pad.ready", "Pad ready for launch", "Rampa fırlatmaya hazır");
        l.add("message.spacebreaker.pad.incomplete", "Pad incomplete: missing blocks are marked with flames", "Rampa eksik: eksik bloklar alevle işaretlendi");
        l.add("message.spacebreaker.assembler.no_stage", "Add at least one stage: an engine and a tank", "En az bir kademe ekle: bir motor ve bir tank");
        l.add("message.spacebreaker.assembler.no_engine", "Every stage needs an engine", "Her kademenin bir motoru olmalı");
        l.add("message.spacebreaker.assembler.no_tank", "Every stage needs at least one fuel tank", "Her kademede en az bir yakıt tankı olmalı");
        l.add("message.spacebreaker.assembler.no_separator", "Put a decoupler or interstage between stages", "Kademeler arasına ayırıcı ya da ara kademe koy");
        l.add("message.spacebreaker.assembler.stage_gap", "Fill the stages from the bottom (stage 1) up", "Kademeleri alttan (1. kademe) başlayarak doldur");
        l.add("message.spacebreaker.assembler.no_payload", "Add a crew capsule or a nose cone on top", "Tepeye bir mürettebat kapsülü ya da burun konisi ekle");
        l.add("message.spacebreaker.assembler.shield_without_capsule", "A heat shield needs a capsule above it", "Isı kalkanının üstünde bir kapsül olmalı");
        l.add("message.spacebreaker.assembler.wrong_part", "A part is in the wrong slot", "Bir parça yanlış yuvada");

        // HUD
        l.add("hud.spacebreaker.coasting", "Coasting to %s", "%s hedefine süzülüyor");
        l.add("hud.spacebreaker.brief", "→ %s · %s", "→ %s · %s");
        l.add("hud.spacebreaker.days", "%s days", "%s gün");
        l.add("hud.spacebreaker.details_hint", "%s: details", "%s: ayrıntılar");
        l.add("hud.spacebreaker.ignition", "Ignition sequence start", "Ateşleme dizisi başladı");
        l.add("hud.spacebreaker.liftoff", "Liftoff!", "Kalkış!");
        l.add("hud.spacebreaker.state.idle", "Ready", "Hazır");
        l.add("hud.spacebreaker.state.countdown", "Countdown", "Geri sayım");
        l.add("hud.spacebreaker.state.ascent", "Ascent", "Tırmanış");
        l.add("hud.spacebreaker.state.orbit", "In orbit", "Yörüngede");
        l.add("hud.spacebreaker.state.burn", "Transfer burn", "Transfer ateşlemesi");
        l.add("hud.spacebreaker.state.transfer", "Coasting", "Süzülüş");
        l.add("hud.spacebreaker.state.descent", "Powered descent", "Motorlu iniş");
        l.add("hud.spacebreaker.state.reentry", "Reentry", "Atmosfere giriş");
        l.add("hud.spacebreaker.state.parachute", "Under parachutes", "Paraşütle iniş");
        l.add("hud.spacebreaker.location", "Location: %s", "Konum: %s");
        l.add("hud.spacebreaker.altitude", "Altitude: %s", "İrtifa: %s");
        l.add("hud.spacebreaker.speed", "Speed: %s", "Hız: %s");
        l.add("hud.spacebreaker.stage", "Stage %s / %s", "Kademe %s / %s");
        l.add("hud.spacebreaker.capsule", "Capsule only", "Yalnızca kapsül");
        l.add("hud.spacebreaker.delta_v", "Fuel left (Δv): %s", "Kalan yakıt (Δv): %s");
        l.add("hud.spacebreaker.fuel", "Fuel", "Yakıt");
        l.add("hud.spacebreaker.hint.launch", "Press %s to start the countdown", "Geri sayımı başlatmak için %s");
        l.add("hud.spacebreaker.hint.abort", "Press %s to abort", "Durdurmak için %s");
        l.add("hud.spacebreaker.hint.map", "Press %s for the star map", "Yıldız haritası için %s");

        // screens
        l.add("gui.spacebreaker.assembler.assemble", "Assemble", "Birleştir");
        l.add("gui.spacebreaker.assembler.empty", "Put an engine and fuel tanks in the stage rows; the capsule goes on top.", "Kademe satırlarına motor ve yakıt tankı koy; kapsül en üste gelir.");
        l.add("gui.spacebreaker.assembler.dv", "Δv %s", "Δv %s");
        l.add("gui.spacebreaker.assembler.twr", "TWR %s", "İ/A %s");
        l.add("gui.spacebreaker.assembler.mass", "Mass %s", "Kütle %s");
        l.add("gui.spacebreaker.assembler.stage_line", "Stage %s (%s): Δv %s, TWR %s, %s propellant, %s s burn", "Kademe %s (%s): Δv %s, İ/A %s, %s yakıt, %s s yanma");
        l.add("gui.spacebreaker.assembler.payload", "Payload: %s, %s seat(s)", "Faydalı yük: %s, %s koltuk");
        l.add("gui.spacebreaker.milestone.orbit", "Earth orbit", "Dünya yörüngesi");
        l.add("gui.spacebreaker.milestone.moon_orbit", "Moon orbit", "Ay yörüngesi");
        l.add("gui.spacebreaker.milestone.landing", "Moon landing", "Ay'a iniş");
        l.add("gui.spacebreaker.milestone.lunar_ascent", "Lunar ascent", "Ay'dan kalkış");
        l.add("gui.spacebreaker.milestone.home", "Back home", "Eve dönüş");
        l.add("gui.spacebreaker.console.launch", "Launch", "Fırlat");
        l.add("gui.spacebreaker.console.abort", "Abort", "Durdur");
        l.add("gui.spacebreaker.console.check", "Check pad", "Rampayı denetle");
        l.add("gui.spacebreaker.console.pad", "Pad and trench (%s/25)", "Rampa ve kanal (%s/25)");
        l.add("gui.spacebreaker.console.tower", "Service tower (%s high)", "Servis kulesi (%s yüksek)");
        l.add("gui.spacebreaker.console.arm", "Crew access arm", "Erişim kolu");
        l.add("gui.spacebreaker.console.umbilical", "Fuel umbilical", "Yakıt hattı");
        l.add("gui.spacebreaker.console.rocket", "Rocket on the pad", "Rampada roket");
        l.add("gui.spacebreaker.console.crew", "Crew aboard (%s)", "Mürettebat (%s)");
        l.add("gui.spacebreaker.console.fuel", "Propellant %s%%", "Yakıt %%%s");
        l.add("gui.spacebreaker.console.twr", "Thrust/weight %s", "İtki/ağırlık %s");
        l.add("gui.spacebreaker.console.dv", "Δv %s / %s m/s", "Δv %s / %s m/s");
        l.add("gui.spacebreaker.console.return", "Return possible", "Dönüş mümkün");
        l.add("gui.spacebreaker.console.status.no_rocket", "No rocket", "Roket yok");
        l.add("gui.spacebreaker.star_map", "Star Map", "Yıldız Haritası");
        l.add("gui.spacebreaker.star_map.window_open", "Launch window: now is a good time", "Fırlatma penceresi: şu an uygun");
        l.add("gui.spacebreaker.star_map.window_wait", "Launch window: best in %s (now costs more fuel)", "Fırlatma penceresi: en iyisi %s sonra (şimdi daha çok yakıt ister)");
        l.add("gui.spacebreaker.star_map.wait_hours", "%s h %s min", "%s sa %s dk");
        l.add("gui.spacebreaker.star_map.wait_minutes", "%s min", "%s dk");
        l.add("gui.spacebreaker.star_map.sun", "Sun", "Güneş");
        l.add("gui.spacebreaker.star_map.nearby_stars", "Nearby stars ▶", "Yakın yıldızlar ▶");
        l.add("gui.spacebreaker.star_map.stars_title", "Stars within 25 light years of the Sun", "Güneş'e 25 ışık yılı içindeki yıldızlar");
        l.add("gui.spacebreaker.star_map.light_years", "%s ly", "%s ışık yılı");
        l.add("gui.spacebreaker.star_map.stars_loading", "Looking at the sky...", "Gökyüzüne bakılıyor...");
        l.add("gui.spacebreaker.star_map.milky_way", "Milky Way", "Samanyolu");
        l.add("gui.spacebreaker.star_map.pick_star", "Click a star to learn about it. Travel between stars is not possible yet.", "Hakkında bilgi almak için bir yıldıza tıkla. Yıldızlar arası yolculuk henüz mümkün değil.");
        l.add("gui.spacebreaker.star_map.star_kind.main_sequence", "A %s-type star like the Sun, steadily burning hydrogen", "Güneş gibi hidrojen yakan %s tipi bir yıldız");
        l.add("gui.spacebreaker.star_map.star_kind.white_dwarf", "A white dwarf: the hot, Earth-sized core left by a dead star (class %s)", "Beyaz cüce: ölmüş bir yıldızdan kalan sıcak, Dünya boyutunda çekirdek (%s sınıfı)");
        l.add("gui.spacebreaker.star_map.star_kind.neutron_star", "A neutron star: a city-sized remnant heavier than the Sun", "Nötron yıldızı: Güneş'ten ağır, bir şehir büyüklüğünde kalıntı");
        l.add("gui.spacebreaker.star_map.star_kind.black_hole", "A black hole: not even light escapes it", "Kara delik: ışık bile kaçamaz");
        l.add("gui.spacebreaker.star_map.star_distance", "Distance: %s light years", "Uzaklık: %s ışık yılı");
        l.add("gui.spacebreaker.star_map.star_planets", "Planets: %s", "Gezegen sayısı: %s");
        l.add("gui.spacebreaker.star_map.base", "Base on the surface · landing beacons: %s", "Yüzeyde üs · %s iniş işareti");
        l.add("gui.spacebreaker.star_map.base_owner", "%s's base on the surface · landing beacons: %s", "Yüzeyde üs (kuran: %s) · %s iniş işareti");
        l.add("gui.spacebreaker.star_map.station", "Space station in orbit · docking ports: %s", "Yörüngede uzay istasyonu · %s yanaşma portu");
        l.add("gui.spacebreaker.star_map.station_owner", "%s's space station in orbit · docking ports: %s", "Yörüngede uzay istasyonu (kuran: %s) · %s yanaşma portu");
        l.add("gui.spacebreaker.star_map.star_habitable", "In the habitable zone: %s", "Yaşanabilir bölgede: %s");
        l.add("gui.spacebreaker.star_map.star_binary", "Double star: two suns", "Çift yıldız: iki güneş");
        l.add("gui.spacebreaker.star_map.star_planets_mixed", "Planets: %s known, %s not yet discovered", "Gezegenler: %s bilinen, %s henüz keşfedilmedi");
        l.add("gui.spacebreaker.star_map.star_mass", "Mass: %s × the Sun", "Kütle: Güneş'in %s katı");
        l.add("gui.spacebreaker.star_map.star_light", "Brightness: %s × the Sun", "Parlaklık: Güneş'in %s katı");
        l.add("gui.spacebreaker.star_map.star_naked_eye", "Visible to the naked eye from Earth", "Dünya'dan çıplak gözle görülür");
        l.add("gui.spacebreaker.star_map.star_telescope", "Too faint to see without a telescope", "Teleskopsuz görülemeyecek kadar sönük");
        l.add("gui.spacebreaker.star_map.block.interstellar", "Needs the deep space drive research: travel between stars is not possible yet.", "Derin uzay sürücüsü araştırması gerekiyor: yıldızlar arası yolculuk henüz mümkün değil.");
        l.add("gui.spacebreaker.star_map.you_are_here", "You are here", "Buradasın");
        l.add("gui.spacebreaker.star_map.system_of", "The %s system", "%s sistemi");
        l.add("body.spacebreaker.near_earth_asteroids", "Near-Earth asteroids", "Dünya'ya yakın asteroitler");
        l.add("body.spacebreaker.near_earth_asteroids.short", "Asteroids", "Asteroitler");
        l.add("body.spacebreaker.comets", "Comets", "Kuyruklu yıldızlar");
        l.add("gui.spacebreaker.star_map.comet.distance", "From the Sun: %s AU", "Güneş'e uzaklık: %s AU");
        l.add("gui.spacebreaker.star_map.comet.perihelion", "Next closest pass: %s (%s AU)", "Güneş'e en yakın geçiş: %s (%s AU)");
        l.add("gui.spacebreaker.star_map.comet.period", "One orbit: %s years", "Bir tur: %s yıl");
        l.add("gui.spacebreaker.star_map.comet.activity", "Activity: %s", "Etkinlik: %s");
        l.add("gui.spacebreaker.star_map.back", "◀ Solar System", "◀ Güneş Sistemi");
        l.add("gui.spacebreaker.star_map.to_orbit", "Go to orbit", "Yörüngeye git");
        l.add("gui.spacebreaker.star_map.land", "Land", "Yüzeye in");
        l.add("gui.spacebreaker.star_map.soon", "Coming soon", "Yakında");
        l.add("gui.spacebreaker.star_map.no_surface", "No ground to land on", "İnilecek yüzey yok");
        l.add("gui.spacebreaker.star_map.go", "Go", "Git");
        l.add("gui.spacebreaker.star_map.pick_target", "Click a world on the map, then choose to go to its orbit or land on it.", "Haritada bir dünyaya tıkla, sonra yörüngesine mi gideceğini yoksa yüzeyine mi ineceğini seç.");
        l.add("gui.spacebreaker.star_map.trip_to", "Trip to %s", "%s yolculuğu");
        l.add("gui.spacebreaker.star_map.fuel_line", "Fuel needed %s · on board %s", "Gereken yakıt %s · roketinde %s");
        l.add("gui.spacebreaker.star_map.enough", "Enough ✔", "Yeter ✔");
        l.add("gui.spacebreaker.star_map.need_nuclear", "This comet is only reached with a nuclear thermal engine (and insulated tanks for the years on the way)",
                "Bu kuyruklu yıldıza ancak nükleer termal motorla ulaşılır (yoldaki yıllar için yalıtımlı tanklar da gerekir)");
        l.add("gui.spacebreaker.star_map.block.observation_only", "Observation only: no rocket can match this comet's speed in a sensible time",
                "Yalnız gözlem: hiçbir roket bu kuyruklu yıldızın hızını makul bir sürede yakalayamaz");
        l.add("gui.spacebreaker.star_map.boil_off", "Boil-off on the way: %s%% of the propellant on board (counted above)",
                "Yolda buharlaşma kaybı: roketteki yakıtın %%%s'i (yukarıdaki hesaba dahil)");
        l.add("gui.spacebreaker.star_map.short", "%s short ✘", "%s eksik ✘");
        l.add("gui.spacebreaker.star_map.return_yes", "Fuel to come home: enough ✔", "Geri dönüş yakıtı: yeter ✔");
        l.add("gui.spacebreaker.star_map.return_refuel", "Fuel to come home: make it there ⛽ (fuel can be produced on this world)", "Geri dönüş yakıtı: orada üret ⛽ (bu dünyada yakıt üretilebilir)");
        l.add("message.spacebreaker.refuel_here", "Not enough fuel to get home, but propellant can be made on this world: refuel here.", "Eve dönecek yakıt yok ama bu dünyada yakıt üretilebilir: burada yakıt al.");
        l.add("gui.spacebreaker.star_map.return_no", "Fuel to come home: not enough ✘", "Geri dönüş yakıtı: yetmez ✘");
        l.add("gui.spacebreaker.star_map.travel_time", "Trip time: %s days", "Yolculuk süresi: %s gün");
        l.add("gui.spacebreaker.star_map.travel_time_short", "Trip time: %s", "Yolculuk süresi: %s");
        l.add("gui.spacebreaker.star_map.need_shield", "Needs a stronger heat shield to come down through the air", "Havadan geçerek inmek için daha güçlü bir ısı kalkanı gerekli");
        l.add("gui.spacebreaker.star_map.need_legs", "No landing legs: the landing will be rough", "İniş bacağı yok: iniş sert olacak");
        l.add("gui.spacebreaker.star_map.need_chute", "No parachute: add one for a soft landing", "Paraşüt yok: yumuşak iniş için bir paraşüt ekle");
        l.add("gui.spacebreaker.star_map.first_stop", "First stop: %s", "İlk durak: %s");
        l.add("gui.spacebreaker.star_map.delta_v_help", "Fuel here means Δv (delta-v): how much your rocket can still change its speed. Every trip uses some; the numbers are in metres per second.", "Buradaki yakıt Δv (delta-v) demek: roketinin hızını daha ne kadar değiştirebileceği. Her yolculuk bir kısmını harcar; sayılar saniyede metre cinsindendir.");
        l.add("gui.spacebreaker.star_map.orbit_short", "Orbit", "Yörünge");
        l.add("gui.spacebreaker.star_map.no_surface_short", "No ground", "Yüzey yok");
        l.add("gui.spacebreaker.star_map.pick_short", "Pick a world on the map.", "Haritadan bir dünya seç.");
        l.add("gui.spacebreaker.star_map.station_short", "Station · %s ports", "İstasyon · %s port");
        l.add("gui.spacebreaker.star_map.station_modules", "Station · %s modules · %s ports", "İstasyon · %s modül · %s port");
        l.add("gui.spacebreaker.star_map.station_yard", "Station · %s modules · %s ports · shipyard", "İstasyon · %s modül · %s port · tersane");
        l.add("hud.spacebreaker.docking_line", "→ %s station · docking", "→ %s istasyonu · kenetleniyor");
        l.add("gui.spacebreaker.star_map.base_short", "Base · %s beacons", "Üs · %s işaret");
        l.add("gui.spacebreaker.star_map.refuel_short", "⛽ Fuel can be made here", "⛽ Burada yakıt üretilebilir");
        l.add("gui.spacebreaker.star_map.comet_short", "%s AU from the Sun · %s", "Güneş'e %s AU · %s");
        l.add("gui.spacebreaker.star_map.no_signal_short", "No signal here", "Burada sinyal yok");
        l.add("gui.spacebreaker.star_map.mode_short.balanced", "Balanced: %s%% of real Δv", "Dengeli: gerçek Δv'nin %%%s'i");
        l.add("gui.spacebreaker.star_map.mode_short.easy", "Easy: %s%% of real Δv", "Kolay: gerçek Δv'nin %%%s'i");
        l.add("gui.spacebreaker.star_map.stat_dv", "Δv", "Δv");
        l.add("gui.spacebreaker.star_map.stat_time", "Time", "Süre");
        l.add("gui.spacebreaker.star_map.unit_days", "%s d", "%s gün");
        l.add("gui.spacebreaker.star_map.unit_hours", "%s h %s m", "%s sa %s dk");
        l.add("gui.spacebreaker.star_map.on_board", "On board: %s m/s", "Rokette: %s m/s");
        l.add("gui.spacebreaker.star_map.home_yes", "Home: enough fuel", "Dönüş: yakıt yeter");
        l.add("gui.spacebreaker.star_map.home_refuel", "Home: make fuel there", "Dönüş: orada yakıt üret");
        l.add("gui.spacebreaker.star_map.home_no", "Home: not enough fuel", "Dönüş: yakıt yetmez");
        l.add("gui.spacebreaker.star_map.controls", "Scroll: zoom · drag: move · right-click: all", "Tekerlek: yakınlaş · sürükle: kaydır · sağ tık: tümü");
        l.add("gui.spacebreaker.star_map.mode.balanced", "Balanced mode: trips take %s%% of the real fuel", "Dengeli mod: yolculuklar gerçek yakıtın %%%s kadarını ister");
        l.add("gui.spacebreaker.star_map.mode.easy", "Easy mode: trips take %s%% of the real fuel", "Kolay mod: yolculuklar gerçek yakıtın %%%s kadarını ister");
        l.add("gui.spacebreaker.star_map.block.none", "", "");
        l.add("gui.spacebreaker.star_map.block.here", "You are already here.", "Zaten buradasın.");
        l.add("gui.spacebreaker.star_map.block.not_in_orbit", "Reach orbit first: press the launch key on the pad.", "Önce yörüngeye çık: rampada fırlatma tuşuna bas.");
        l.add("gui.spacebreaker.star_map.block.not_built", "This world is not in the game yet.", "Bu dünya henüz oyunda yok.");
        l.add("gui.spacebreaker.star_map.block.no_route", "There is no way to fly there from here.", "Buradan oraya uçmanın yolu yok.");
        l.add("gui.spacebreaker.star_map.block.needs_heat_shield", "Needs a heat shield: the air there would burn the rocket up. Put one under the capsule, satellite or probe.",
                "Isı kalkanı gerekiyor: oradaki hava roketi yakar. Kapsülün, uydunun ya da sondanın altına bir kalkan tak.");
        l.add("gui.spacebreaker.star_map.block.not_enough_fuel", "Not enough fuel: refuel or build a bigger rocket.", "Yakıt yetmiyor: yakıt al ya da daha büyük bir roket yap.");
        l.add("gui.spacebreaker.star_map.block.busy", "The rocket is busy.", "Roket şu an meşgul.");
        l.add("gui.spacebreaker.star_map.hazard.no_air", "No air", "Hava yok");
        l.add("gui.spacebreaker.star_map.hazard.thin_air", "Unbreathable air", "Solunamaz hava");
        l.add("gui.spacebreaker.star_map.hazard.crushing_air", "Crushing air", "Ezici basınç");
        l.add("gui.spacebreaker.star_map.hazard.hot", "Hot", "Sıcak");
        l.add("gui.spacebreaker.star_map.hazard.cold", "Cold", "Soğuk");
        l.add("gui.spacebreaker.star_map.hazard.radiation", "Radiation", "Radyasyon");
        for (String[] k : new String[][]{
                {"terrestrial", "Rocky planet", "Kayalık gezegen"}, {"dwarf", "Dwarf planet", "Cüce gezegen"}, {"moon", "Moon", "Uydu"},
                {"gas_giant", "Gas giant", "Gaz devi"}, {"ice_giant", "Ice giant", "Buz devi"}, {"asteroid", "Asteroid", "Asteroit"}, {"comet", "Comet", "Kuyruklu yıldız"},
                {"star", "Star", "Yıldız"}, {"station", "Space station", "Uzay istasyonu"}}) {
            l.add("gui.spacebreaker.star_map.kind." + k[0], k[1], k[2]);
        }
        for (String[] k : new String[][]{
                {"terrestrial", "A rocky world.", "Kayalık bir dünya."},
                {"dwarf", "A small world, too light to clear its own path around the Sun.", "Güneş çevresindeki yolunu temizleyemeyecek kadar hafif, küçük bir dünya."},
                {"moon", "A moon circling its planet.", "Gezegeninin çevresinde dönen bir uydu."},
                {"gas_giant", "A giant ball of gas with no ground to stand on.", "Üzerinde durulacak zemini olmayan dev bir gaz küresi."},
                {"ice_giant", "A cold giant of gas and ice, with no ground to stand on.", "Gaz ve buzdan soğuk bir dev; üzerinde durulacak zemin yok."},
                {"asteroid", "A lump of rock left over from the birth of the planets.", "Gezegenlerin doğumundan kalmış bir kaya parçası."},
                {"comet", "A dirty snowball on a long, stretched orbit: near the Sun its ice boils off into a glowing tail.",
                        "Uzun, basık bir yörüngede kirli bir kar topu: Güneş'e yaklaşınca buzu kaynayıp parlayan bir kuyruğa dönüşür."},
                {"star", "", ""}, {"station", "A home built in space.", "Uzayda kurulmuş bir yuva."}}) {
            l.add("gui.spacebreaker.star_map.about_kind." + k[0], k[1], k[2]);
        }
        for (String[] k : new String[][]{
                {"mercury", "The smallest planet, closest to the Sun: scorching by day, freezing by night.", "Güneş'e en yakın, en küçük gezegen: gündüz kavurucu, gece dondurucu."},
                {"venus", "Earth's twin in size, hidden under acid clouds. The air on the ground would crush a submarine.", "Boyutça Dünya'nın ikizi, asit bulutlarının altında saklı. Yerdeki hava bir denizaltıyı ezerdi."},
                {"earth", "Home: air to breathe, water and a blue sky.", "Evimiz: solunacak hava, su ve mavi gökyüzü."},
                {"moon", "Earth's grey companion: no air, low gravity, dusty seas of old lava.", "Dünya'nın gri yoldaşı: hava yok, yerçekimi az, eski lavdan tozlu denizler."},
                {"mars", "The red planet: thin cold air, giant canyons and ice at the poles. Its ice can be made into rocket fuel.", "Kızıl gezegen: ince soğuk hava, dev kanyonlar ve kutuplarda buz. Buzu roket yakıtına çevrilebilir."},
                {"phobos", "Mars's larger moon, a lumpy rock that skims low over the planet.", "Mars'ın büyük uydusu; gezegenin hemen üstünden geçen yumru bir kaya."},
                {"deimos", "Mars's tiny outer moon: you could almost jump off it.", "Mars'ın minik dış uydusu: neredeyse zıplayarak üstünden kurtulabilirsin."},
                {"ceres", "The largest body in the asteroid belt, with bright salt patches in its craters.", "Asteroit kuşağının en büyük cismi; kraterlerinde parlak tuz lekeleri var."},
                {"vesta", "A battered asteroid with a mountain twice as tall as Everest.", "Everest'in iki katı yüksekliğinde bir dağı olan hırpalanmış bir asteroit."},
                {"jupiter", "The biggest planet, a storm-striped ball of gas. Its radiation belts are deadly.", "En büyük gezegen; fırtına çizgili bir gaz küresi. Radyasyon kuşakları ölümcül."},
                {"io", "Jupiter's volcanic moon: sulphur plains and fountains of lava.", "Jüpiter'in volkanik uydusu: kükürt ovaları ve lav fıskiyeleri."},
                {"europa", "An ice shell over a hidden ocean, deep in Jupiter's radiation.", "Jüpiter'in radyasyonu içinde, gizli bir okyanusun üstünde buz kabuk."},
                {"ganymede", "The largest moon in the Solar System, bigger than Mercury.", "Güneş Sistemi'nin en büyük uydusu; Merkür'den bile büyük."},
                {"callisto", "An old, cratered moon of ice and rock, outside Jupiter's worst radiation.", "Jüpiter'in en sert radyasyonunun dışında, buz ve kayadan yaşlı, kraterli bir uydu."},
                {"saturn", "The ringed giant: a world of gas lighter than water.", "Halkalı dev: sudan hafif bir gaz dünyası."},
                {"enceladus", "A bright little ice moon that sprays water into space.", "Uzaya su püskürten parlak, küçük bir buz uydu."},
                {"titan", "A moon with thick orange haze and lakes of liquid methane.", "Kalın turuncu pusu ve sıvı metan gölleri olan bir uydu."},
                {"uranus", "A pale blue ice giant that rolls around the Sun on its side.", "Güneş'in çevresinde yan yatmış dönen soluk mavi bir buz devi."},
                {"miranda", "A small moon of Uranus with cliffs taller than any on Earth.", "Uranüs'ün, Dünya'dakilerden yüksek uçurumları olan küçük bir uydusu."},
                {"neptune", "The farthest planet: deep blue, cold and windy.", "En uzak gezegen: koyu mavi, soğuk ve rüzgârlı."},
                {"triton", "Neptune's frozen moon, with geysers of nitrogen.", "Neptün'ün donmuş uydusu; azot gayzerleri var."},
                {"pluto", "A small icy world at the edge of the planets, with a heart-shaped plain.", "Gezegenlerin kıyısında, kalp biçimli bir ovası olan küçük buzlu bir dünya."},
                {"charon", "Pluto's big moon: the two circle each other like a pair.", "Plüton'un büyük uydusu: ikisi bir çift gibi birbirinin çevresinde döner."}}) {
            l.add("gui.spacebreaker.star_map.about." + k[0], k[1], k[2]);
        }
        l.add("location.spacebreaker.earth_surface", "Earth", "Dünya");
        l.add("location.spacebreaker.orbit_of", "%s orbit", "%s yörüngesi");
        l.add("body.spacebreaker.earth", "Earth", "Dünya");
        // bodies no other area names yet (the outer system beyond Saturn)
        // (Neptune, Triton, Pluto and Charon are named by their own world modules)
        for (String[] b : new String[][]{{"uranus", "Uranus", "Uranüs"}, {"miranda", "Miranda", "Miranda"}}) {
            l.add("body.spacebreaker." + b[0], b[1], b[2]);
        }
        // comets have no world module: the map names them from the astro library's catalogue
        for (var c : com.pizzaparker07.spacebreaker.core.astro.smallbody.Comet.CATALOGUE) {
            l.add("body.spacebreaker." + c.id(), c.nameEn(), c.nameTr());
        }
    }
}
