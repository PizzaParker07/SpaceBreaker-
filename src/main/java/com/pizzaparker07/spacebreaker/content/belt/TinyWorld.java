package com.pizzaparker07.spacebreaker.content.belt;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.IsruRecipes;
import com.pizzaparker07.spacebreaker.content.mars.BlockKit;
import com.pizzaparker07.spacebreaker.content.moon.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.worldgen.Landforms;
import com.pizzaparker07.spacebreaker.content.worldgen.TerrainProfile;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.List;
import java.util.Optional;

/**
 * Everything a tiny irregular moon (a few tens of km, like Nix, Hydra or Proteus) needs, so each one stays a small
 * package: a soil that keeps boot prints and a bedrock block, the dimension key, three biomes (plains, crater, polar),
 * a lumpy, crater-saturated terrain profile with very strong relief for the body's irregular shape, boulders, names,
 * loot and tags. A world adds only what is its own (an extra landform or feature, a patch of a second soil).
 */
public final class TinyWorld {
    public final String name;
    public final ResourceKey<Level> level;
    public final DeferredBlock<LunarRegolithBlock> soil;
    public final DeferredBlock<Block> rock;
    public final TagKey<Block> stone;
    public final ResourceKey<Biome> plains;
    public final ResourceKey<Biome> crater;
    public final ResourceKey<Biome> polar;
    private final boolean waterIce;

    /**
     * @param waterIce true if the bedrock is water ice (it then melts to water in the ice melter: a refuel world)
     */
    public TinyWorld(BlockKit kit, String name, String soilId, MapColor soilColor, String rockId, MapColor rockColor, boolean waterIce) {
        this.name = name;
        this.level = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id(name));
        this.soil = kit.block(soilId, LunarRegolithBlock::new,
                BlockBehaviour.Properties.of().mapColor(soilColor).strength(0.5F).sound(SoundType.SAND));
        this.rock = kit.rock(rockId, rockColor, SoundType.STONE, 1.6F);
        this.stone = SbTagKeys.modBlock(name + "_stone");
        this.plains = OuterWorlds.biome(name + "_plains");
        this.crater = OuterWorlds.biome(name + "_crater");
        this.polar = OuterWorlds.biome(name + "_polar");
        this.waterIce = waterIce;
    }

    public List<ResourceKey<Biome>> biomes() {
        return List.of(plains, crater, polar);
    }

    public ResourceKey<TerrainProfile> terrainKey() {
        return ResourceKey.create(TerrainProfile.REGISTRY, Spacebreaker.id(name));
    }

    /** A lumpy little body: strong relief standing in for its irregular shape, saturated with craters, frost at the poles. */
    public TerrainProfile profile(double radiusKm, double relief, Optional<BlockState> polarFrost, Landforms landforms) {
        BlockState s = soil.get().defaultBlockState();
        BlockState r = rock.get().defaultBlockState();
        var m = new SmallBodyTerrain.Materials(s, r, r, s, polarFrost, Optional.of(r));
        return SmallBodyTerrain.of(Spacebreaker.id(name), radiusKm).baseHeight(76).relief(relief).roughness(1.2).craters(1.3).age(1.0)
                .iceLatitude(82).materials(m).landforms(landforms).build();
    }

    public void translations(Lang lang, String en, String tr, String soilEn, String soilTr, String rockEn, String rockTr) {
        lang.add("dimension.spacebreaker." + name, en, tr);
        lang.block(soil, soilEn, soilTr);
        lang.block(rock, rockEn, rockTr);
        lang.add("biome.spacebreaker." + name + "_plains", en + " Surface", en + " Yüzeyi");
        lang.add("biome.spacebreaker." + name + "_crater", en + " Crater", en + " Krateri");
        lang.add("biome.spacebreaker." + name + "_polar", en + " Pole", en + " Kutbu");
    }

    public void loot(SbBlockLoot loot) {
        loot.self(soil.get());
        loot.self(rock.get());
    }

    public void tags(SbTags tags, TagKey<Block> ground) {
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, soil.get());
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, rock.get());
        tags.block(stone, rock.get());
        tags.block(ground, soil.get(), rock.get());
        if (waterIce) {
            tags.item(IsruRecipes.WATER_ICES, rock.get().asItem());
        }
    }

    /** Boulders of the bedrock with some of the accent block. */
    public void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx, BlockState accent) {
        configured(ctx, accent, 0.5F, 1.6F);
    }

    /** Boulders of a chosen size range (radius in blocks). */
    public void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx, BlockState accent, float min, float max) {
        ctx.register(OuterWorlds.cf(name + "_block"), OuterWorlds.boulder(min, max, 0.5F, rock.get().defaultBlockState(), 3, accent, 1));
    }

    public void placed(BootstrapContext<PlacedFeature> ctx) {
        placed(ctx, 3);
    }

    /** Boulders on average once every {@code rarity} chunks (1 = in every chunk). */
    public void placed(BootstrapContext<PlacedFeature> ctx, int rarity) {
        OuterWorlds.placedBoulder(ctx, name + "_block", rarity);
    }

    /**
     * The three biomes. {@code extra} are more (step, placed feature name) pairs placed before the boulders.
     */
    public void biomes(BootstrapContext<Biome> ctx, HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers, int grass,
                       Object... extra) {
        Object[] steps = new Object[extra.length + 2];
        System.arraycopy(extra, 0, steps, 0, extra.length);
        steps[extra.length] = GenerationStep.Decoration.LOCAL_MODIFICATIONS;
        steps[extra.length + 1] = name + "_block";
        for (ResourceKey<Biome> key : biomes()) {
            ctx.register(key, OuterWorlds.biome(pf, carvers, key, -2.0F, OuterWorlds.Look.airless(grass), steps));
        }
    }
}
