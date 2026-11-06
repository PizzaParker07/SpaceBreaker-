package com.pizzaparker07.spacebreaker.content.suit;

import com.mojang.serialization.Codec;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.Ingredient;
import com.pizzaparker07.spacebreaker.content.suit.net.SuitNetwork;
import com.pizzaparker07.spacebreaker.client.hud.HudConfig;
import com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock;
import com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlockEntity;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenDistributorBlock;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenDistributorBlockEntity;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenFillerBlock;
import com.pizzaparker07.spacebreaker.content.suit.block.OxygenFillerBlockEntity;
import com.pizzaparker07.spacebreaker.content.suit.block.SealedRooms;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/** EVA suits, oxygen tanks and life support equipment. */
public final class SuitModule implements SbModule {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Spacebreaker.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Spacebreaker.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Spacebreaker.MOD_ID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Spacebreaker.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Spacebreaker.MOD_ID);

    /** Stored oxygen, in breathing ticks (20 = one second of breathing). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> OXYGEN =
            COMPONENTS.registerComponentType("oxygen", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** Whether the gold sun visor of a helmet is lowered. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SUN_VISOR =
            COMPONENTS.registerComponentType("sun_visor", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    /** Oxygen tanks slotted into an EVA torso's backpack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> SUIT_TANKS =
            COMPONENTS.registerComponentType("suit_tanks", b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding());
    /** Maneuvering Unit clipped onto an EVA torso. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> SUIT_MMU =
            COMPONENTS.registerComponentType("suit_mmu", b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding());
    /** Cold-gas propellant of a Maneuvering Unit, in thrust ticks. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GAS_FUEL =
            COMPONENTS.registerComponentType("gas_fuel", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Breath, body temperature and radiation dose of a player. Reset on death. */
    public static final Supplier<AttachmentType<SurvivalData>> SURVIVAL = ATTACHMENTS.register("survival",
            () -> AttachmentType.builder(() -> new SurvivalData()).serialize(SurvivalData.CODEC).build());

    /** Mk1 EVA suit: vacuum rated, light protection — a pressure garment, not combat armour. */
    public static final Holder<ArmorMaterial> EVA_SUIT = MATERIALS.register("eva_suit", () -> new ArmorMaterial(
            defense(1, 2, 3, 1, 3), 9, SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(EarthModule.ALUMINUM_PLATE.get()),
            List.of(new ArmorMaterial.Layer(Spacebreaker.id("eva_suit"))), 0.0F, 0.0F));

    public static final DeferredItem<OxygenTankItem> OXYGEN_TANK = ITEMS.registerItem("oxygen_tank",
            p -> new OxygenTankItem(p, OxygenTankItem.SMALL_CAPACITY), new Item.Properties().stacksTo(1));
    public static final DeferredItem<OxygenTankItem> LARGE_OXYGEN_TANK = ITEMS.registerItem("large_oxygen_tank",
            p -> new OxygenTankItem(p, OxygenTankItem.LARGE_CAPACITY), new Item.Properties().stacksTo(1));
    public static final DeferredItem<SpaceSuitItem> EVA_HELMET = suit("eva_helmet", ArmorItem.Type.HELMET, "eva_helmet");
    public static final DeferredItem<SpaceSuitItem> EVA_CHESTPLATE = suit("eva_chestplate", ArmorItem.Type.CHESTPLATE, "eva_torso");
    public static final DeferredItem<SpaceSuitItem> EVA_LEGGINGS = suit("eva_leggings", ArmorItem.Type.LEGGINGS, "eva_legs");
    public static final DeferredItem<SpaceSuitItem> EVA_BOOTS = suit("eva_boots", ArmorItem.Type.BOOTS, "eva_boots");
    public static final DeferredItem<SpaceSuitItem> MAGNETIC_BOOTS = suit("magnetic_boots", ArmorItem.Type.BOOTS, "magnetic_boots");
    public static final DeferredItem<ManeuveringUnitItem> MANEUVERING_UNIT = ITEMS.registerItem("maneuvering_unit",
            ManeuveringUnitItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    // ------------------------------------------------------------------ life support blocks

    /** Blocks that always hold air (in addition to full solid blocks). */
    public static final TagKey<Block> AIRTIGHT = TagKey.create(Registries.BLOCK, Spacebreaker.id("airtight"));
    /** Full blocks that still let air through (leaves, ...). */
    public static final TagKey<Block> LEAKY = TagKey.create(Registries.BLOCK, Spacebreaker.id("leaky"));

    public static final DeferredBlock<OxygenFillerBlock> OXYGEN_FILLER = BLOCKS.registerBlock("oxygen_filler", OxygenFillerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<OxygenDistributorBlock> OXYGEN_DISTRIBUTOR = BLOCKS.registerBlock("oxygen_distributor", OxygenDistributorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredBlock<AirlockDoorBlock> AIRLOCK_DOOR = BLOCKS.registerBlock("airlock_door", AirlockDoorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 12.0F).sound(SoundType.METAL).noOcclusion()
                    .requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> OXYGEN_FILLER_ITEM = ITEMS.registerSimpleBlockItem(OXYGEN_FILLER);
    public static final DeferredItem<BlockItem> OXYGEN_DISTRIBUTOR_ITEM = ITEMS.registerSimpleBlockItem(OXYGEN_DISTRIBUTOR);
    public static final DeferredItem<DoubleHighBlockItem> AIRLOCK_DOOR_ITEM = ITEMS.registerItem("airlock_door",
            p -> new DoubleHighBlockItem(AIRLOCK_DOOR.get(), p));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OxygenFillerBlockEntity>> OXYGEN_FILLER_BE =
            BLOCK_ENTITIES.register("oxygen_filler", () -> BlockEntityType.Builder.of(OxygenFillerBlockEntity::new, OXYGEN_FILLER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OxygenDistributorBlockEntity>> OXYGEN_DISTRIBUTOR_BE =
            BLOCK_ENTITIES.register("oxygen_distributor", () -> BlockEntityType.Builder.of(OxygenDistributorBlockEntity::new, OXYGEN_DISTRIBUTOR.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AirlockDoorBlockEntity>> AIRLOCK_DOOR_BE =
            BLOCK_ENTITIES.register("airlock_door", () -> BlockEntityType.Builder.of(AirlockDoorBlockEntity::new, AIRLOCK_DOOR.get()).build(null));

    private static DeferredItem<SpaceSuitItem> suit(String name, ArmorItem.Type type, String texture) {
        return ITEMS.registerItem(name, p -> new SpaceSuitItem(EVA_SUIT, type, SuitTier.MK1, texture, p),
                new Item.Properties().durability(type.getDurability(20)).rarity(Rarity.UNCOMMON));
    }

    @Override
    public String id() {
        return "suit";
    }

    @Override
    public void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(SuitModule::capabilities);
        Environment.addOxygenSource(SealedRooms::providesOxygen);
        ATTACHMENTS.register(modBus);
        modBus.addListener(SuitNetwork::register);
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, HudConfig.SPEC, "spacebreaker-hud.toml");
        MATERIALS.register(modBus);
        ITEMS.register(modBus);
        SbTabs.EQUIPMENT.icon(EVA_HELMET).add(EVA_HELMET).add(EVA_CHESTPLATE).add(EVA_LEGGINGS).add(EVA_BOOTS).add(MAGNETIC_BOOTS)
                .addStack(() -> OxygenTankItem.filled(OXYGEN_TANK.get()))
                .addStack(() -> OxygenTankItem.filled(LARGE_OXYGEN_TANK.get()))
                .addStack(() -> ManeuveringUnitItem.filled(MANEUVERING_UNIT.get()))
                .add(OXYGEN_FILLER_ITEM).add(OXYGEN_DISTRIBUTOR_ITEM).add(AIRLOCK_DOOR_ITEM);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        for (var type : List.of(OXYGEN_FILLER_BE.get(), OXYGEN_DISTRIBUTOR_BE.get())) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> be.getItems());
        }
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, OXYGEN_FILLER_BE.get(), (be, side) -> be.getEnergy());
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.add("itemGroup.spacebreaker.equipment", "Spacebreaker: Equipment", "Spacebreaker: Ekipman");
        lang.item(OXYGEN_TANK, "Oxygen Tank", "Oksijen Tüpü");
        lang.block(OXYGEN_FILLER, "Oxygen Filler", "Oksijen Dolum İstasyonu");
        lang.block(OXYGEN_DISTRIBUTOR, "Oxygen Distributor", "Oksijen Dağıtıcı");
        lang.block(AIRLOCK_DOOR, "Airlock Door", "Hava Kilidi Kapısı");
        lang.add("message.spacebreaker.machine.empty", "No tank inserted · %s / %s FE", "Tüp takılı değil · %s / %s FE");
        lang.add("message.spacebreaker.distributor.scanning", "Checking the room for leaks...", "Oda sızıntıya karşı kontrol ediliyor...");
        lang.add("message.spacebreaker.distributor.leak", "Room is not sealed", "Oda sızdırmaz değil");
        lang.add("message.spacebreaker.distributor.leak_at", "Leak near %s %s %s (or the room is too large)", "Sızıntı: %s %s %s yakınında (ya da oda çok büyük)");
        lang.add("message.spacebreaker.distributor.no_oxygen", "Sealed room of %s blocks: insert an oxygen tank", "%s bloklu sızdırmaz oda: bir oksijen tüpü tak");
        lang.add("message.spacebreaker.distributor.sealed", "Sealed room of %s blocks · oxygen for %s", "%s bloklu sızdırmaz oda · %s yetecek oksijen");
        lang.add("tooltip.spacebreaker.oxygen_filler", "Refills tanks from breathable air. Needs FE", "Tüpleri solunabilir havadan doldurur. FE gerekir");
        lang.add("tooltip.spacebreaker.oxygen_distributor", "Pressurises the sealed room around it from a tank", "Çevresindeki sızdırmaz odayı tüpten oksijenle doldurur");
        lang.add("tooltip.spacebreaker.airlock_door", "Airtight when closed. Opens by hand or redstone", "Kapalıyken hava geçirmez. Elle ya da kızıltaşla açılır");
        lang.item(LARGE_OXYGEN_TANK, "Large Oxygen Tank", "Büyük Oksijen Tüpü");
        lang.item(EVA_HELMET, "EVA Helmet", "EVA Kaskı");
        lang.item(EVA_CHESTPLATE, "EVA Torso", "EVA Gövdesi");
        lang.item(EVA_LEGGINGS, "EVA Leggings", "EVA Pantolonu");
        lang.item(EVA_BOOTS, "EVA Boots", "EVA Botları");
        lang.item(MAGNETIC_BOOTS, "Magnetic EVA Boots", "Manyetik EVA Botları");
        lang.item(MANEUVERING_UNIT, "Maneuvering Unit", "Manevra Ünitesi");
        lang.add("tooltip.spacebreaker.oxygen.hint", "Use while wearing an EVA torso to slot it in", "EVA gövdesi giyiliyken kullanınca sırt çantasına takılır");
        lang.add("tooltip.spacebreaker.suit.range", "Rated %s °C to %s °C", "Dayanım: %s °C ile %s °C arası");
        lang.add("tooltip.spacebreaker.suit.set", "Sealed only with all four pieces worn", "Yalnızca dört parça birlikte giyilince sızdırmaz");
        lang.add("tooltip.spacebreaker.suit.tank", "Tank %s: %s %s / %s", "Tüp %s: %s %s / %s");
        lang.add("tooltip.spacebreaker.suit.tank_empty", "Tank %s: empty slot", "Tüp %s: boş yuva");
        lang.add("tooltip.spacebreaker.suit.mmu_installed", "Maneuvering Unit fitted (%s%% gas)", "Manevra Ünitesi takılı (%%%s gaz)");
        lang.add("tooltip.spacebreaker.suit.tank_hint", "Right-click with a tank or unit to fit it; with an empty cursor to take one out",
                "Tüp ya da ünite ile sağ tıkla: takar; boş imleçle sağ tıkla: çıkarır");
        lang.add("death.attack.spacebreaker.vacuum", "%1$s was exposed to vacuum", "%1$s boşlukta havasız kaldı");
        lang.add("death.attack.spacebreaker.vacuum.player", "%1$s was exposed to vacuum while fighting %2$s", "%1$s, %2$s ile dövüşürken boşlukta havasız kaldı");
        lang.add("death.attack.spacebreaker.hypothermia", "%1$s froze in the cold of space", "%1$s uzayın soğuğunda dondu");
        lang.add("death.attack.spacebreaker.hypothermia.player", "%1$s froze while fighting %2$s", "%1$s, %2$s ile dövüşürken dondu");
        lang.add("death.attack.spacebreaker.hyperthermia", "%1$s overheated", "%1$s aşırı ısındı");
        lang.add("death.attack.spacebreaker.hyperthermia.player", "%1$s overheated while fighting %2$s", "%1$s, %2$s ile dövüşürken aşırı ısındı");
        lang.add("death.attack.spacebreaker.radiation", "%1$s succumbed to radiation sickness", "%1$s radyasyon hastalığına yenik düştü");
        lang.add("death.attack.spacebreaker.radiation.player", "%1$s succumbed to radiation while fighting %2$s", "%1$s, %2$s ile dövüşürken radyasyona yenik düştü");
        lang.add("hud.spacebreaker.oxygen", "O2", "O2");
        lang.add("hud.spacebreaker.suit", "SUIT", "GİYSİ");
        lang.add("hud.spacebreaker.sealed", "SEALED", "SIZDIRMAZ");
        lang.add("hud.spacebreaker.open", "OPEN", "AÇIK");
        lang.add("hud.spacebreaker.body", "BODY", "VÜCUT");
        lang.add("hud.spacebreaker.outside", "OUTSIDE", "DIŞARI");
        lang.add("hud.spacebreaker.radiation", "RAD", "RAD");
        lang.add("hud.spacebreaker.pressure", "AMB", "ORTAM");
        lang.add("hud.spacebreaker.mmu", "MMU GAS", "MMU GAZ");
        lang.add("hud.spacebreaker.warn.unsealed", "SUIT NOT SEALED", "GİYSİ SIZDIRMAZ DEĞİL");
        lang.add("hud.spacebreaker.warn.no_oxygen", "OXYGEN DEPLETED", "OKSİJEN BİTTİ");
        lang.add("hud.spacebreaker.warn.low_oxygen", "LOW OXYGEN", "OKSİJEN AZ");
        lang.add("hud.spacebreaker.warn.hypoxia", "HYPOXIA", "HİPOKSİ");
        lang.add("hud.spacebreaker.warn.cold", "HYPOTHERMIA", "HİPOTERMİ");
        lang.add("hud.spacebreaker.warn.hot", "OVERHEATING", "AŞIRI ISINMA");
        lang.add("hud.spacebreaker.warn.radiation", "HIGH RADIATION", "YÜKSEK RADYASYON");
        lang.add("hud.spacebreaker.warn.damaged", "SUIT DAMAGED", "GİYSİ HASARLI");
        lang.add("tooltip.spacebreaker.mmu.fuel", "Cold gas: %s%%", "Soğuk gaz: %%%s");
        lang.add("tooltip.spacebreaker.mmu.hint", "Fit onto an EVA torso; fly with jump and sneak", "EVA gövdesine takılır; zıpla ve eğil tuşlarıyla uçar");
        lang.add("tooltip.spacebreaker.oxygen", "Oxygen: %s / %s", "Oksijen: %s / %s");
        lang.add("tooltip.spacebreaker.sun_visor.up", "Sun visor: raised", "Güneş vizörü: kalkık");
        lang.add("tooltip.spacebreaker.sun_visor.down", "Sun visor: lowered", "Güneş vizörü: indirilmiş");
        lang.add("tooltip.spacebreaker.sun_visor.hint", "Sneak + use to toggle the sun visor", "Güneş vizörünü indirip kaldırmak için eğilip kullan");
    }

    @Override
    public void addTags(SbTags tags) {
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, OXYGEN_FILLER.get(), OXYGEN_DISTRIBUTOR.get(), AIRLOCK_DOOR.get());
        tags.block(AIRTIGHT, Blocks.IRON_TRAPDOOR, Blocks.IRON_DOOR);
        tags.blockTag(LEAKY, BlockTags.LEAVES);
        tags.item(ItemTags.HEAD_ARMOR, EVA_HELMET.get());
        tags.item(ItemTags.CHEST_ARMOR, EVA_CHESTPLATE.get());
        tags.item(ItemTags.LEG_ARMOR, EVA_LEGGINGS.get());
        tags.item(ItemTags.FOOT_ARMOR, EVA_BOOTS.get(), MAGNETIC_BOOTS.get());
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        loot.self(OXYGEN_FILLER.get());
        loot.self(OXYGEN_DISTRIBUTOR.get());
        loot.custom(AIRLOCK_DOOR.get(), b -> LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(b).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(b)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(AirlockDoorBlock.HALF, DoubleBlockHalf.LOWER))))
                .when(ExplosionCondition.survivesExplosion())));
    }

    private static EnumMap<ArmorItem.Type, Integer> defense(int boots, int legs, int chest, int helmet, int body) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.BOOTS, boots);
        map.put(ArmorItem.Type.LEGGINGS, legs);
        map.put(ArmorItem.Type.CHESTPLATE, chest);
        map.put(ArmorItem.Type.HELMET, helmet);
        map.put(ArmorItem.Type.BODY, body);
        return map;
    }
}
