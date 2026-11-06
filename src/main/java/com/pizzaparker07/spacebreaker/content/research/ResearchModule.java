package com.pizzaparker07.spacebreaker.content.research;

import com.mojang.serialization.Codec;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.research.lab.ResearchLabBlock;
import com.pizzaparker07.spacebreaker.content.research.lab.ResearchLabBlockEntity;
import com.pizzaparker07.spacebreaker.content.research.lab.ResearchLabMenu;
import com.pizzaparker07.spacebreaker.content.research.net.ResearchNet;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.advancements.CriterionTrigger;

/**
 * Research: the tech tree content, tech disks, field samples and experiment data, the Research Lab,
 * client sync of known technologies, research advancements and the guide book item.
 * Public entry points for other areas: {@link Research} (teach, disks, samples) and {@link Milestones}.
 */
public final class ResearchModule implements SbModule {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Spacebreaker.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Spacebreaker.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Spacebreaker.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, Spacebreaker.MOD_ID);

    // ------------------------------------------------------------------ data components

    /** The technology stored on a tech disk. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> TECH =
            COMPONENTS.registerComponentType("tech", b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));
    /** What a field sample or experiment record contains: a celestial body id or an experiment id. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SAMPLE =
            COMPONENTS.registerComponentType("sample", b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));
    /** Set once a disk has been read by a lab (purely informative). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ANALYSED =
            COMPONENTS.registerComponentType("analysed", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    // ------------------------------------------------------------------ items

    public static final DeferredItem<TechDiskItem> TECH_DISK = ITEMS.registerItem("tech_disk", TechDiskItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<SampleItem> FIELD_SAMPLE = ITEMS.registerItem("field_sample", p -> new SampleItem(p, false),
            new Item.Properties().stacksTo(16));
    public static final DeferredItem<SampleItem> EXPERIMENT_DATA = ITEMS.registerItem("experiment_data", p -> new SampleItem(p, true),
            new Item.Properties().stacksTo(16));
    public static final DeferredItem<SampleKitItem> SAMPLE_KIT = ITEMS.registerItem("sample_kit", SampleKitItem::new,
            new Item.Properties().durability(32));
    public static final DeferredItem<GuideBookItem> GUIDE_BOOK = ITEMS.registerItem("guide_book", GuideBookItem::new,
            new Item.Properties().stacksTo(1));

    // ------------------------------------------------------------------ research lab

    public static final DeferredBlock<ResearchLabBlock> RESEARCH_LAB = BLOCKS.registerBlock("research_lab", ResearchLabBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(s -> s.getValue(ResearchLabBlock.WORKING) ? 6 : 0));
    public static final DeferredItem<BlockItem> RESEARCH_LAB_ITEM = ITEMS.registerSimpleBlockItem(RESEARCH_LAB);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchLabBlockEntity>> RESEARCH_LAB_BE =
            BLOCK_ENTITIES.register("research_lab", () -> BlockEntityType.Builder.of(ResearchLabBlockEntity::new, RESEARCH_LAB.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<ResearchLabMenu>> RESEARCH_LAB_MENU =
            MENUS.register("research_lab", () -> IMenuTypeExtension.create(ResearchLabMenu::new));

    // ------------------------------------------------------------------ loot + advancements

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<TechDiskLootFunction>> RANDOM_TECH_DISK =
            LOOT_FUNCTIONS.register("random_tech_disk", () -> new LootItemFunctionType<>(TechDiskLootFunction.CODEC));

    public static final DeferredHolder<CriterionTrigger<?>, TechLearnedTrigger> TECH_LEARNED =
            TRIGGERS.register("tech_learned", TechLearnedTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, Milestones.Trigger> MILESTONE =
            TRIGGERS.register("milestone", Milestones.Trigger::new);

    /** Items a Research Lab accepts. */
    public static final TagKey<Item> RESEARCHABLE = SbTagKeys.modItem("researchable");

    @Override
    public String id() {
        return "research";
    }

    @Override
    public void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        LOOT_FUNCTIONS.register(modBus);
        TRIGGERS.register(modBus);
        modBus.addListener(ResearchModule::capabilities);
        modBus.addListener(ResearchNet::register);
        modBus.addListener(com.pizzaparker07.spacebreaker.content.research.data.ResearchDataGen::gather);
        ResearchEvents.register();

        SbTabs.RESEARCH.icon(GUIDE_BOOK).add(GUIDE_BOOK).add(RESEARCH_LAB_ITEM).add(SAMPLE_KIT)
                .addStack(() -> Research.sample(ResearchTree.EARTH))
                .addStack(() -> Research.sample(ResearchTree.MOON))
                .addStack(() -> Research.experimentData(ResearchTree.SUBORBITAL));
        // one disk per technology (in tree order); the list is static so the tab does not depend on a world
        for (ResourceLocation tech : ResearchTree.all().keySet()) {
            SbTabs.RESEARCH.addStack(() -> Research.disk(tech));
        }
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, RESEARCH_LAB_BE.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RESEARCH_LAB_BE.get(), ResearchLabBlockEntity::itemHandler);
    }

    @Override
    public void addTranslations(Lang lang) {
        ResearchTree.addTranslations(lang);
        ResearchLang.add(lang);
        com.pizzaparker07.spacebreaker.content.research.data.ResearchAdvancements.addTranslations(lang);
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        loot.self(RESEARCH_LAB.get());
    }

    @Override
    public void addTags(SbTags tags) {
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, RESEARCH_LAB.get());
        tags.item(RESEARCHABLE, TECH_DISK.get(), FIELD_SAMPLE.get(), EXPERIMENT_DATA.get());
    }

    static ItemStack stack(Item item) {
        return new ItemStack(item);
    }
}
