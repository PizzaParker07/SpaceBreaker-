package com.pizzaparker07.spacebreaker.content.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Picks biomes from the planet's terrain zones (highlands, lowlands, large craters, volcanic deposits, poles),
 * so biomes always match the ground they sit on. Bound to its {@link PlanetChunkGenerator}'s terrain.
 */
public class PlanetBiomeSource extends BiomeSource {
    public static final MapCodec<PlanetBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.CODEC.fieldOf("highlands").forGetter(s -> s.highlands),
            Biome.CODEC.optionalFieldOf("lowlands").forGetter(s -> s.lowlands),
            Biome.CODEC.optionalFieldOf("crater").forGetter(s -> s.crater),
            Biome.CODEC.optionalFieldOf("volcanic").forGetter(s -> s.volcanic),
            Biome.CODEC.optionalFieldOf("polar").forGetter(s -> s.polar)
    ).apply(i, i.stable(PlanetBiomeSource::new)));

    private final Holder<Biome> highlands;
    private final Optional<Holder<Biome>> lowlands;
    private final Optional<Holder<Biome>> crater;
    private final Optional<Holder<Biome>> volcanic;
    private final Optional<Holder<Biome>> polar;
    private volatile Supplier<PlanetTerrain> terrain;
    private final ConcurrentHashMap<Long, PlanetTerrain.Zone> zones = new ConcurrentHashMap<>();
    private volatile long zonesSeed;

    public PlanetBiomeSource(Holder<Biome> highlands, Optional<Holder<Biome>> lowlands, Optional<Holder<Biome>> crater,
                             Optional<Holder<Biome>> volcanic, Optional<Holder<Biome>> polar) {
        this.highlands = highlands;
        this.lowlands = lowlands;
        this.crater = crater;
        this.volcanic = volcanic;
        this.polar = polar;
    }

    void attach(Supplier<PlanetTerrain> terrain) {
        this.terrain = terrain;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(Optional.of(highlands), lowlands, crater, volcanic, polar).flatMap(Optional::stream);
    }

    public Holder<Biome> biome(PlanetTerrain.Zone zone) {
        return switch (zone) {
            case HIGHLANDS -> highlands;
            case LOWLANDS -> lowlands.orElse(highlands);
            case CRATER -> crater.orElse(highlands);
            case VOLCANIC -> volcanic.or(() -> lowlands).orElse(highlands);
            case POLAR -> polar.orElse(highlands);
        };
    }

    /** Zone at a quart (4×4 column) position, cached. */
    public PlanetTerrain.Zone zone(int quartX, int quartZ) {
        Supplier<PlanetTerrain> source = terrain;
        if (source == null) {
            return PlanetTerrain.Zone.HIGHLANDS;
        }
        PlanetTerrain t = source.get();
        if (t.seed() != zonesSeed) {
            zones.clear();
            zonesSeed = t.seed();
        }
        long key = ((long) quartX << 32) ^ (quartZ & 0xFFFFFFFFL);
        PlanetTerrain.Zone z = zones.get(key);
        if (z == null) {
            PlanetTerrain.Column c = t.column(QuartPos.toBlock(quartX) + 2, QuartPos.toBlock(quartZ) + 2);
            z = c.zone(t.profile());
            if (zones.size() > 65536) {
                zones.clear();
            }
            zones.put(key, z);
        }
        return z;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return biome(zone(x, z));
    }
}
