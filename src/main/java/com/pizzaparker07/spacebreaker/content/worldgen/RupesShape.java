package com.pizzaparker07.spacebreaker.content.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Shape of a giant fault cliff ({@link Landforms.Rupes}): along a straight, slightly wavering line the ground on one
 * side drops by a great height in a near-vertical wall that fades out toward the ends, with a talus slope at its foot
 * and bare layered rock on the face. Same maths as the earlier cliff feature, so cliffs keep their place.
 */
public final class RupesShape {
    private final Landforms.Rupes cfg;
    private final double centreZ;
    private final double cos;
    private final double sin;
    private final SimplexNoise waver;
    private final SimplexNoise layers;

    public RupesShape(Landforms.Rupes cfg, double poleDistance, long seed) {
        this.cfg = cfg;
        this.centreZ = -cfg.latitude() / 90.0 * poleDistance;
        double a = Math.toRadians(cfg.angleDeg());
        this.cos = Math.cos(a);
        this.sin = Math.sin(a);
        this.waver = noise(seed, 63);
        this.layers = noise(seed, 91);
    }

    /** The noise the cliff feature used (seed mix kept exactly, or the cliff line would move). */
    static SimplexNoise noise(long seed, int salt) {
        return new SimplexNoise(new XoroshiroRandomSource(seed ^ (0x0D7E8L * salt + salt)));
    }

    public Landforms.Rupes config() {
        return cfg;
    }

    /** Coordinates along (u) and across (v, + on the low side) the cliff line, with the line's waver applied. */
    public double[] local(double x, double z) {
        double dx = x - cfg.x();
        double dz = z - centreZ;
        double u = dx * cos + dz * sin;
        double v = -dx * sin + dz * cos;
        v += 14 * waver.getValue(u / 160.0, 2.1) + 3 * waver.getValue(u / 23.0, 7.7);
        return new double[]{u, v};
    }

    /** True if the column is near enough to the cliff to be affected at all (cheap early out). */
    public boolean near(double x, double z) {
        double dx = x - cfg.x();
        double dz = z - centreZ;
        double u = dx * cos + dz * sin;
        return Math.abs(u) <= cfg.length() / 2 + cfg.wall() + 2;
    }

    /** Height change: 0 on the high side, -height (tapering at the ends) on the low side, a talus at the foot. */
    public double offset(double[] uv) {
        double half = cfg.length() / 2;
        if (Math.abs(uv[0]) > half + cfg.wall()) {
            return 0;
        }
        double taper = smooth((half - Math.abs(uv[0])) / (cfg.length() * 0.2));
        double fall = smooth(uv[1] / cfg.wall());
        double talus = uv[1] > cfg.wall() ? Math.max(0, 1 - (uv[1] - cfg.wall()) / (cfg.wall() * 2.5)) * 0.18 : 0;
        return -cfg.height() * taper * (fall - talus * fall);
    }

    /** True on the cliff face (bare layered rock). */
    public boolean face(double[] uv) {
        return uv[1] > -2 && uv[1] < cfg.wall() * 1.2 && Math.abs(uv[0]) < cfg.length() / 2;
    }

    /**
     * Whether a block of the face (u along the cliff, y up) lies in a dark band: layers that swell, thin and drift up
     * and down along the cliff, broken by faults, with patches of slanting cross-bedded layers.
     */
    public boolean layer(double u, int y) {
        SimplexNoise n = layers;
        double drift = 7 * n.getValue(u / 70.0, 3.1) + 2.5 * n.getValue(u / 19.0, y / 13.0);
        double fault = Math.floor(u / 23.0 + 0.5 * n.getValue(u / 41.0, 7.7));
        double phase = y + drift + 4 * n.getValue(fault * 1.37, 11.3);
        if (n.getValue(u / 34.0 + 50, y / 26.0) > 0.45) {
            phase = y + u * 0.6 + drift;
        }
        double period = 9 + 3 * n.getValue(y / 40.0, 21.0);
        double thick = 0.3 + 0.15 * n.getValue(u / 27.0 + 9, y / 9.0);
        double f = phase / period;
        return f - Math.floor(f) < thick;
    }

    static double smooth(double t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
