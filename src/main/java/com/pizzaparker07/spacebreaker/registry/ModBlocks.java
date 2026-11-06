package com.pizzaparker07.spacebreaker.registry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.block.LunarRegolithBlock;
import com.pizzaparker07.spacebreaker.content.block.SolarPanelBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Spacebreaker.MOD_ID);

    // ---- Earth ores
    public static final DeferredBlock<DropExperienceBlock> BAUXITE_ORE = BLOCKS.registerBlock("bauxite_ore",
            p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(3.0F, 3.0F));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_BAUXITE_ORE = BLOCKS.registerBlock("deepslate_bauxite_ore",
            p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE));

    // ---- Moon
    public static final DeferredBlock<LunarRegolithBlock> LUNAR_REGOLITH = BLOCKS.registerBlock("lunar_regolith",
            LunarRegolithBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.6F).sound(SoundType.SAND));
    public static final DeferredBlock<Block> MARE_BASALT = BLOCKS.registerSimpleBlock("mare_basalt",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(1.8F, 6.0F).sound(SoundType.BASALT));
    public static final DeferredBlock<DropExperienceBlock> ILMENITE_ORE = BLOCKS.registerBlock("ilmenite_ore",
            p -> new DropExperienceBlock(UniformInt.of(1, 3), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(SoundType.BASALT));

    // ---- Storage
    public static final DeferredBlock<Block> RAW_BAUXITE_BLOCK = BLOCKS.registerSimpleBlock("raw_bauxite_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F));

    // ---- Machines
    public static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL = BLOCKS.registerBlock("solar_panel",
            SolarPanelBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(2.0F, 6.0F)
                    .sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());

    private ModBlocks() {
    }
}
