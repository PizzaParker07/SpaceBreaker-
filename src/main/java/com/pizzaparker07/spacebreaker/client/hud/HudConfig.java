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

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("hud");
        ENABLED = b.comment("Show the helmet HUD while wearing an EVA helmet.").define("enabled", true);
        OPACITY = b.comment("Opacity of the helmet HUD (0.2 - 1.0).").defineInRange("opacity", 0.9, 0.2, 1.0);
        VISOR_FRAME = b.comment("Show the visor frame (rounded rim with glass reflections) around the screen in first person.").define("visorFrame", true);
        VISOR_FRAME_OPACITY = b.comment("Opacity of the visor frame (0.1 - 1.0).").defineInRange("visorFrameOpacity", 0.7, 0.1, 1.0);
        b.pop();
        b.push("sound");
        REALISTIC_SOUND = b.comment("Realistic sound (off by default): in thin air and vacuum the world sounds a little quieter and softer.",
                "Your own steps and tools, helmet sounds and pressurised rooms stay normal.").define("realisticSound", false);
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
        return SPEC.isLoaded() ? VISOR_FRAME_OPACITY.get().floatValue() : 0.7F;
    }

    public static boolean realisticSound() {
        return SPEC.isLoaded() && REALISTIC_SOUND.get();
    }

    public static boolean visorFrame() {
        return !SPEC.isLoaded() || VISOR_FRAME.get();
    }
}
