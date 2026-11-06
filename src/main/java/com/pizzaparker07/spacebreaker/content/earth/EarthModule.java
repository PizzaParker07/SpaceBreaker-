package com.pizzaparker07.spacebreaker.content.earth;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbDatapack;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/** Earth ores, the metals refined from them, alloys and the intermediate parts of the industry chain. */
public final class EarthModule implements SbModule {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Spacebreaker.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Spacebreaker.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Spacebreaker.MOD_ID);
    /** Shapeless crafting that keeps (and wears) tools such as shears. */
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, ToolShapelessRecipe.Serializer> TOOL_SHAPELESS =
            SERIALIZERS.register("tool_shapeless", ToolShapelessRecipe.Serializer::new);

    // ------------------------------------------------------------------ ore metals
    public static final Metal ALUMINUM = Metal.builder("aluminum", "Aluminum", "Alüminyum")
            .ore("bauxite", "Bauxite", "Boksit", MapColor.TERRACOTTA_RED, 2).wire().color(MapColor.METAL).build(BLOCKS, ITEMS);
    public static final Metal TITANIUM = Metal.builder("titanium", "Titanium", "Titanyum")
            .ore("rutile", "Rutile", "Rutil", MapColor.TERRACOTTA_BROWN, 4).arcFurnaceOnly().rod().color(MapColor.COLOR_GRAY).build(BLOCKS, ITEMS);
    public static final Metal NICKEL = Metal.builder("nickel", "Nickel", "Nikel")
            .ore("pentlandite", "Pentlandite", "Pentlandit", MapColor.GOLD, 3).color(MapColor.SAND).build(BLOCKS, ITEMS);
    public static final Metal LITHIUM = Metal.builder("lithium", "Lithium", "Lityum")
            .ore("spodumene", "Spodumene", "Spodümen", MapColor.TERRACOTTA_PINK, 3).color(MapColor.TERRACOTTA_WHITE).build(BLOCKS, ITEMS);
    public static final Metal TUNGSTEN = Metal.builder("tungsten", "Tungsten", "Tungsten")
            .ore("wolframite", "Wolframite", "Volframit", MapColor.COLOR_BLACK, 5).arcFurnaceOnly().rod().wire().color(MapColor.COLOR_GRAY).build(BLOCKS, ITEMS);

    // ------------------------------------------------------------------ alloys
    public static final Metal STEEL = Metal.builder("steel", "Steel", "Çelik").rod().noDust().color(MapColor.COLOR_GRAY).build(BLOCKS, ITEMS);
    public static final Metal AL_LI = Metal.builder("aluminum_lithium", "Al-Li Alloy", "Al-Li Alaşımı").noDust().build(BLOCKS, ITEMS);
    public static final Metal SUPERALLOY = Metal.builder("nickel_superalloy", "Nickel Superalloy", "Nikel Süper Alaşımı").noDust().build(BLOCKS, ITEMS);

    // ------------------------------------------------------------------ vanilla metals (missing parts only)
    public static final Metal IRON = Metal.builder("iron", "Iron", "Demir").vanilla(() -> Items.IRON_INGOT).rod().build(BLOCKS, ITEMS);
    public static final Metal COPPER = Metal.builder("copper", "Copper", "Bakır").vanilla(() -> Items.COPPER_INGOT).wire().build(BLOCKS, ITEMS);
    public static final Metal GOLD = Metal.builder("gold", "Gold", "Altın").vanilla(() -> Items.GOLD_INGOT).wire().build(BLOCKS, ITEMS);

    public static final List<Metal> ORE_METALS = List.of(ALUMINUM, TITANIUM, NICKEL, LITHIUM, TUNGSTEN);
    public static final List<Metal> METALS = List.of(ALUMINUM, TITANIUM, NICKEL, LITHIUM, TUNGSTEN, STEEL, AL_LI, SUPERALLOY, IRON, COPPER, GOLD);

    // ------------------------------------------------------------------ backwards-compatible names
    public static final DeferredBlock<DropExperienceBlock> BAUXITE_ORE = ALUMINUM.ore;
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_BAUXITE_ORE = ALUMINUM.deepslateOre;
    public static final DeferredBlock<Block> RAW_BAUXITE_BLOCK = ALUMINUM.rawBlock;
    public static final DeferredItem<Item> RAW_BAUXITE = ALUMINUM.raw;
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item> ALUMINUM_INGOT = (DeferredItem<Item>) ALUMINUM.ingot;
    public static final DeferredItem<Item> ALUMINUM_PLATE = ALUMINUM.plate;
    public static final DeferredItem<BlockItem> BAUXITE_ORE_ITEM = ALUMINUM.oreItem;
    public static final DeferredItem<BlockItem> DEEPSLATE_BAUXITE_ORE_ITEM = ALUMINUM.deepslateOreItem;
    public static final DeferredItem<BlockItem> RAW_BAUXITE_BLOCK_ITEM = ALUMINUM.rawBlockItem;
    public static final TagKey<Block> ORES_BAUXITE = ALUMINUM.oreBlockTag;
    public static final TagKey<Item> ITEM_ORES_BAUXITE = ALUMINUM.oreItemTag;
    public static final TagKey<Item> RAW_MATERIALS_BAUXITE = ALUMINUM.rawTag;
    public static final TagKey<Item> INGOTS_ALUMINUM = ALUMINUM.ingotTag;
    public static final TagKey<Item> PLATES_ALUMINUM = ALUMINUM.plateTag;

    // ------------------------------------------------------------------ intermediates
    public static final DeferredItem<Item> COAL_DUST = ITEMS.registerSimpleItem("coal_dust");
    public static final DeferredItem<Item> SILICON = ITEMS.registerSimpleItem("silicon");
    public static final DeferredItem<Item> SILICON_WAFER = ITEMS.registerSimpleItem("silicon_wafer");
    public static final DeferredItem<Item> SOLAR_CELL = ITEMS.registerSimpleItem("solar_cell");
    public static final DeferredItem<Item> PLASTIC_SHEET = ITEMS.registerSimpleItem("plastic_sheet");
    public static final DeferredItem<Item> BASIC_CIRCUIT = ITEMS.registerSimpleItem("basic_circuit");
    public static final DeferredItem<Item> ADVANCED_CIRCUIT = ITEMS.registerSimpleItem("advanced_circuit");
    public static final DeferredItem<Item> ELECTRIC_MOTOR = ITEMS.registerSimpleItem("electric_motor");
    public static final DeferredItem<Item> ABLATIVE_SHIELD_PLATE = ITEMS.registerSimpleItem("ablative_shield_plate");
    public static final DeferredItem<Item> GOLD_FOIL_INSULATION = ITEMS.registerSimpleItem("gold_foil_insulation");
    public static final DeferredItem<Item> ORANGE_FOAM_INSULATION = ITEMS.registerSimpleItem("orange_foam_insulation");
    public static final DeferredItem<Item> SILICONE_SEAL = ITEMS.registerSimpleItem("silicone_seal");
    public static final DeferredItem<Item> PLATE_DIE = ITEMS.registerItem("plate_press_die", Item::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> WIRE_DIE = ITEMS.registerItem("wire_press_die", Item::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> ROD_DIE = ITEMS.registerItem("rod_press_die", Item::new, new Item.Properties().stacksTo(1));

    /** Oil seeps mark crude-oil pockets below: dark tar-stained ground. */
    public static final DeferredBlock<OilSeepBlock> OIL_SEEP = BLOCKS.registerBlock("oil_seep", OilSeepBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.6F).sound(SoundType.MUD));
    public static final DeferredItem<BlockItem> OIL_SEEP_ITEM = ITEMS.registerSimpleBlockItem(OIL_SEEP);

    public static final DeferredHolder<Feature<?>, OilDepositFeature> OIL_DEPOSIT = FEATURES.register("oil_deposit", OilDepositFeature::new);

    public static final TagKey<Item> DIES = SbTagKeys.modItem("press_dies");
    public static final TagKey<Item> SILICON_TAG = SbTagKeys.commonItem("silicon");
    public static final TagKey<Item> PLASTIC_TAG = SbTagKeys.commonItem("plastics");
    public static final TagKey<Item> COAL_DUST_TAG = SbTagKeys.commonItem("dusts/coal");

    @Override
    public String id() {
        return "earth";
    }

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
        SERIALIZERS.register(modBus);
        SbTabs.Tab tab = SbTabs.MATERIALS.icon(ALUMINUM_INGOT);
        for (Metal m : METALS) {
            m.items().forEach(tab::add);
        }
        tab.add(COAL_DUST).add(SILICON).add(SILICON_WAFER).add(SOLAR_CELL).add(PLASTIC_SHEET)
                .add(BASIC_CIRCUIT).add(ADVANCED_CIRCUIT).add(ELECTRIC_MOTOR).add(ABLATIVE_SHIELD_PLATE)
                .add(GOLD_FOIL_INSULATION).add(ORANGE_FOAM_INSULATION).add(SILICONE_SEAL)
                .add(PLATE_DIE).add(WIRE_DIE).add(ROD_DIE).add(OIL_SEEP_ITEM);
    }

    // ------------------------------------------------------------------ translations

    @Override
    public void addTranslations(Lang lang) {
        lang.add("itemGroup.spacebreaker.materials", "Spacebreaker: Materials", "Spacebreaker: Malzemeler");
        for (Metal m : METALS) {
            if (m.hasOre()) {
                lang.block(m.ore, m.oreEn + " Ore", m.oreTr + " Cevheri");
                lang.block(m.deepslateOre, "Deepslate " + m.oreEn + " Ore", "Derin Kayrak " + m.oreTr + " Cevheri");
                lang.block(m.rawBlock, "Block of Raw " + m.oreEn, "Ham " + m.oreTr + " Bloğu");
                lang.item(m.raw, "Raw " + m.oreEn, "Ham " + m.oreTr);
            }
            if (!m.isVanilla()) {
                lang.add(m.ingot.get().getDescriptionId(), m.en + " Ingot", m.tr + " Külçe");
                lang.item(m.nugget, m.en + " Nugget", m.tr + " Parçası");
                lang.block(m.block, "Block of " + m.en, m.tr + " Bloğu");
            }
            if (m.plate != null) {
                lang.item(m.plate, m.en + " Plate", m.tr + " Levha");
            }
            if (m.dust != null) {
                lang.item(m.dust, m.en + " Dust", m.tr + " Tozu");
            }
            if (m.wire != null) {
                lang.item(m.wire, m.en + " Wire", m.tr + " Tel");
            }
            if (m.rod != null) {
                lang.item(m.rod, m.en + " Rod", m.tr + " Çubuk");
            }
        }
        tagNames(lang);
        lang.item(COAL_DUST, "Coal Dust", "Kömür Tozu");
        lang.item(SILICON, "Silicon", "Silisyum");
        lang.item(SILICON_WAFER, "Silicon Wafer", "Silisyum Plaka");
        lang.item(SOLAR_CELL, "Solar Cell", "Güneş Hücresi");
        lang.item(PLASTIC_SHEET, "Plastic Sheet", "Plastik Levha");
        lang.item(BASIC_CIRCUIT, "Basic Circuit", "Temel Devre Kartı");
        lang.item(ADVANCED_CIRCUIT, "Advanced Circuit", "Gelişmiş Devre Kartı");
        lang.item(ELECTRIC_MOTOR, "Electric Motor", "Elektrik Motoru");
        lang.item(ABLATIVE_SHIELD_PLATE, "Ablative Shield Plate", "Ablatif Kalkan Levhası");
        lang.item(GOLD_FOIL_INSULATION, "Gold Foil Insulation", "Altın Folyo Yalıtım");
        lang.item(ORANGE_FOAM_INSULATION, "Orange Foam Insulation", "Turuncu Köpük Yalıtım");
        lang.item(SILICONE_SEAL, "Silicone Seal", "Silikon Conta");
        lang.item(PLATE_DIE, "Plate Press Die", "Levha Pres Kalıbı");
        lang.item(WIRE_DIE, "Wire Press Die", "Tel Pres Kalıbı");
        lang.item(ROD_DIE, "Rod Press Die", "Çubuk Pres Kalıbı");
        lang.block(OIL_SEEP, "Oil Seep", "Petrol Sızıntısı");
        lang.add("tooltip.spacebreaker.press_die", "Used in the Press; not consumed", "Preste kullanılır; tükenmez");
        lang.add("tooltip.spacebreaker.oil_seep", "Crude oil lies somewhere below", "Altında bir yerde ham petrol var");
    }

    /** Names for every item tag this module owns (shown by recipe viewers), e.g. tag.item.c.ingots.titanium. */
    private static void tagNames(Lang lang) {
        for (Metal m : METALS) {
            if (m.hasOre()) {
                tag(lang, m.oreItemTag, m.oreEn + " Ores", m.oreTr + " Cevherleri");
                tag(lang, m.rawTag, "Raw " + m.oreEn, "Ham " + m.oreTr);
                tag(lang, m.rawBlockItemTag, "Blocks of Raw " + m.oreEn, "Ham " + m.oreTr + " Blokları");
            }
            if (!m.isVanilla()) {
                tag(lang, m.ingotTag, m.en + " Ingots", m.tr + " Külçeleri");
                tag(lang, m.nuggetTag, m.en + " Nuggets", m.tr + " Parçaları");
                tag(lang, m.blockItemTag, "Blocks of " + m.en, m.tr + " Blokları");
            }
            if (m.plate != null) {
                tag(lang, m.plateTag, m.en + " Plates", m.tr + " Levhaları");
            }
            if (m.dust != null) {
                tag(lang, m.dustTag, m.en + " Dusts", m.tr + " Tozları");
            }
            if (m.wire != null) {
                tag(lang, m.wireTag, m.en + " Wires", m.tr + " Telleri");
            }
            if (m.rod != null) {
                tag(lang, m.rodTag, m.en + " Rods", m.tr + " Çubukları");
            }
        }
        tag(lang, SbTagKeys.commonItem("plates"), "Plates", "Levhalar");
        tag(lang, SbTagKeys.commonItem("wires"), "Wires", "Teller");
        tag(lang, DIES, "Press Dies", "Pres Kalıpları");
        tag(lang, SILICON_TAG, "Silicon", "Silisyum");
        tag(lang, PLASTIC_TAG, "Plastics", "Plastikler");
        tag(lang, COAL_DUST_TAG, "Coal Dusts", "Kömür Tozları");
    }

    public static void tag(Lang lang, TagKey<Item> tag, String en, String tr) {
        lang.add("tag.item." + tag.location().getNamespace() + "." + tag.location().getPath().replace('/', '.'), en, tr);
    }

    // ------------------------------------------------------------------ loot and tags

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        for (Metal m : METALS) {
            if (m.hasOre()) {
                loot.ore(m.ore.get(), m.raw.get());
                loot.ore(m.deepslateOre.get(), m.raw.get());
                loot.self(m.rawBlock.get());
            }
            if (m.block != null) {
                loot.self(m.block.get());
            }
        }
        loot.self(OIL_SEEP.get());
    }

    @Override
    public void addTags(SbTags tags) {
        for (Metal m : METALS) {
            if (m.hasOre()) {
                Block[] ores = {m.ore.get(), m.deepslateOre.get()};
                tags.block(BlockTags.MINEABLE_WITH_PICKAXE, m.ore.get(), m.deepslateOre.get(), m.rawBlock.get());
                TagKey<Block> tier = m.furnace ? BlockTags.NEEDS_STONE_TOOL : BlockTags.NEEDS_IRON_TOOL;
                tags.block(tier, m.ore.get(), m.deepslateOre.get(), m.rawBlock.get());
                tags.block(m.oreBlockTag, ores);
                tags.blockTag(Tags.Blocks.ORES, m.oreBlockTag);
                tags.block(Tags.Blocks.ORES_IN_GROUND_STONE, m.ore.get());
                tags.block(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE, m.deepslateOre.get());
                tags.block(Tags.Blocks.ORE_RATES_SINGULAR, ores);
                tags.copy(m.oreBlockTag, m.oreItemTag);
                tags.itemTag(Tags.Items.ORES, m.oreItemTag);
                tags.item(m.rawTag, m.raw.get());
                tags.itemTag(Tags.Items.RAW_MATERIALS, m.rawTag);
                tags.block(m.rawBlockTag, m.rawBlock.get());
                tags.blockTag(Tags.Blocks.STORAGE_BLOCKS, m.rawBlockTag);
                tags.copy(m.rawBlockTag, m.rawBlockItemTag);
                tags.itemTag(Tags.Items.STORAGE_BLOCKS, m.rawBlockItemTag);
            }
            if (!m.isVanilla()) {
                tags.item(m.ingotTag, m.ingot.get());
                tags.itemTag(Tags.Items.INGOTS, m.ingotTag);
                tags.item(m.nuggetTag, m.nugget.get());
                tags.itemTag(Tags.Items.NUGGETS, m.nuggetTag);
                tags.block(BlockTags.MINEABLE_WITH_PICKAXE, m.block.get());
                tags.block(BlockTags.NEEDS_STONE_TOOL, m.block.get());
                tags.block(m.blockTag, m.block.get());
                tags.blockTag(Tags.Blocks.STORAGE_BLOCKS, m.blockTag);
                tags.copy(m.blockTag, m.blockItemTag);
                tags.itemTag(Tags.Items.STORAGE_BLOCKS, m.blockItemTag);
                tags.block(BlockTags.BEACON_BASE_BLOCKS, m.block.get());
            }
            tagPart(tags, m.plate, m.plateTag, SbTagKeys.commonItem("plates"));
            tagPart(tags, m.dust, m.dustTag, Tags.Items.DUSTS);
            tagPart(tags, m.wire, m.wireTag, SbTagKeys.commonItem("wires"));
            tagPart(tags, m.rod, m.rodTag, Tags.Items.RODS);
        }
        tags.item(DIES, PLATE_DIE.get(), WIRE_DIE.get(), ROD_DIE.get());
        tags.item(SILICON_TAG, SILICON.get());
        tags.item(PLASTIC_TAG, PLASTIC_SHEET.get());
        tags.item(COAL_DUST_TAG, COAL_DUST.get());
        tags.itemTag(Tags.Items.DUSTS, COAL_DUST_TAG);
        tags.item(ItemTags.BEACON_PAYMENT_ITEMS, ALUMINUM_INGOT.get());
        tags.block(BlockTags.MINEABLE_WITH_SHOVEL, OIL_SEEP.get());
    }

    private static void tagPart(SbTags tags, Supplier<? extends Item> item, TagKey<Item> tag, TagKey<Item> parent) {
        if (item != null) {
            tags.item(tag, item.get());
            tags.itemTag(parent, tag);
        }
    }

    // ------------------------------------------------------------------ crafting and smelting

    @Override
    public void addRecipes(RecipeOutput out, HolderLookup.Provider registries) {
        for (Metal m : METALS) {
            ItemLike ingot = m.ingotItem();
            if (m.nugget != null) {
                pack(out, m.nugget.get(), ingot, m.nuggetTag, m.name + "_ingot_from_nuggets", m.name + "_nuggets");
                pack(out, ingot, m.block.get(), m.ingotTag, m.name + "_block", m.name + "_ingot_from_block");
            }
            if (m.hasOre()) {
                pack(out, m.raw.get(), m.rawBlock.get(), m.rawTag, "raw_" + m.oreName + "_block", "raw_" + m.oreName + "_from_block");
                if (m.furnace) {
                    smelt(out, Ingredient.of(m.oreItemTag), ingot, m.name + "_ingot_from_ore", 0.7F);
                    smelt(out, Ingredient.of(m.rawTag), ingot, m.name + "_ingot_from_raw", 0.7F);
                }
            }
            if (m.dust != null && m.furnace) {
                smelt(out, Ingredient.of(m.dustTag), ingot, m.name + "_ingot_from_dust", 0.2F);
            }
            if (m.plate != null) {
                // early-game fallback: two ingots hammered into one plate (the Press makes them 1:1)
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, m.plate.get()).pattern("#").pattern("#")
                        .define('#', m.ingotTag)
                        .unlockedBy("has_ingot", has(m.ingotTag)).save(out, Spacebreaker.id(m.name + "_plate_by_hand"));
            }
        }
        // early-game fallback (the Press makes 2 wires per ingot): one hand-made plate + shears -> 2 wires
        out.accept(Spacebreaker.id("copper_wire_by_hand"), new ToolShapelessRecipe("", net.minecraft.world.item.crafting.CraftingBookCategory.MISC,
                new net.minecraft.world.item.ItemStack(COPPER.wire.get(), 2),
                net.minecraft.core.NonNullList.of(Ingredient.EMPTY, Ingredient.of(COPPER.plateTag), Ingredient.of(Items.SHEARS))), null);
        // rods by hand (the Press gives 1 rod per ingot): three ingots in a column -> 2 rods.
        // Breaks the loop "electric motor needs a rod, rods need the Press, the Press needs a motor".
        for (Metal m : METALS) {
            if (m.rod != null) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, m.rod.get(), 2).pattern("#").pattern("#").pattern("#")
                        .define('#', m.ingotTag).unlockedBy("has_ingot", has(m.ingotTag)).save(out, Spacebreaker.id(m.name + "_rod_by_hand"));
            }
        }
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BASIC_CIRCUIT.get(), 2)
                .pattern("WRW").pattern("RPR").pattern("WRW")
                .define('W', COPPER.wireTag).define('R', Tags.Items.DUSTS_REDSTONE).define('P', IRON.plateTag)
                .unlockedBy("has_wire", has(COPPER.wireTag)).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ELECTRIC_MOTOR.get())
                .pattern("PWP").pattern("WIW").pattern("PWP")
                .define('W', COPPER.wireTag).define('I', IRON.rodTag).define('P', IRON.plateTag)
                .unlockedBy("has_wire", has(COPPER.wireTag)).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, PLATE_DIE.get())
                .pattern("SS").pattern("SS").define('S', STEEL.plateTag)
                .unlockedBy("has_steel", has(STEEL.plateTag)).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, WIRE_DIE.get())
                .pattern("SSS").pattern("S S").pattern("SSS").define('S', STEEL.plateTag)
                .unlockedBy("has_steel", has(STEEL.plateTag)).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ROD_DIE.get())
                .pattern("S S").pattern("S S").pattern("S S").define('S', STEEL.plateTag)
                .unlockedBy("has_steel", has(STEEL.plateTag)).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SILICONE_SEAL.get(), 4)
                .requires(SILICON_TAG).requires(PLASTIC_TAG).requires(Tags.Items.DYES_RED)
                .unlockedBy("has_silicon", has(SILICON_TAG)).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, COAL_DUST.get())
                .requires(Items.COAL).requires(Items.FLINT)
                .unlockedBy("has_coal", has(Items.COAL)).save(out, Spacebreaker.id("coal_dust_by_hand"));
    }

    private static void pack(RecipeOutput out, ItemLike small, ItemLike big, TagKey<Item> smallTag, String packId, String unpackId) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, big).pattern("###").pattern("###").pattern("###")
                .define('#', small).unlockedBy("has_item", has(smallTag)).save(out, Spacebreaker.id(packId));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, small, 9).requires(big)
                .unlockedBy("has_item", has(big)).save(out, Spacebreaker.id(unpackId));
    }

    private static void smelt(RecipeOutput out, Ingredient in, ItemLike result, String id, float xp) {
        SimpleCookingRecipeBuilder.smelting(in, RecipeCategory.MISC, result, xp, 200)
                .unlockedBy("has_item", has(Items.FURNACE)).save(out, Spacebreaker.id(id + "_smelting"));
        SimpleCookingRecipeBuilder.blasting(in, RecipeCategory.MISC, result, xp, 100)
                .unlockedBy("has_item", has(Items.BLAST_FURNACE)).save(out, Spacebreaker.id(id + "_blasting"));
    }

    static net.minecraft.advancements.Criterion<?> has(TagKey<Item> tag) {
        return net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(
                net.minecraft.advancements.critereon.ItemPredicate.Builder.item().of(tag));
    }

    static net.minecraft.advancements.Criterion<?> has(ItemLike item) {
        return net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    // ------------------------------------------------------------------ worldgen

    @Override
    public void addDatapackEntries(SbDatapack datapack) {
        EarthWorldgen.bootstrap(datapack);
    }
}
