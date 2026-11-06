package com.pizzaparker07.spacebreaker.client.sky;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client sky settings (file: spacebreaker-worlds-client.toml). */
public final class SkyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue BODY_SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("sky");
        BODY_SCALE = b.comment("Apparent size of the sun and planets relative to reality (1 = real angular size, which is tiny on screen).")
                .defineInRange("bodyScale", 8.0, 1.0, 20.0);
        b.pop();
        SPEC = b.build();
    }

    public static double bodyScale() {
        return SPEC.isLoaded() ? BODY_SCALE.get() : 8.0;
    }

    private SkyConfig() {
    }
}
