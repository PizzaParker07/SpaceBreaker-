package com.pizzaparker07.spacebreaker.client.hud;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client settings of the helmet HUD (spacebreaker-hud.toml). The scale lives in the main client config. */
public final class HudConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue OPACITY;
    public static final ModConfigSpec.BooleanValue VISOR_FRAME;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("hud");
        ENABLED = b.comment("Show the helmet HUD while wearing an EVA helmet.").define("enabled", true);
        OPACITY = b.comment("Opacity of the helmet HUD (0.2 - 1.0).").defineInRange("opacity", 0.9, 0.2, 1.0);
        VISOR_FRAME = b.comment("Darken the screen edges like the inside of a visor.").define("visorFrame", true);
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

    public static boolean visorFrame() {
        return !SPEC.isLoaded() || VISOR_FRAME.get();
    }
}
