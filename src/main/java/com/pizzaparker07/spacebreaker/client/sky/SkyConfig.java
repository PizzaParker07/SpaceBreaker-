package com.pizzaparker07.spacebreaker.client.sky;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client sky settings (file: spacebreaker-worlds-client.toml). */
public final class SkyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue BODY_SCALE;
    public static final ModConfigSpec.EnumValue<PlanetStyle> PLANET_STYLE;

    /** How planets and moons look from outside. */
    public enum PlanetStyle {
        /** A ball built from blocks. */
        VOXEL,
        /** A slowly turning cube with pixel faces. */
        CUBE,
        /** The old smooth disc. */
        ROUND
    }

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("sky");
        BODY_SCALE = b.comment("Apparent size of the sun and planets relative to reality (1 = real angular size, which is tiny on screen).")
                .defineInRange("bodyScale", 8.0, 1.0, 20.0);
        PLANET_STYLE = b.comment("How planets and moons look from outside: VOXEL (a ball of blocks), CUBE (a pixel cube) or ROUND (smooth disc).")
                .defineEnum("planetStyle", PlanetStyle.VOXEL);
        b.pop();
        SPEC = b.build();
    }

    public static double bodyScale() {
        // dev checks (showcase) may enlarge bodies past the setting's range to walk through every level of detail
        String dev = System.getProperty("spacebreaker.bodyScale");
        if (dev != null) {
            try {
                return Double.parseDouble(dev);
            } catch (NumberFormatException ignored) {
                // fall through to the setting
            }
        }
        return SPEC.isLoaded() ? BODY_SCALE.get() : 8.0;
    }

    public static PlanetStyle planetStyle() {
        return SPEC.isLoaded() ? PLANET_STYLE.get() : PlanetStyle.VOXEL;
    }

    /** The blocky style to draw with, or null for the old smooth disc. */
    public static com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh.Style blockyStyle() {
        return switch (planetStyle()) {
            case VOXEL -> com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh.Style.VOXEL;
            case CUBE -> com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh.Style.CUBE;
            case ROUND -> null;
        };
    }

    private SkyConfig() {
    }
}
