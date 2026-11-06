package com.pizzaparker07.spacebreaker.core.astro.galaxy;

import com.pizzaparker07.spacebreaker.core.astro.AstroConstants;
import com.pizzaparker07.spacebreaker.core.astro.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A deterministic, seed-based Milky Way. Space is cut into cubic sectors of {@link #SECTOR_SIZE} parsecs that
 * are generated lazily (and cached) the first time they are asked for: the same world seed always gives the
 * same stars in the same places. Real nearby stars are fixed entries at their measured positions; generated
 * stars that would land on top of one are dropped. Thread-safe.
 */
public final class Galaxy {
    /** Edge of a sector, pc (≈ 100 stars per sector near the Sun). */
    public static final double SECTOR_SIZE = 10.0;
    /** Generated stars closer than this to a fixed star are removed, pc. */
    public static final double FIXED_CLEARANCE = 0.5;
    /** Inside the real-star catalogue radius only real stars exist, pc. */
    public static final double SUN_CLEARANCE = NearbyStars.CATALOGUE_RADIUS_LY * AstroConstants.LIGHT_YEAR / AstroConstants.PARSEC;
    private static final int SECTOR_CACHE = 512;
    private static final int SYSTEM_CACHE = 256;

    public record SectorKey(int x, int y, int z) {
        public Vec3 min() {
            return new Vec3(x * SECTOR_SIZE, y * SECTOR_SIZE, z * SECTOR_SIZE);
        }
    }

    public record Sector(SectorKey key, List<StarSeed> stars) {
    }

    private final long seed;
    private final Map<SectorKey, Sector> sectors = lru(SECTOR_CACHE);
    private final Map<Long, StarSystem> systems = lru(SYSTEM_CACHE);
    private final List<StarSeed> fixed = new ArrayList<>();

    public Galaxy(long worldSeed) {
        this.seed = worldSeed;
        for (int i = 0; i < NearbyStars.ALL.size(); i++) {
            NearbyStars.Entry e = NearbyStars.ALL.get(i);
            Vec3 p = e.position();
            SectorKey k = sectorOf(p);
            fixed.add(new StarSeed(Rng.hash(worldSeed, 0x46495845L, i), k.x, k.y, k.z, -1 - i, p, e.mass(), e.ageYears(), e.key()));
        }
    }

    public long seed() {
        return seed;
    }

    public static SectorKey sectorOf(Vec3 pc) {
        return new SectorKey((int) Math.floor(pc.x() / SECTOR_SIZE), (int) Math.floor(pc.y() / SECTOR_SIZE),
                (int) Math.floor(pc.z() / SECTOR_SIZE));
    }

    /** The Sun as a star seed (index −1 in its sector). */
    public StarSeed sun() {
        return fixed.get(0);
    }

    public List<StarSeed> fixedStars() {
        return Collections.unmodifiableList(fixed);
    }

    public Optional<StarSeed> fixedStar(String key) {
        return fixed.stream().filter(s -> s.fixedKey().equals(key)).findFirst();
    }

    public Sector sector(int x, int y, int z) {
        SectorKey key = new SectorKey(x, y, z);
        synchronized (sectors) {
            Sector s = sectors.get(key);
            if (s == null) {
                s = generateSector(key);
                sectors.put(key, s);
            }
            return s;
        }
    }

    public Sector sector(SectorKey k) {
        return sector(k.x, k.y, k.z);
    }

    private Sector generateSector(SectorKey key) {
        Rng rng = new Rng(Rng.hash(seed, key.x, key.y, key.z));
        Vec3 min = key.min();
        int n = rng.poisson(GalaxyModel.expectedStars(min, SECTOR_SIZE));
        List<StarSeed> fixedHere = new ArrayList<>();
        for (StarSeed f : fixed) {
            if (f.sx() == key.x && f.sy() == key.y && f.sz() == key.z) {
                fixedHere.add(f);
            }
        }
        List<StarSeed> out = new ArrayList<>(fixedHere);
        double maxDensity = 0;
        for (int c = 0; c < 8; c++) {
            maxDensity = Math.max(maxDensity, GalaxyModel.density(min.x() + (c & 1) * SECTOR_SIZE,
                    min.y() + (c >> 1 & 1) * SECTOR_SIZE, min.z() + (c >> 2 & 1) * SECTOR_SIZE));
        }
        for (int i = 0; i < n; i++) {
            // positions follow the density gradient inside the sector (rejection sampling, bounded tries)
            Vec3 p = null;
            for (int t = 0; t < 8; t++) {
                Vec3 q = new Vec3(min.x() + rng.nextDouble() * SECTOR_SIZE, min.y() + rng.nextDouble() * SECTOR_SIZE,
                        min.z() + rng.nextDouble() * SECTOR_SIZE);
                p = q;
                if (rng.nextDouble() * maxDensity <= GalaxyModel.density(q)) {
                    break;
                }
            }
            double mass = StarSystemGenerator.sampleMass(rng);
            double age = rng.range(1e8, 1e10);
            long id = Rng.hash(seed, key.x, key.y, key.z, i);
            boolean clash = p.distance(GalaxyModel.SUN) < SUN_CLEARANCE;
            for (StarSeed f : fixedHere) {
                clash |= f.position().distance(p) < FIXED_CLEARANCE;
            }
            if (!clash) {
                out.add(new StarSeed(id, key.x, key.y, key.z, i, p, mass, age, null));
            }
        }
        return new Sector(key, List.copyOf(out));
    }

    /** A star by its address; empty if that sector has no such index. */
    public Optional<StarSeed> star(int sx, int sy, int sz, int index) {
        return sector(sx, sy, sz).stars().stream().filter(s -> s.index() == index).findFirst();
    }

    /** Every star within {@code radius} pc of {@code center}. */
    public List<StarSeed> starsWithin(Vec3 center, double radius) {
        SectorKey lo = sectorOf(center.sub(new Vec3(radius, radius, radius)));
        SectorKey hi = sectorOf(center.add(new Vec3(radius, radius, radius)));
        List<StarSeed> out = new ArrayList<>();
        for (int x = lo.x; x <= hi.x; x++) {
            for (int y = lo.y; y <= hi.y; y++) {
                for (int z = lo.z; z <= hi.z; z++) {
                    for (StarSeed s : sector(x, y, z).stars()) {
                        if (s.position().distance(center) <= radius) {
                            out.add(s);
                        }
                    }
                }
            }
        }
        return out;
    }

    /** The full system of a star (generated on demand, cached). Empty for the Sun, whose system is the data registry. */
    public Optional<StarSystem> system(StarSeed s) {
        if (s.fixed()) {
            synchronized (systems) {
                StarSystem cached = systems.get(s.id());
                if (cached != null) {
                    return Optional.of(cached);
                }
            }
            Optional<StarSystem> sys = NearbyStars.system(s);
            sys.ifPresent(v -> {
                synchronized (systems) {
                    systems.put(s.id(), v);
                }
            });
            return sys;
        }
        synchronized (systems) {
            return Optional.of(systems.computeIfAbsent(s.id(), k -> StarSystemGenerator.generate(s)));
        }
    }

    private static <K, V> Map<K, V> lru(int max) {
        return new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > max;
            }
        };
    }
}
