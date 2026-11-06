package com.pizzaparker07.spacebreaker.content.mars;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registration helper shared by the inner-planet modules (Mars, Phobos, Deimos, Venus, Mercury): block + item
 * pairs in creative-tab order, rock families with stairs, slabs and walls, and their names and loot.
 */
public final class BlockKit {
    public final DeferredRegister.Blocks blocks = DeferredRegister.createBlocks(Spacebreaker.MOD_ID);
    public final DeferredRegister.Items items = DeferredRegister.createItems(Spacebreaker.MOD_ID);
    /** Block items and items in creative-tab order. */
    public final List<Supplier<? extends Item>> tab = new ArrayList<>();
    private final List<Family> families = new ArrayList<>();

    /** A rock with stairs, slab and optional wall, like vanilla's stone families. */
    public record Family(DeferredBlock<Block> base, DeferredBlock<StairBlock> stairs, DeferredBlock<SlabBlock> slab,
                         DeferredBlock<WallBlock> wall) {
        public List<DeferredBlock<? extends Block>> all() {
            List<DeferredBlock<? extends Block>> l = new ArrayList<>(List.of(base, stairs, slab));
            if (wall != null) {
                l.add(wall);
            }
            return l;
        }
    }

    public static BlockBehaviour.Properties rockProps(MapColor color, SoundType sound, float hardness) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(hardness, 6.0F).sound(sound);
    }

    public <B extends Block> DeferredBlock<B> block(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties props) {
        DeferredBlock<B> b = blocks.registerBlock(name, factory, props);
        DeferredItem<BlockItem> item = items.registerSimpleBlockItem(b);
        tab.add(item);
        return b;
    }

    public DeferredBlock<Block> block(String name, BlockBehaviour.Properties props) {
        return block(name, Block::new, props);
    }

    public DeferredBlock<Block> rock(String name, MapColor color, SoundType sound, float hardness) {
        return block(name, rockProps(color, sound, hardness));
    }

    public DeferredItem<Item> item(String name) {
        DeferredItem<Item> item = items.registerSimpleItem(name);
        tab.add(item);
        return item;
    }

    public <I extends Item> DeferredItem<I> item(String name, Supplier<I> factory) {
        DeferredItem<I> item = items.register(name, factory);
        tab.add(item);
        return item;
    }

    /** A rock family; for brick families pass the plural block name ("x_bricks"), shapes use the singular stem. */
    public Family family(String name, MapColor color, SoundType sound, float hardness, boolean wall) {
        String stem = name.endsWith("_bricks") ? name.substring(0, name.length() - 1) : name;
        DeferredBlock<Block> base = rock(name, color, sound, hardness);
        DeferredBlock<StairBlock> stairs = block(stem + "_stairs", p -> new StairBlock(base.get().defaultBlockState(), p),
                rockProps(color, sound, hardness));
        DeferredBlock<SlabBlock> slab = block(stem + "_slab", SlabBlock::new, rockProps(color, sound, hardness));
        DeferredBlock<WallBlock> w = wall ? block(stem + "_wall", WallBlock::new, rockProps(color, sound, hardness).forceSolidOn()) : null;
        Family f = new Family(base, stairs, slab, w);
        families.add(f);
        return f;
    }

    public List<Family> families() {
        return families;
    }

    /** Stairs/slab/wall loot for every family (the base block is up to the caller). */
    public void familyLoot(SbBlockLoot loot) {
        for (Family f : families) {
            loot.self(f.base().get());
            loot.self(f.stairs().get());
            loot.slab(f.slab().get());
            if (f.wall() != null) {
                loot.self(f.wall().get());
            }
        }
    }

    /**
     * Names a family. For brick families give the singular stem ("Martian Basalt Brick"): the base block becomes plural
     * like vanilla's "Stone Bricks" / "Stone Brick Stairs".
     */
    public static void familyNames(Lang lang, Family f, String en, String tr) {
        boolean bricks = f.base().getId().getPath().endsWith("_bricks");
        lang.block(f.base(), bricks ? en + "s" : en, tr);
        lang.block(f.stairs(), en + " Stairs", tr + " Merdiven");
        lang.block(f.slab(), en + " Slab", tr + " Basamak");
        if (f.wall() != null) {
            lang.block(f.wall(), en + " Wall", tr + " Duvar");
        }
    }
}
