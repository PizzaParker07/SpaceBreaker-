package com.pizzaparker07.spacebreaker.content.earth;

import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * One metal and everything made from it: optional ore family (stone + deepslate ore, raw item, raw block),
 * ingot, nugget, storage block, plate, dust, wire and rod. Vanilla metals (iron, copper, gold) only get the
 * parts vanilla lacks and point at the vanilla ingot.
 */
public final class Metal {
    public final String name;
    public final String en;
    public final String tr;
    @Nullable public final String oreName;
    @Nullable public final String oreEn;
    @Nullable public final String oreTr;
    /** Whether raw ore / dust can be smelted in a vanilla furnace (titanium and tungsten need the arc furnace). */
    public final boolean furnace;

    @Nullable public DeferredBlock<DropExperienceBlock> ore;
    @Nullable public DeferredBlock<DropExperienceBlock> deepslateOre;
    @Nullable public DeferredBlock<Block> rawBlock;
    @Nullable public DeferredBlock<Block> block;
    @Nullable public DeferredItem<BlockItem> oreItem;
    @Nullable public DeferredItem<BlockItem> deepslateOreItem;
    @Nullable public DeferredItem<BlockItem> rawBlockItem;
    @Nullable public DeferredItem<BlockItem> blockItem;
    @Nullable public DeferredItem<Item> raw;
    @Nullable public DeferredItem<Item> nugget;
    @Nullable public DeferredItem<Item> plate;
    @Nullable public DeferredItem<Item> dust;
    @Nullable public DeferredItem<Item> wire;
    @Nullable public DeferredItem<Item> rod;
    public Supplier<? extends Item> ingot;

    public final TagKey<Item> ingotTag;
    public final TagKey<Item> nuggetTag;
    public final TagKey<Item> plateTag;
    public final TagKey<Item> dustTag;
    public final TagKey<Item> wireTag;
    public final TagKey<Item> rodTag;
    public final TagKey<Item> blockItemTag;
    public final TagKey<Block> blockTag;
    @Nullable public final TagKey<Block> oreBlockTag;
    @Nullable public final TagKey<Item> oreItemTag;
    @Nullable public final TagKey<Item> rawTag;
    @Nullable public final TagKey<Block> rawBlockTag;
    @Nullable public final TagKey<Item> rawBlockItemTag;

    private Metal(Builder b) {
        this.name = b.name;
        this.en = b.en;
        this.tr = b.tr;
        this.oreName = b.oreName;
        this.oreEn = b.oreEn;
        this.oreTr = b.oreTr;
        this.furnace = b.furnace;
        ingotTag = SbTagKeys.commonItem("ingots/" + name);
        nuggetTag = SbTagKeys.commonItem("nuggets/" + name);
        plateTag = SbTagKeys.commonItem("plates/" + name);
        dustTag = SbTagKeys.commonItem("dusts/" + name);
        wireTag = SbTagKeys.commonItem("wires/" + name);
        rodTag = SbTagKeys.commonItem("rods/" + name);
        blockItemTag = SbTagKeys.commonItem("storage_blocks/" + name);
        blockTag = SbTagKeys.commonBlock("storage_blocks/" + name);
        if (oreName != null) {
            oreBlockTag = SbTagKeys.commonBlock("ores/" + oreName);
            oreItemTag = SbTagKeys.commonItem("ores/" + oreName);
            rawTag = SbTagKeys.commonItem("raw_materials/" + oreName);
            rawBlockTag = SbTagKeys.commonBlock("storage_blocks/raw_" + oreName);
            rawBlockItemTag = SbTagKeys.commonItem("storage_blocks/raw_" + oreName);
        } else {
            oreBlockTag = null;
            oreItemTag = null;
            rawTag = null;
            rawBlockTag = null;
            rawBlockItemTag = null;
        }
    }

    public boolean hasOre() {
        return ore != null;
    }

    public boolean isVanilla() {
        return block == null && nugget == null;
    }

    public ItemLike ingotItem() {
        return ingot.get();
    }

    /** All items of this metal in creative-tab order. */
    public List<Supplier<? extends Item>> items() {
        List<Supplier<? extends Item>> list = new ArrayList<>();
        for (Supplier<? extends Item> s : new Supplier[]{oreItem, deepslateOreItem, raw, rawBlockItem}) {
            if (s != null) {
                list.add(s);
            }
        }
        if (!isVanilla()) {
            list.add(ingot);
        }
        for (Supplier<? extends Item> s : new Supplier[]{nugget, blockItem, plate, dust, wire, rod}) {
            if (s != null) {
                list.add(s);
            }
        }
        return list;
    }

    public static Builder builder(String name, String en, String tr) {
        return new Builder(name, en, tr);
    }

    public static final class Builder {
        private final String name;
        private final String en;
        private final String tr;
        private String oreName;
        private String oreEn;
        private String oreTr;
        private MapColor rawColor = MapColor.STONE;
        private MapColor blockColor = MapColor.METAL;
        private int oreXpMax = 2;
        private boolean furnace = true;
        private boolean nuggetAndBlock = true;
        private boolean plate = true;
        private boolean dust = true;
        private boolean wire;
        private boolean rod;
        private Supplier<? extends Item> vanillaIngot;

        private Builder(String name, String en, String tr) {
            this.name = name;
            this.en = en;
            this.tr = tr;
        }

        public Builder ore(String oreName, String oreEn, String oreTr, MapColor rawColor, int xpMax) {
            this.oreName = oreName;
            this.oreEn = oreEn;
            this.oreTr = oreTr;
            this.rawColor = rawColor;
            this.oreXpMax = xpMax;
            return this;
        }

        public Builder color(MapColor color) {
            this.blockColor = color;
            return this;
        }

        public Builder arcFurnaceOnly() {
            this.furnace = false;
            return this;
        }

        public Builder wire() {
            this.wire = true;
            return this;
        }

        public Builder rod() {
            this.rod = true;
            return this;
        }

        public Builder noDust() {
            this.dust = false;
            return this;
        }

        public Builder noPlate() {
            this.plate = false;
            return this;
        }

        /** Vanilla metal: only plate/dust/wire/rod are added, the ingot is the vanilla one. */
        public Builder vanilla(Supplier<? extends Item> ingot) {
            this.vanillaIngot = ingot;
            this.nuggetAndBlock = false;
            return this;
        }

        public Metal build(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
            Metal m = new Metal(this);
            if (oreName != null) {
                int xp = oreXpMax;
                m.ore = blocks.registerBlock(oreName + "_ore", p -> new DropExperienceBlock(UniformInt.of(0, xp), p),
                        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                                .requiresCorrectToolForDrops().strength(3.0F, 3.0F));
                m.deepslateOre = blocks.registerBlock("deepslate_" + oreName + "_ore", p -> new DropExperienceBlock(UniformInt.of(0, xp), p),
                        BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
                                .requiresCorrectToolForDrops().strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE));
                m.rawBlock = blocks.registerSimpleBlock("raw_" + oreName + "_block",
                        BlockBehaviour.Properties.of().mapColor(rawColor).instrument(NoteBlockInstrument.BASEDRUM)
                                .requiresCorrectToolForDrops().strength(5.0F, 6.0F));
                m.oreItem = items.registerSimpleBlockItem(m.ore);
                m.deepslateOreItem = items.registerSimpleBlockItem(m.deepslateOre);
                m.raw = items.registerSimpleItem("raw_" + oreName);
                m.rawBlockItem = items.registerSimpleBlockItem(m.rawBlock);
            }
            if (vanillaIngot != null) {
                m.ingot = vanillaIngot;
            } else {
                m.ingot = items.registerSimpleItem(name + "_ingot");
            }
            if (nuggetAndBlock) {
                m.nugget = items.registerSimpleItem(name + "_nugget");
                m.block = blocks.registerSimpleBlock(name + "_block", BlockBehaviour.Properties.of().mapColor(blockColor)
                        .instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL));
                m.blockItem = items.registerSimpleBlockItem(m.block);
            }
            if (plate) {
                m.plate = items.registerSimpleItem(name + "_plate");
            }
            if (dust) {
                m.dust = items.registerSimpleItem(name + "_dust");
            }
            if (wire) {
                m.wire = items.registerSimpleItem(name + "_wire");
            }
            if (rod) {
                m.rod = items.registerSimpleItem(name + "_rod");
            }
            return m;
        }
    }
}
