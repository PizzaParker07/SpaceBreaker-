package com.pizzaparker07.spacebreaker.content.moon.dungeon;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.moon.MoonData;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;

/** Moon dungeons: structure types, piece types, and their data (structures and structure sets, via datagen). */
public final class MoonStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Spacebreaker.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<SurveyOutpostStructure>> SURVEY_OUTPOST =
            TYPES.register("survey_outpost", () -> () -> SurveyOutpostStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<LavaTubeStructure>> LAVA_TUBE =
            TYPES.register("lava_tube", () -> () -> LavaTubeStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<IceDrillStructure>> ICE_DRILL =
            TYPES.register("ice_drill_station", () -> () -> IceDrillStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ICE_DRILL_PIECE =
            PIECES.register("ice_drill_station", () -> IceDrillPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> OUTPOST_PIECE =
            PIECES.register("survey_outpost", () -> OutpostPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> LAVA_TUBE_PIECE =
            PIECES.register("lava_tube", () -> LavaTubePiece::new);

    public static final ResourceKey<Structure> OUTPOST_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("survey_outpost"));
    public static final ResourceKey<Structure> LAVA_TUBE_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("lava_tube"));
    public static final ResourceKey<Structure> ICE_DRILL_KEY = ResourceKey.create(Registries.STRUCTURE, Spacebreaker.id("ice_drill_station"));
    public static final ResourceKey<StructureSet> ICE_DRILL_SET = ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("ice_drill_stations"));
    public static final ResourceKey<StructureSet> OUTPOST_SET = ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("survey_outposts"));
    public static final ResourceKey<StructureSet> LAVA_TUBE_SET = ResourceKey.create(Registries.STRUCTURE_SET, Spacebreaker.id("lava_tubes"));

    private MoonStructures() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
    }

    public static void structures(BootstrapContext<Structure> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        ctx.register(OUTPOST_KEY, new SurveyOutpostStructure(new Structure.StructureSettings(
                HolderSet.direct(biomes.getOrThrow(MoonData.HIGHLANDS), biomes.getOrThrow(MoonData.MARE), biomes.getOrThrow(MoonData.VOLCANIC)),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE)));
        ctx.register(LAVA_TUBE_KEY, new LavaTubeStructure(new Structure.StructureSettings(
                HolderSet.direct(biomes.getOrThrow(MoonData.MARE), biomes.getOrThrow(MoonData.VOLCANIC)),
                Map.of(), GenerationStep.Decoration.UNDERGROUND_STRUCTURES, TerrainAdjustment.NONE)));
        ctx.register(ICE_DRILL_KEY, new IceDrillStructure(new Structure.StructureSettings(
                HolderSet.direct(biomes.getOrThrow(MoonData.POLAR), biomes.getOrThrow(MoonData.CRATER), biomes.getOrThrow(MoonData.HIGHLANDS)),
                Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE)));
    }

    public static void sets(BootstrapContext<StructureSet> ctx) {
        HolderGetter<Structure> structures = ctx.lookup(Registries.STRUCTURE);
        ctx.register(OUTPOST_SET, new StructureSet(structures.getOrThrow(OUTPOST_KEY),
                new RandomSpreadStructurePlacement(36, 14, RandomSpreadType.LINEAR, 0x5B0071)));
        ctx.register(LAVA_TUBE_SET, new StructureSet(structures.getOrThrow(LAVA_TUBE_KEY),
                new RandomSpreadStructurePlacement(26, 9, RandomSpreadType.LINEAR, 0x1A7A70)));
        // cold traps are rare, so candidates are tried often; most fail the shadow check
        ctx.register(ICE_DRILL_SET, new StructureSet(structures.getOrThrow(ICE_DRILL_KEY),
                new RandomSpreadStructurePlacement(6, 2, RandomSpreadType.LINEAR, 0x1CEC0E)));
    }
}
