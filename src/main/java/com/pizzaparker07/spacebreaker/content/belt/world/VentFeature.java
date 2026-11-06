package com.pizzaparker07.spacebreaker.content.belt.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.List;
import java.util.Optional;

/**
 * Active vents: a vent block (volcanic plume source, ice geyser) in a small pit, ringed by the fallout it throws out -
 * an inner ring of fresh deposit and a patchy outer ring, like the red rings around Io's plumes or the fresh frost around
 * Enceladus' geysers. Vents sit on a jittered grid, optionally only inside a latitude band.
 *
 * @param vent      the vent block
 * @param deposits  fallout blocks, inner to outer
 * @param cell      grid cell size, one possible vent per cell
 * @param chance    chance for a cell to hold a vent
 * @param radius    fallout ring radius
 * @param minLatitude / maxLatitude  latitude band (degrees, signed)
 */
public class VentFeature extends Feature<VentFeature.Config> {
    public record Config(BlockState vent, List<BlockState> deposits, int cell, double chance, double radius, double minLatitude,
                         double maxLatitude, Optional<BlockState> pit) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("vent").forGetter(Config::vent),
                BlockState.CODEC.listOf().fieldOf("deposits").forGetter(Config::deposits),
                Codec.INT.optionalFieldOf("cell", 160).forGetter(Config::cell),
                Codec.DOUBLE.optionalFieldOf("chance", 0.3).forGetter(Config::chance),
                Codec.DOUBLE.optionalFieldOf("radius", 18.0).forGetter(Config::radius),
                Codec.DOUBLE.optionalFieldOf("min_latitude", -90.0).forGetter(Config::minLatitude),
                Codec.DOUBLE.optionalFieldOf("max_latitude", 90.0).forGetter(Config::maxLatitude),
                BlockState.CODEC.optionalFieldOf("pit").forGetter(Config::pit)
        ).apply(i, Config::new));
    }

    public VentFeature() {
        super(Config.CODEC);
    }

    /** Vent position of a grid cell as {x, z}, or null. */
    public static int[] vent(PlanetTerrain t, Config cfg, int gx, int gz) {
        long h = Columns.cell(t.seed(), 101, gx, gz);
        if (Columns.unit(h, 0) >= cfg.chance()) {
            return null;
        }
        int m = (int) Math.ceil(cfg.radius()) + 2;
        int x = gx * cfg.cell() + m + (int) (Columns.unit(h, 1) * Math.max(1, cfg.cell() - 2 * m));
        int z = gz * cfg.cell() + m + (int) (Columns.unit(h, 2) * Math.max(1, cfg.cell() - 2 * m));
        double lat = t.latitude(z);
        return lat < cfg.minLatitude() || lat > cfg.maxLatitude() ? null : new int[]{x, z};
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        PlanetTerrain t = Columns.terrain(ctx.chunkGenerator());
        if (t == null || ctx.config().deposits().isEmpty()) {
            return false;
        }
        Config cfg = ctx.config();
        WorldGenLevel level = ctx.level();
        int x0 = ctx.origin().getX() & ~15;
        int z0 = ctx.origin().getZ() & ~15;
        double reach = cfg.radius() * 1.3 + 2;
        SimplexNoise n = Columns.noise(t.seed(), 102);
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int gx = Mth.floor((x0 - reach) / cfg.cell()); gx <= Mth.floor((x0 + 16 + reach) / cfg.cell()); gx++) {
            for (int gz = Mth.floor((z0 - reach) / cfg.cell()); gz <= Mth.floor((z0 + 16 + reach) / cfg.cell()); gz++) {
                int[] v = vent(t, cfg, gx, gz);
                if (v == null) {
                    continue;
                }
                int layers = cfg.deposits().size();
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        int x = x0 + dx;
                        int z = z0 + dz;
                        double d = Math.hypot(x - v[0], z - v[1]);
                        double r = cfg.radius() * (0.8 + 0.35 * (n.getValue(x / 6.0, z / 6.0) * 0.5 + 0.5));
                        if (d > r) {
                            continue;
                        }
                        if (d < 1.6) {
                            // the vent sits in a small pit
                            Columns.shift(level, x, z, -1, cfg.pit().orElse(cfg.deposits().get(0)));
                            if (cfg.pit().isPresent()) {
                                Columns.paint(level, x, z, 1, cfg.pit().get());
                            }
                            if (x == v[0] && z == v[1]) {
                                level.setBlock(p.set(x, Columns.top(level, x, z), z), cfg.vent(), 2);
                            }
                            any = true;
                            continue;
                        }
                        double f = d / r;
                        int k = Math.min(layers - 1, (int) (f * layers));
                        // the outer ring is patchy
                        if (k == layers - 1 && layers > 1 && n.getValue(x / 3.0 + 50, z / 3.0) < 0.1) {
                            continue;
                        }
                        Columns.paint(level, x, z, 1, cfg.deposits().get(k));
                        any = true;
                    }
                }
            }
        }
        return any;
    }
}
