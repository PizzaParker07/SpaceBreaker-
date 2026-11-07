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

    /** Yellow-and-black hazard stripes, with a slab for floor edges. */
    public static final BlockKit.Family HAZARD_STRIPE = KIT.family("hazard_stripe", MapColor.COLOR_YELLOW, SoundType.METAL, 3.0F, false);
    /** Flush LED ceiling and wall panel, full light. */
    public static final DeferredBlock<Block> LED_PANEL = KIT.block("led_panel", metal(MapColor.SNOW).lightLevel(s -> 15));
    /** A floodlight on any face. */
    public static final DeferredBlock<FloodlightBlock> FLOODLIGHT = KIT.block("floodlight", FloodlightBlock::new,
            metal(MapColor.METAL).noOcclusion().lightLevel(s -> 15));
    /** Wall covers that hide cable runs and pipe runs (full blocks: they hold air). */
    public static final DeferredBlock<Block> CABLE_DUCT = KIT.block("cable_duct", metal(MapColor.COLOR_GRAY));
    public static final DeferredBlock<Block> PIPE_DUCT = KIT.block("pipe_duct", metal(MapColor.COLOR_GRAY));
    /** A steel ladder. */
    public static final DeferredBlock<net.minecraft.world.level.block.LadderBlock> STEEL_LADDER = KIT.block("steel_ladder",
            net.minecraft.world.level.block.LadderBlock::new, metal(MapColor.METAL).noOcclusion());
    /** Safety railing that joins up like iron bars. */
    public static final DeferredBlock<net.minecraft.world.level.block.IronBarsBlock> RAILING = KIT.block("railing",
            net.minecraft.world.level.block.IronBarsBlock::new, metal(MapColor.COLOR_YELLOW).noOcclusion());
    /** Half-height grate platform for catwalks. */
    public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> GRATE_PLATFORM = KIT.block("grate_platform",
            net.minecraft.world.level.block.SlabBlock::new, metal(MapColor.METAL).noOcclusion().isViewBlocking((s, l, p) -> false));

    /** Two-high sliding door: holds air when shut. */
    public static final DeferredBlock<SlidingDoorBlock> SLIDING_DOOR = KIT.blocks.registerBlock("sliding_door", SlidingDoorBlock::new,
            metal(MapColor.METAL).noOcclusion());
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.DoubleHighBlockItem> SLIDING_DOOR_ITEM =
            KIT.items.register("sliding_door", () -> new net.minecraft.world.item.DoubleHighBlockItem(SLIDING_DOOR.get(), new net.minecraft.world.item.Item.Properties()));

    static {
        KIT.tab.add(SLIDING_DOOR_ITEM);
    }

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
        out.add(SLIDING_DOOR.get());
        for (var b : java.util.List.of(LED_PANEL, FLOODLIGHT, CABLE_DUCT, PIPE_DUCT, STEEL_LADDER, RAILING, GRATE_PLATFORM)) {
            out.add(b.get());
        }
        return out.toArray(Block[]::new);
    }
}
