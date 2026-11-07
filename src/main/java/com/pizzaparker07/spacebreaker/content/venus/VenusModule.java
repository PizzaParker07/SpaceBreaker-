package com.pizzaparker07.spacebreaker.content.venus;

import com.mojang.serialization.Codec;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.mars.BlockKit;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Venus: a furnace surface (464 °C, 92 bar) of basalt plains, folded tesserae and shield volcanoes under a yellow
 * haze, and the habitable cloud deck about 50 km up, where air pressure and temperature are Earth-like, sulfuric-acid
 * clouds corrode unprotected gear, and bases float on light composites.
 */
public final class VenusModule implements SbModule {
    public static final BlockKit KIT = new BlockKit();
    public static final ResourceKey<Level> VENUS = ResourceKey.create(Registries.DIMENSION, Spacebreaker.id("venus"));
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Spacebreaker.MOD_ID);

    // ------------------------------------------------------------------ surface

    public static final DeferredBlock<Block> VENUS_BASALT = KIT.rock("venus_basalt", MapColor.COLOR_BLACK, SoundType.BASALT, 1.8F);
    public static final DeferredBlock<Block> VENUS_SOIL = KIT.block("venus_soil", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
            .instrument(NoteBlockInstrument.SNARE).strength(0.7F).sound(SoundType.GRAVEL));
    public static final DeferredBlock<Block> TESSERA_ROCK = KIT.rock("tessera_rock", MapColor.TERRACOTTA_GRAY, SoundType.DEEPSLATE, 2.2F);
    public static final DeferredBlock<DropExperienceBlock> SULFUR_CRUST = KIT.block("sulfur_crust", p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(1.2F, 4.0F).sound(SoundType.NETHER_ORE));
    public static final DeferredBlock<Block> METAL_FROST = KIT.block("metal_frost", BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .instrument(NoteBlockInstrument.BELL).requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.AMETHYST));

    // ------------------------------------------------------------------ cloud-deck building

    public static final DeferredBlock<Block> COMPOSITE_PANEL = KIT.block("composite_panel", BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ)
            .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.5F, 8.0F).sound(SoundType.COPPER_GRATE));
    public static final DeferredBlock<TransparentBlock> AEROGEL = KIT.block("aerogel", TransparentBlock::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.ICE).strength(0.4F).sound(SoundType.GLASS).noOcclusion()
            .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
            .isViewBlocking((s, l, p) -> false));
    public static final DeferredBlock<Block> ENVELOPE_FABRIC = KIT.block("envelope_fabric", BlockBehaviour.Properties.of().mapColor(MapColor.WOOL)
            .instrument(NoteBlockInstrument.GUITAR).strength(0.8F).sound(SoundType.WOOL).ignitedByLava());

    public static final DeferredItem<Item> SULFUR = KIT.item("sulfur");
    public static final DeferredItem<Item> ACID_COATING = KIT.item("acid_coating");
    public static final DeferredItem<LiftGasSacItem> LIFT_GAS_SAC = KIT.item("lift_gas_sac", () -> new LiftGasSacItem(new Item.Properties().stacksTo(16)));

    /** Armour with this component resists the acid clouds. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ACID_COATED =
            COMPONENTS.registerComponentType("acid_coated", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<AcidCoatingRecipe>> ACID_COATING_RECIPE =
            RECIPES.register("acid_coating", () -> new SimpleCraftingRecipeSerializer<>(AcidCoatingRecipe::new));

    public static final TagKey<Block> ORES_SULFUR = SbTagKeys.commonBlock("ores/sulfur");
    public static final TagKey<Block> VENUS_STONE = SbTagKeys.modBlock("venus_stone");

    @Override
    public String id() {
        return "venus";
    }

    @Override
    public void register(IEventBus modBus) {
        com.pizzaparker07.spacebreaker.content.venus.entity.VenusEntities.register(modBus, KIT.items);
        com.pizzaparker07.spacebreaker.content.venus.dungeon.VenusStructures.register(modBus);
        KIT.blocks.register(modBus);
        KIT.items.register(modBus);
        COMPONENTS.register(modBus);
        RECIPES.register(modBus);
        KIT.tab.forEach(SbTabs.PLANETS::add);
        com.pizzaparker07.spacebreaker.content.venus.entity.VenusEntities.eggs().forEach(SbTabs.PLANETS::add);
        VenusData.register();
        VenusClimate.register();
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, e -> VenusClimate.acid(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(ItemTooltipEvent.class, e -> {
            if (Boolean.TRUE.equals(e.getItemStack().get(ACID_COATED.get()))) {
                e.getToolTip().add(Component.translatable("tooltip.spacebreaker.acid_coated").withStyle(ChatFormatting.GOLD));
            }
        });
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.add("body.spacebreaker.venus", "Venus", "Venüs");
        lang.add("dimension.spacebreaker.venus", "Venus", "Venüs");
        lang.add("dimension.spacebreaker.venus_orbit", "Venus Orbit", "Venüs Yörüngesi");
        lang.block(VENUS_BASALT, "Venusian Basalt", "Venüs Bazaltı");
        lang.block(VENUS_SOIL, "Scorched Gravel", "Kavrulmuş Çakıl");
        lang.block(TESSERA_ROCK, "Tessera Rock", "Tessera Kayası");
        lang.block(SULFUR_CRUST, "Sulfur Crust", "Kükürt Kabuğu");
        lang.block(METAL_FROST, "Metal Frost", "Metal Kırağısı");
        lang.block(COMPOSITE_PANEL, "Light Composite Panel", "Hafif Kompozit Panel");
        lang.block(AEROGEL, "Aerogel", "Aerojel");
        lang.block(ENVELOPE_FABRIC, "Envelope Fabric", "Balon Kumaşı");
        lang.item(SULFUR, "Sulfur", "Kükürt");
        lang.item(LIFT_GAS_SAC, "Lift Gas Sac", "Kaldırma Gazı Kesesi");
        String[][] valve = {{"GONDOLA SEALED", "GONDOL KAPALI"}, {"open the gas", "kükürt sarısı"}, {"valve under the", "işaretin altındaki"}, {"sulfur mark", "vanayı aç"}};
        for (int i = 0; i < 4; i++) {
            lang.add("sign.spacebreaker.aerostat_valve." + i, valve[i][0], valve[i][1]);
        }
        lang.add("item.spacebreaker.lift_gas_sac.desc", "Use to sink slowly for 30 seconds", "Kullanınca 30 saniye yavaşça alçalırsın");
        lang.item(ACID_COATING, "Fluoropolymer Coating", "Floropolimer Kaplama");
        lang.add("tooltip.spacebreaker.acid_coated", "Acid-resistant coating", "Aside dayanıklı kaplama");
        lang.add("message.spacebreaker.acid_clouds", "The acid clouds are eating into your gear!", "Asit bulutları ekipmanını aşındırıyor!");
        VenusData.translations(lang);
        com.pizzaparker07.spacebreaker.content.venus.entity.VenusEntities.translations(lang);
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        for (DeferredBlock<? extends Block> b : List.of(VENUS_BASALT, VENUS_SOIL, TESSERA_ROCK, METAL_FROST, COMPOSITE_PANEL, ENVELOPE_FABRIC)) {
            loot.self(b.get());
        }
        loot.ore(SULFUR_CRUST.get(), SULFUR.get());
        loot.self(AEROGEL.get());
    }

    @Override
    public void addTags(SbTags tags) {
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, VENUS_BASALT.get(), TESSERA_ROCK.get(), SULFUR_CRUST.get(), METAL_FROST.get(), COMPOSITE_PANEL.get());
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, VENUS_SOIL.get());
        tags.block(BlockTags.WOOL, ENVELOPE_FABRIC.get());
        tags.block(BlockTags.NEEDS_STONE_TOOL, METAL_FROST.get(), COMPOSITE_PANEL.get());
        tags.block(VENUS_STONE, VENUS_BASALT.get(), TESSERA_ROCK.get());
        tags.block(ORES_SULFUR, SULFUR_CRUST.get());
        tags.blockTag(net.neoforged.neoforge.common.Tags.Blocks.ORES, ORES_SULFUR);
    }
}
