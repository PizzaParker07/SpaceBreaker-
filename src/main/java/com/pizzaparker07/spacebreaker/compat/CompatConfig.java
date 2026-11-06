package com.pizzaparker07.spacebreaker.compat;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client renderer-compatibility settings (file: spacebreaker-compat-client.toml). */
public final class CompatConfig {
    public enum SkyMode {
        /** Draw our sky over the shader pack's sky when a pack is active (not with LOD mods, see COMPAT.md). */
        AUTO,
        /** Like AUTO, but also with Distant Horizons / Voxy installed (their far terrain is then hidden by the sky). */
        ALWAYS,
        /** Never: the shader pack draws its own (Overworld) sky in space worlds. */
        OFF
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<SkyMode> SHADER_SKY;
    public static final ModConfigSpec.BooleanValue HIDE_PACK_CLOUDS;
    public static final ModConfigSpec.BooleanValue ATMOSPHERE_HAZE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("shaders");
        SHADER_SKY = b.comment("Space skies (black sky, stars, sun, planets) with an Iris shader pack active.",
                        "AUTO: replace the pack's sky in space worlds; ALWAYS: also with LOD mods; OFF: leave the pack's sky.")
                .defineEnum("spaceSky", SkyMode.AUTO);
        HIDE_PACK_CLOUDS = b.comment("Move the shader pack's own clouds out of sight in space worlds.")
                .define("hidePackClouds", true);
        ATMOSPHERE_HAZE = b.comment("Thick and dusty air (Venus, Titan, Mars storms) and the dawn white balance over the shader pack.",
                        "Two full-screen draws per frame; turn off on slow GPUs (the pack then shows its own clear air).")
                .define("atmosphereHaze", true);
        b.pop();
        SPEC = b.build();
    }

    public static SkyMode skyMode() {
        return SPEC.isLoaded() ? SHADER_SKY.get() : SkyMode.AUTO;
    }

    public static boolean atmosphereHaze() {
        return !SPEC.isLoaded() || ATMOSPHERE_HAZE.get();
    }

    public static boolean hidePackClouds() {
        return !SPEC.isLoaded() || HIDE_PACK_CLOUDS.get();
    }

    private CompatConfig() {
    }
}
