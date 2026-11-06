package com.pizzaparker07.spacebreaker.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server (gameplay, synced) and client (visual) settings. */
public final class SbConfig {
    /** How much real physics is softened for fun. Data always stores real values. */
    public enum Realism {
        REALISTIC(1.0),
        BALANCED(0.75),
        EASY(0.5);

        private final double gravityExponent;

        Realism(double gravityExponent) {
            this.gravityExponent = gravityExponent;
        }

        /** Maps real relative gravity (Moon 0.165) to the applied factor (BALANCED Moon ≈ 0.26). */
        public double scaleGravity(double relative) {
            if (relative <= 0) {
                return 0;
            }
            return Math.pow(relative, gravityExponent);
        }
    }

    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    private static final ModConfigSpec.EnumValue<Realism> REALISM;
    public static final ModConfigSpec.DoubleValue TIME_SCALE;
    public static final ModConfigSpec.BooleanValue SURVIVAL_HAZARDS;

    public static final ModConfigSpec.EnumValue<Quality> EFFECTS_QUALITY;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    public static final ModConfigSpec.BooleanValue CINEMATIC_CAMERA;
    public static final ModConfigSpec.BooleanValue MISSION_CONTROL_VOICE;

    public enum Quality { LOW, MEDIUM, HIGH }

    static {
        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        server.push("gameplay");
        REALISM = server.comment("How strongly real physics applies: REALISTIC, BALANCED (default) or EASY.")
                .defineEnum("realism", Realism.BALANCED);
        TIME_SCALE = server.comment("How many real seconds one in-game hour of orbital/planetary time takes.")
                .defineInRange("orbitalTimeScale", 1.0, 0.01, 100.0);
        SURVIVAL_HAZARDS = server.comment("Vacuum, temperature and radiation can hurt players.")
                .define("survivalHazards", true);
        server.pop();
        SERVER_SPEC = server.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        client.push("visuals");
        EFFECTS_QUALITY = client.comment("Particle and effect density.").defineEnum("effectsQuality", Quality.HIGH);
        HUD_SCALE = client.comment("Helmet HUD scale.").defineInRange("hudScale", 1.0, 0.5, 2.0);
        CINEMATIC_CAMERA = client.comment("Use the cinematic camera during launches and landings.").define("cinematicCamera", true);
        client.pop();
        client.push("audio");
        MISSION_CONTROL_VOICE = client.comment("Spoken mission control announcements (countdown, liftoff, staging, orbit, landing, warnings), "
                + "in Turkish when the game language is Turkish, otherwise in English.").define("missionControlVoice", true);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    public static Realism realism() {
        return SERVER_SPEC.isLoaded() ? REALISM.get() : Realism.BALANCED;
    }

    private SbConfig() {
    }
}
