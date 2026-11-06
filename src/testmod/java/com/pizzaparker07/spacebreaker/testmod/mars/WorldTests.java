package com.pizzaparker07.spacebreaker.testmod.mars;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.dimension.LevelStem;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

/** Shared helpers for the inner-planet GameTests: dimension stems, generators, profiles and debug maps. */
public final class WorldTests {
    public static final String EMPTY = "empty";

    private WorldTests() {
    }

    /** Decodes a data-pack dimension (the game test server never loads them). */
    public static LevelStem stem(GameTestHelper helper, String name) {
        var server = helper.getLevel().getServer();
        try (var reader = server.getResourceManager().openAsReader(Spacebreaker.id("dimension/" + name + ".json"))) {
            JsonElement json = JsonParser.parseReader(reader);
            return LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()), json)
                    .getOrThrow(msg -> new AssertionError(name + " dimension JSON is invalid: " + msg));
        } catch (java.io.IOException e) {
            throw new AssertionError(name + " dimension JSON missing", e);
        }
    }

    public static PlanetChunkGenerator generator(GameTestHelper helper, String name, long seed) {
        if (!(stem(helper, name).generator() instanceof PlanetChunkGenerator gen)) {
            throw new AssertionError(name + " must use the planet generator");
        }
        gen.bindSeed(seed);
        return gen;
    }

    public static TerrainProfile profile(GameTestHelper helper, String name) {
        return helper.getLevel().registryAccess().registryOrThrow(TerrainProfile.REGISTRY)
                .getOptional(Spacebreaker.id(name)).orElseThrow(() -> new AssertionError(name + " terrain profile missing"));
    }

    private static final Map<PlanetTerrain.Zone, Integer> ZONE_COLOURS = Map.of(
            PlanetTerrain.Zone.HIGHLANDS, 0xB07850, PlanetTerrain.Zone.LOWLANDS, 0x9A6040, PlanetTerrain.Zone.CRATER, 0x806050,
            PlanetTerrain.Zone.VOLCANIC, 0x404040, PlanetTerrain.Zone.POLAR, 0xC0B0A8, PlanetTerrain.Zone.DUNES, 0x504038,
            PlanetTerrain.Zone.CANYON, 0xD09060, PlanetTerrain.Zone.VOLCANO, 0x585058, PlanetTerrain.Zone.VALLEY, 0x9AA088,
            PlanetTerrain.Zone.ICE_CAP, 0xF0F4F8);

    /**
     * Writes a shaded relief map coloured by zone to {@code maps/<name>.png} in the test run directory, for reviewing
     * terrain by eye. Never fails a test.
     */
    public static void dumpMap(PlanetTerrain t, String name, double cx, double cz, double size, int px) {
        try {
            BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_RGB);
            double step = size / px;
            double[] prev = new double[px];
            for (int j = 0; j < px; j++) {
                for (int i = 0; i < px; i++) {
                    double x = cx - size / 2 + i * step;
                    double z = cz - size / 2 + j * step;
                    PlanetTerrain.Column c = t.column(x, z);
                    double hx = t.height(x + step, z);
                    double hz = t.height(x, z + step);
                    double shade = 0.75 + Math.max(-0.5, Math.min(0.5, (c.height - hx + c.height - hz) * 0.12 / Math.max(1, step / 4)));
                    double alt = 0.7 + 0.3 * Math.max(0, Math.min(1, (c.height - 40) / 200));
                    int rgb = ZONE_COLOURS.getOrDefault(c.zone(t.profile()), 0xFF00FF);
                    int r = clamp((int) (((rgb >> 16) & 255) * shade * alt));
                    int g = clamp((int) (((rgb >> 8) & 255) * shade * alt));
                    int b = clamp((int) ((rgb & 255) * shade * alt));
                    img.setRGB(i, j, (r << 16) | (g << 8) | b);
                    prev[i] = c.height;
                }
            }
            File dir = new File("maps");
            dir.mkdirs();
            ImageIO.write(img, "png", new File(dir, name + ".png"));
        } catch (Exception e) {
            Spacebreaker.LOGGER.warn("could not write terrain map {}", name, e);
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
