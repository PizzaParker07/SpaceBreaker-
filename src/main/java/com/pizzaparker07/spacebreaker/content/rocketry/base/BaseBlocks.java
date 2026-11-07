package com.pizzaparker07.spacebreaker.content.rocketry.base;

import com.pizzaparker07.spacebreaker.content.mars.BlockKit;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Building blocks for bases and stations. Full blocks (panels, plates, pressure glass, portholes) hold air for the
 * oxygen distributor; the grate floor lets it through.
 */
public final class BaseBlocks {
    public static final BlockKit KIT = new BlockKit();

    private static BlockBehaviour.Properties metal(MapColor colour) {
        return BlockKit.rockProps(colour, SoundType.METAL, 3.0F);
    }

    private static BlockBehaviour.Properties glass() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(2.0F, 8.0F).sound(SoundType.GLASS)
                .noOcclusion().isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false).requiresCorrectToolForDrops();
    }

    /** Brushed aluminium wall panels, with stairs, slab and wall. */
    public static final BlockKit.Family ALUMINUM_PANEL = KIT.family("aluminum_panel", MapColor.METAL, SoundType.METAL, 3.0F, true);
    /** Heavy titanium deck plate, with stairs and slab. */
    public static final BlockKit.Family TITANIUM_DECK = KIT.family("titanium_deck", MapColor.COLOR_LIGHT_GRAY, SoundType.METAL, 4.0F, false);
    /** Thick pressure glass: see through, air stays in. */
    public static final DeferredBlock<TransparentBlock> PRESSURE_GLASS = KIT.block("pressure_glass", TransparentBlock::new, glass());
    /** A round porthole in a hull plate: a window that holds air. */
    public static final DeferredBlock<TransparentBlock> PORTHOLE = KIT.block("porthole", TransparentBlock::new, glass().sound(SoundType.METAL));
    /** Open grating for floors and catwalks: you see (and air flows) through it. */
    public static final DeferredBlock<TransparentBlock> GRATE = KIT.block("grate", TransparentBlock::new,
            metal(MapColor.METAL).noOcclusion().isViewBlocking((s, l, p) -> false));

    private BaseBlocks() {
    }

    /** Touches the class so the registers are filled before they are added to the bus. */
    public static void init() {
    }

    public static Block[] all() {
        java.util.List<Block> out = new java.util.ArrayList<>();
        for (BlockKit.Family f : KIT.families()) {
            f.all().forEach(b -> out.add(b.get()));
        }
        out.add(PRESSURE_GLASS.get());
        out.add(PORTHOLE.get());
        out.add(GRATE.get());
        return out.toArray(Block[]::new);
    }
}
