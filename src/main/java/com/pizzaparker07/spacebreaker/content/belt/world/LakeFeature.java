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

import java.util.Optional;

/**
 * Liquid lakes in shallow basins: hydrocarbon seas on Titan, lava lakes in Io's paterae. Lakes sit on a jittered grid;
 * each has a lobed outline, a flat surface just below the lowest ground on its shore (so the liquid never spills), a
 * bowl-shaped bed and an optional shore and bed block. Placed once per chunk.
 *
 * @param liquid      the liquid
 * @param bed         optional lake bed block
 * @param shore       optional block on the shore ring
 * @param cell        grid cell size; one possible lake per cell
 * @param chance      chance for a cell to hold a lake
 * @param minRadius   smallest lake radius
 * @param maxRadius   largest lake radius
 * @param depth       depth at the centre
 * @param latitude    lakes only poleward of this absolute latitude (0 = anywhere)
 */
public class LakeFeature extends Feature<LakeFeature.Config> {
    public record Config(BlockState liquid, Optional<BlockState> bed, Optional<BlockState> shore, int cell, double chance, double minRadius,
                         double maxRadius, double depth, double latitude) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("liquid").forGetter(Config::liquid),
                BlockState.CODEC.optionalFieldOf("bed").forGetter(Config::bed),
                BlockState.CODEC.optionalFieldOf("shore").forGetter(Config::shore),
                Codec.INT.optionalFieldOf("cell", 400).forGetter(Config::cell),
                Codec.DOUBLE.optionalFieldOf("chance", 0.4).forGetter(Config::chance),
                Codec.DOUBLE.optionalFieldOf("min_radius", 14.0).forGetter(Config::minRadius),
                Codec.DOUBLE.optionalFieldOf("max_radius", 60.0).forGetter(Config::maxRadius),
                Codec.DOUBLE.optionalFieldOf("depth", 6.0).forGetter(Config::depth),
                Codec.DOUBLE.optionalFieldOf("latitude", 0.0).forGetter(Config::latitude)
        ).apply(i, Config::new));
    }

    /** One lake: centre, radius and surface level. */
    public record Lake(double x, double z, double radius, int level, long seed) {
    }

    public LakeFeature() {
        super(Config.CODEC);
    }

    /** The lake of a grid cell, or null. */
    public static Lake lake(PlanetTerrain t, Config cfg, int gx, int gz) {
        long h = Columns.cell(t.seed(), 91, gx, gz);
        if (Columns.unit(h, 0) >= cfg.chance()) {
            return null;
        }
        double r = cfg.minRadius() + (cfg.maxRadius() - cfg.minRadius()) * Math.pow(Columns.unit(h, 3), 1.6);
        double margin = r * 1.4;
        double x = gx * cfg.cell() + margin + Columns.unit(h, 1) * Math.max(1, cfg.cell() - 2 * margin);
        double z = gz * cfg.cell() + margin + Columns.unit(h, 2) * Math.max(1, cfg.cell() - 2 * margin);
        if (Math.abs(t.latitude(z)) < cfg.latitude()) {
            return null;
        }
        // surface: just below the lowest point of the shore
        double low = Double.MAX_VALUE;
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6;
            low = Math.min(low, t.height(x + Math.cos(a) * r * 1.15, z + Math.sin(a) * r * 1.15));
        }
        low = Math.min(low, t.height(x, z) + cfg.depth() * 0.5);
        return new Lake(x, z, r, Mth.floor(low) - 1, h);
    }

    /** Normalized distance from a lake's centre with a lobed outline (1 = shore). */
    public static double distance(PlanetTerrain t, Lake l, double x, double z) {
        double dx = x - l.x();
        double dz = z - l.z();
        double a = Math.atan2(dz, dx);
        SimplexNoise n = Columns.noise(t.seed(), 92);
        double lobes = 1 + 0.22 * n.getValue(Math.cos(a) * 1.7 + l.seed() % 97, Math.sin(a) * 1.7)
                + 0.08 * n.getValue(Math.cos(a) * 6 - 3, Math.sin(a) * 6 + l.seed() % 89);
        return Math.sqrt(dx * dx + dz * dz) / (l.radius() * lobes);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        PlanetTerrain t = Columns.terrain(ctx.chunkGenerator());
        if (t == null) {
            return false;
        }
        Config cfg = ctx.config();
        WorldGenLevel level = ctx.level();
        int x0 = ctx.origin().getX() & ~15;
        int z0 = ctx.origin().getZ() & ~15;
        boolean any = false;
        int g0x = Mth.floor((x0 - cfg.maxRadius() * 1.5) / cfg.cell());
        int g1x = Mth.floor((x0 + 16 + cfg.maxRadius() * 1.5) / cfg.cell());
        int g0z = Mth.floor((z0 - cfg.maxRadius() * 1.5) / cfg.cell());
        int g1z = Mth.floor((z0 + 16 + cfg.maxRadius() * 1.5) / cfg.cell());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int gx = g0x; gx <= g1x; gx++) {
            for (int gz = g0z; gz <= g1z; gz++) {
                Lake l = lake(t, cfg, gx, gz);
                if (l == null || Math.abs(l.x() - (x0 + 8)) > l.radius() * 1.5 + 8 || Math.abs(l.z() - (z0 + 8)) > l.radius() * 1.5 + 8) {
                    continue;
                }
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        int x = x0 + dx;
                        int z = z0 + dz;
                        double s = distance(t, l, x + 0.5, z + 0.5);
                        if (s > 1.25) {
                            continue;
                        }
                        if (s >= 1) {
                            if (cfg.shore().isPresent()) {
                                Columns.paint(level, x, z, 1, cfg.shore().get());
                            }
                            continue;
                        }
                        int bottom = l.level() - Math.max(1, (int) Math.round(cfg.depth() * Math.sqrt(1 - s * s)));
                        int top = Columns.top(level, x, z);
                        if (top > bottom) {
                            Columns.shift(level, x, z, bottom - top, level.getBlockState(p.set(x, top - Columns.CARRY, z)));
                        }
                        int floor = Columns.top(level, x, z);
                        if (cfg.bed().isPresent()) {
                            Columns.paint(level, x, z, 1, cfg.bed().get());
                        }
                        for (int y = floor + 1; y <= l.level(); y++) {
                            level.setBlock(p.set(x, y, z), cfg.liquid(), 2);
                        }
                        any = true;
                    }
                }
            }
        }
        return any;
    }
}
