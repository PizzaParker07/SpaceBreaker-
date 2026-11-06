package com.pizzaparker07.spacebreaker.core.astro.galaxy;

/**
 * Small deterministic random generator (SplitMix64). Same seed, same numbers on every machine and Java
 * version, which keeps generated galaxies identical for a world seed.
 */
public final class Rng {
    private long state;

    public Rng(long seed) {
        this.state = seed;
    }

    /** Mixes any number of values into one well-distributed seed. */
    public static long hash(long... values) {
        long h = 0x9E3779B97F4A7C15L;
        for (long v : values) {
            h = mix(h ^ mix(v + 0x632BE59BD9B4E019L));
        }
        return h;
    }

    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public long nextLong() {
        state += 0x9E3779B97F4A7C15L;
        return mix(state);
    }

    /** Uniform in [0, 1). */
    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    public double range(double lo, double hi) {
        return lo + (hi - lo) * nextDouble();
    }

    /** Log-uniform between lo and hi (both &gt; 0). */
    public double logRange(double lo, double hi) {
        return lo * Math.pow(hi / lo, nextDouble());
    }

    public int nextInt(int bound) {
        return (int) Math.floorMod(nextLong(), (long) bound);
    }

    public boolean chance(double p) {
        return nextDouble() < p;
    }

    public double gaussian() {
        double u = 1 - nextDouble();
        double v = nextDouble();
        return Math.sqrt(-2 * Math.log(u)) * Math.cos(2 * Math.PI * v);
    }

    /** Rayleigh-distributed value with scale sigma (orbital eccentricities). */
    public double rayleigh(double sigma) {
        return sigma * Math.sqrt(-2 * Math.log(1 - nextDouble()));
    }

    /** Poisson-distributed count (Knuth for small means, normal approximation above 50). */
    public int poisson(double mean) {
        if (mean <= 0) {
            return 0;
        }
        if (mean > 50) {
            return Math.max(0, (int) Math.round(mean + Math.sqrt(mean) * gaussian()));
        }
        double l = Math.exp(-mean), p = 1;
        int k = 0;
        do {
            k++;
            p *= nextDouble();
        } while (p > l);
        return k - 1;
    }
}
