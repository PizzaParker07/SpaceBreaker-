package com.pizzaparker07.spacebreaker.client.sky;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client sky settings (file: spacebreaker-worlds-client.toml). */
public final class SkyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue BODY_SCALE;
    public static final ModConfigSpec.EnumValue<PlanetStyle> PLANET_STYLE;
    /** Settings file version: 1 once the switch of the default style to CUBE has been applied to this file. */
    public static final ModConfigSpec.IntValue STYLE_VERSION;
    static final int CURRENT_STYLE_VERSION = 1;

    /** How planets and moons look from outside. */
    public enum PlanetStyle {
        /** A ball built from blocks. */
        VOXEL,
        /** A slowly turning cube with pixel faces. */
        CUBE,
        /** A ball of blocks far away that turns into a smooth sphere in its pixel map when it fills the view. */
        PIXEL,
        /** The old smooth disc. */
        ROUND
    }

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("sky");
        BODY_SCALE = b.comment("Apparent size of the sun and planets relative to reality (1 = real angular size, which is tiny on screen).")
                .defineInRange("bodyScale", 8.0, 1.0, 20.0);
        PLANET_STYLE = b.comment("How planets and moons look from outside: CUBE (a pixel cube, the default), VOXEL (a ball of blocks), PIXEL (blocks far away, a pixel-textured sphere close up) or ROUND (smooth disc).")
                .defineEnum("planetStyle", PlanetStyle.CUBE);
        STYLE_VERSION = b.comment("Internal: settings version, do not edit.")
                .defineInRange("styleVersion", 0, 0, 1000);
        b.pop();
        SPEC = b.build();
    }

    /**
     * Drawn angular diameter (degrees) of a giant parent hanging close over a small moon, from its real one: small bodies
     * get the usual {@link #bodyScale()}, but big ones grow ever more slowly (as the real size to the power 0.65), so two
     * moons at different distances still show their parent at clearly different sizes instead of both at the cap
     * (Mars: about 57 degrees from Phobos, 31 from Deimos), and never past 70 degrees.
     */
    public static double nearParentSize(double realDegrees) {
        return Math.min(70, Math.min(realDegrees * bodyScale(), 5.1 * Math.pow(realDegrees, 0.65)));
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
        return SPEC.isLoaded() ? PLANET_STYLE.get() : PlanetStyle.CUBE;
    }

    /**
     * One-time switch when the file is loaded: the default style became CUBE, so a file written before that (version 0)
     * that still says VOXEL, the old default, moves to CUBE once. The version is then set, so a player who later picks
     * VOXEL or ROUND on purpose keeps that choice.
     */
    static void migrate(net.neoforged.fml.event.config.ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC || event instanceof net.neoforged.fml.event.config.ModConfigEvent.Unloading) {
            return;
        }
        if (STYLE_VERSION.get() >= CURRENT_STYLE_VERSION) {
            return;
        }
        if (migratedStyle(PLANET_STYLE.get(), STYLE_VERSION.get()) != PLANET_STYLE.get()) {
            PLANET_STYLE.set(PlanetStyle.CUBE);
        }
        STYLE_VERSION.set(CURRENT_STYLE_VERSION);
        SPEC.save();
    }

    /** The style a settings file of {@code version} keeps after the one-time switch (pure, for tests). */
    public static PlanetStyle migratedStyle(PlanetStyle saved, int version) {
        return version < CURRENT_STYLE_VERSION && saved == PlanetStyle.VOXEL ? PlanetStyle.CUBE : saved;
    }

    /** The blocky style to draw with, or null for the old smooth disc. */
    public static com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh.Style blockyStyle() {
        return switch (planetStyle()) {
            case VOXEL, PIXEL -> com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh.Style.VOXEL;
            case CUBE -> com.pizzaparker07.spacebreaker.content.worldgen.BlockyBodyMesh.Style.CUBE;
            case ROUND -> null;
        };
    }

    private SkyConfig() {
    }
}
