package com.pizzaparker07.spacebreaker.client.hud;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client settings of the helmet HUD (spacebreaker-hud.toml). The scale lives in the main client config. */
public final class HudConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue OPACITY;
    public static final ModConfigSpec.BooleanValue VISOR_FRAME;
    public static final ModConfigSpec.DoubleValue VISOR_FRAME_OPACITY;
    public static final ModConfigSpec.BooleanValue REALISTIC_SOUND;
    public static final ModConfigSpec.BooleanValue DETAILED;

    // 1.1: the visor rim and realistic sound are off by default; their keys were renamed (visorFrame,
    // visorFrameOpacity, realisticSound) so that values saved by 1.0 with the old defaults do not keep them on
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("hud");
        ENABLED = b.comment("Show the helmet HUD while wearing an EVA helmet.").define("enabled", true);
        OPACITY = b.comment("Opacity of the helmet HUD (0.2 - 1.0).").defineInRange("opacity", 0.9, 0.2, 1.0);
        DETAILED = b.comment("Detailed helmet HUD: all gauges (body temperature, radiation, pressure, gravity, gas, way home).",
                "Off by default: only a small oxygen gauge, hazard icons when something is wrong and a one-line warning.").define("detailedHud", false);
        VISOR_FRAME = b.comment("Show a thin visor rim (and rime on it in the cold) around the screen in first person. Off by default.").define("visorRim", false);
        VISOR_FRAME_OPACITY = b.comment("Opacity of the visor rim (0.1 - 1.0).").defineInRange("visorRimOpacity", 0.35, 0.1, 1.0);
        b.pop();
        b.push("sound");
        REALISTIC_SOUND = b.comment("Realistic sound (off by default): in thin air and vacuum the world sounds a little quieter and softer.",
                "Your own steps and tools, helmet sounds and pressurised rooms stay normal.").define("atmosphereSound", false);
        b.pop();
        SPEC = b.build();
    }

    private HudConfig() {
    }

    public static boolean enabled() {
        return !SPEC.isLoaded() || ENABLED.get();
    }

    public static float opacity() {
        return SPEC.isLoaded() ? OPACITY.get().floatValue() : 0.9F;
    }

    public static float visorFrameOpacity() {
        return SPEC.isLoaded() ? VISOR_FRAME_OPACITY.get().floatValue() : 0.35F;
    }

    public static boolean detailed() {
        return SPEC.isLoaded() && DETAILED.get();
    }

    public static boolean realisticSound() {
        return SPEC.isLoaded() && REALISTIC_SOUND.get();
    }

    public static boolean visorFrame() {
        return SPEC.isLoaded() && VISOR_FRAME.get();
    }
}
