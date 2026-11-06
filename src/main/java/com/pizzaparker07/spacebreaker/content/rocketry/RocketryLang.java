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
        l.item(RocketryModule.KEROSENE_ENGINE, "Kerosene Engine (Sea Level)", "Kerosen Motoru (Deniz Seviyesi)");
        l.item(RocketryModule.VACUUM_ENGINE, "Kerosene Engine (Vacuum)", "Kerosen Motoru (Vakum)");
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
        l.add("container.spacebreaker.rocket_assembler", "Rocket Assembler", "Roket Montaj Tezgâhı");
        l.add("container.spacebreaker.launch_console", "Launch Control", "Fırlatma Kontrolü");
        l.add("key.categories.spacebreaker.spaceflight", "Spacebreaker: Spaceflight", "Spacebreaker: Uzay Uçuşu");
        l.add("key.spacebreaker.launch", "Launch / abort countdown", "Fırlat / geri sayımı durdur");
        l.add("key.spacebreaker.star_map", "Star map", "Yıldız haritası");
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
        l.add("hud.spacebreaker.delta_v", "Δv left: %s", "Kalan Δv: %s");
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
        l.add("gui.spacebreaker.assembler.stage_line", "Stage %s: Δv %s, TWR %s, %s propellant, %s s burn", "Kademe %s: Δv %s, İ/A %s, %s yakıt, %s s yanma");
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
        l.add("gui.spacebreaker.star_map.burn", "Burn", "Ateşle");
        l.add("gui.spacebreaker.star_map.available", "Δv available: %s", "Kullanılabilir Δv: %s");
        l.add("gui.spacebreaker.star_map.route", "To %s: %s", "%s: %s");
        l.add("gui.spacebreaker.star_map.too_far", "Not enough propellant for this trip", "Bu yolculuk için yakıt yetmiyor");
        l.add("gui.spacebreaker.star_map.return_ok", "You can still get home afterwards", "Sonrasında eve dönebilirsin");
        l.add("gui.spacebreaker.star_map.no_return", "Warning: no fuel to come back from there!", "Uyarı: oradan dönecek yakıt yok!");
        l.add("gui.spacebreaker.star_map.window_open", "Launch window open · transfer %s days", "Fırlatma penceresi açık · transfer %s gün");
        l.add("gui.spacebreaker.star_map.window_wait", "Next launch window in %s · transfer %s days", "Sonraki fırlatma penceresi %s sonra · transfer %s gün");
        l.add("gui.spacebreaker.star_map.scaled.balanced", "Balanced mode: Δv is %s%% of the real value", "Dengeli mod: Δv gerçek değerin %%%s kadarı");
        l.add("gui.spacebreaker.star_map.scaled.easy", "Easy mode: Δv is %s%% of the real value", "Kolay mod: Δv gerçek değerin %%%s kadarı");
        l.add("gui.spacebreaker.star_map.real", "real: %s km/s", "gerçek: %s km/s");
        l.add("gui.spacebreaker.star_map.pick", "Pick a destination", "Bir hedef seç");
        l.add("gui.spacebreaker.star_map.reach_orbit", "Destinations can be chosen once in orbit", "Hedefler yörüngedeyken seçilebilir");

        l.add("location.spacebreaker.earth_surface", "Earth", "Dünya");
        l.add("gui.spacebreaker.star_map.here", "You are at: %s", "Konum: %s");
        l.add("location.spacebreaker.orbit_of", "%s orbit", "%s yörüngesi");
        l.add("body.spacebreaker.earth", "Earth", "Dünya");
    }
}
