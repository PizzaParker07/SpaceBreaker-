package com.pizzaparker07.spacebreaker.content.audio;

import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.neoforged.bus.api.IEventBus;

/** Sounds shared by every area: rocket, suit and the mission control voice (see {@link SbSounds}). */
public final class AudioModule implements SbModule {
    @Override
    public String id() {
        return "audio";
    }

    @Override
    public void register(IEventBus modBus) {
        SbSounds.SOUNDS.register(modBus);
    }

    @Override
    public void addTranslations(Lang lang) {
        String s = "subtitles.spacebreaker.";
        lang.add(s + "rocket.engine", "Rocket engine roars", "Roket motoru gürlüyor");
        lang.add(s + "rocket.ignition", "Engines ignite", "Motorlar ateşleniyor");
        lang.add(s + "rocket.stage_separation", "Stage separates", "Kademe ayrılıyor");
        lang.add(s + "rocket.parachute_deploy", "Parachute opens", "Paraşüt açılıyor");
        lang.add(s + "rocket.reentry", "Plasma roars around the capsule", "Kapsülün çevresinde plazma gürlüyor");
        lang.add(s + "suit.oxygen_alarm", "Suit alarm: oxygen low", "Giysi alarmı: oksijen azaldı");
        lang.add(s + "mission_control.radio", "Radio beep", "Telsiz sinyali");
        lang.add(s + "rocket.satellite_release", "Satellite released", "Uydu bırakıldı");
        lang.add(s + "rocket.probe_landing", "Probe touches down", "Sonda iniyor");
        lang.add(s + "station.airlock_cycle", "Airlock cycles", "Hava kilidi çalışıyor");
        lang.add(s + "vehicle.rover_motor", "Rover motors whine", "Gezici motorları vınlıyor");
        lang.add(s + "machine.electrolyzer", "Electrolyzer bubbles", "Elektroliz ünitesi fokurduyor");
        lang.add(s + "machine.drill", "Drill grinds rock", "Matkap kaya öğütüyor");
        lang.add(s + "machine.centrifuge", "Centrifuges whine", "Santrifüjler vınlıyor");
        lang.add(s + "machine.fission_reactor", "Reactor hums", "Reaktör uğulduyor");

        String m = "Mission control: ";
        String t = "Görev kontrol: ";
        for (int n = 10; n >= 1; n--) {
            lang.add(SbSounds.Line.countdown(n).subtitleKey(), m + "T-minus " + n, t + n);
        }
        lang.add(SbSounds.Line.IGNITION.subtitleKey(), m + "Ignition", t + "Ateşleme");
        lang.add(SbSounds.Line.LIFTOFF.subtitleKey(), m + "Liftoff!", t + "Kalkış!");
        lang.add(SbSounds.Line.STAGE_SEPARATION.subtitleKey(), m + "Stage separation confirmed", t + "Kademe ayrıldı");
        lang.add(SbSounds.Line.ORBIT_REACHED.subtitleKey(), m + "Orbit achieved", t + "Yörüngeye ulaşıldı");
        lang.add(SbSounds.Line.ENTRY_INTERFACE.subtitleKey(), m + "Entry interface, expect blackout",
                t + "Atmosfere giriş, iletişim kesilebilir");
        lang.add(SbSounds.Line.PARACHUTES.subtitleKey(), m + "Parachutes deployed", t + "Paraşütler açıldı");
        lang.add(SbSounds.Line.TOUCHDOWN.subtitleKey(), m + "Touchdown, welcome home", t + "Yere indik, eve hoş geldin");
        lang.add(SbSounds.Line.FUEL_LOW.subtitleKey(), m + "Fuel low", t + "Yakıt azaldı");
        lang.add(SbSounds.Line.OXYGEN_LOW.subtitleKey(), m + "Warning, oxygen low", t + "Uyarı, oksijen azaldı");
        lang.add(SbSounds.Line.COUNTDOWN_HOLD.subtitleKey(), m + "Countdown on hold", t + "Geri sayım durduruldu");
    }
}
