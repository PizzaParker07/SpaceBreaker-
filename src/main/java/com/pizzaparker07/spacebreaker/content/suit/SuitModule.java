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
import net.minecraft.world.item.crafting.RecipeSerializer;
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
    /** Helmet lamps in long-beam mode: a narrower, longer cone for lava tubes and cold traps. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LONG_BEAM =
            COMPONENTS.registerComponentType("long_beam", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
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
            List.of(new ArmorMaterial.Layer(Spacebreaker.id("eva_suit")), new ArmorMaterial.Layer(Spacebreaker.id("eva_suit"), "_accent", true)), 0.0F, 0.0F));

    public static final DeferredItem<OxygenTankItem> OXYGEN_TANK = ITEMS.registerItem("oxygen_tank",
            p -> new OxygenTankItem(p, OxygenTankItem.SMALL_CAPACITY), new Item.Properties().stacksTo(1));
    public static final DeferredItem<OxygenTankItem> LARGE_OXYGEN_TANK = ITEMS.registerItem("large_oxygen_tank",
            p -> new OxygenTankItem(p, OxygenTankItem.LARGE_CAPACITY), new Item.Properties().stacksTo(1));
    public static final DeferredItem<SpaceSuitItem> EVA_HELMET = suit("eva_helmet", ArmorItem.Type.HELMET, "eva_helmet");
    public static final DeferredItem<SpaceSuitItem> EVA_CHESTPLATE = suit("eva_chestplate", ArmorItem.Type.CHESTPLATE, "eva_torso");
    public static final DeferredItem<SpaceSuitItem> EVA_LEGGINGS = suit("eva_leggings", ArmorItem.Type.LEGGINGS, "eva_legs");
    public static final DeferredItem<SpaceSuitItem> EVA_BOOTS = suit("eva_boots", ArmorItem.Type.BOOTS, "eva_boots");
    public static final DeferredItem<SpaceSuitItem> MAGNETIC_BOOTS = suit("magnetic_boots", ArmorItem.Type.BOOTS, "magnetic_boots");

    /** Radiation-hardened suits are heavier armour (tungsten plates). */
    public static final Holder<ArmorMaterial> HEAVY_SUIT = MATERIALS.register("heavy_suit", () -> new ArmorMaterial(
            defense(2, 4, 5, 2, 5), 7, SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT),
            List.of(new ArmorMaterial.Layer(Spacebreaker.id("heavy_suit")), new ArmorMaterial.Layer(Spacebreaker.id("heavy_suit"), "_accent", true)), 1.0F, 0.05F));

    public static final DeferredItem<SpaceSuitItem> THERMAL_HELMET = tierPiece(SuitTier.THERMAL, ArmorItem.Type.HELMET);
    public static final DeferredItem<SpaceSuitItem> THERMAL_CHESTPLATE = tierPiece(SuitTier.THERMAL, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<SpaceSuitItem> THERMAL_LEGGINGS = tierPiece(SuitTier.THERMAL, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<SpaceSuitItem> THERMAL_BOOTS = tierPiece(SuitTier.THERMAL, ArmorItem.Type.BOOTS);
    public static final DeferredItem<SpaceSuitItem> HEATED_HELMET = tierPiece(SuitTier.HEATED, ArmorItem.Type.HELMET);
    public static final DeferredItem<SpaceSuitItem> HEATED_CHESTPLATE = tierPiece(SuitTier.HEATED, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<SpaceSuitItem> HEATED_LEGGINGS = tierPiece(SuitTier.HEATED, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<SpaceSuitItem> HEATED_BOOTS = tierPiece(SuitTier.HEATED, ArmorItem.Type.BOOTS);
    public static final DeferredItem<SpaceSuitItem> RADIATION_HELMET = tierPiece(SuitTier.RADIATION, ArmorItem.Type.HELMET);
    public static final DeferredItem<SpaceSuitItem> RADIATION_CHESTPLATE = tierPiece(SuitTier.RADIATION, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<SpaceSuitItem> RADIATION_LEGGINGS = tierPiece(SuitTier.RADIATION, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<SpaceSuitItem> RADIATION_BOOTS = tierPiece(SuitTier.RADIATION, ArmorItem.Type.BOOTS);

    /** Every suit piece, for tags, client registration and tests. */
    public static final List<DeferredItem<SpaceSuitItem>> ALL_PIECES = List.of(EVA_HELMET, EVA_CHESTPLATE, EVA_LEGGINGS, EVA_BOOTS, MAGNETIC_BOOTS,
            THERMAL_HELMET, THERMAL_CHESTPLATE, THERMAL_LEGGINGS, THERMAL_BOOTS, HEATED_HELMET, HEATED_CHESTPLATE, HEATED_LEGGINGS, HEATED_BOOTS,
            RADIATION_HELMET, RADIATION_CHESTPLATE, RADIATION_LEGGINGS, RADIATION_BOOTS);

    // ------------------------------------------------------------------ upgrade modules

    /** Which module a suit piece carries. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SuitUpgrade>> SUIT_UPGRADE =
            COMPONENTS.registerComponentType("suit_upgrade", b -> b.persistent(SuitUpgrade.CODEC).networkSynchronized(SuitUpgrade.STREAM_CODEC));
    public static final DeferredItem<Item> SUIT_UPGRADE_TEMPLATE = ITEMS.registerSimpleItem("suit_upgrade_template", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<SuitModuleItem> THERMAL_REGULATOR_MODULE = module("thermal_regulator_module", SuitUpgrade.THERMAL_REGULATOR);
    public static final DeferredItem<SuitModuleItem> RADIATION_SHIELD_MODULE = module("radiation_shield_module", SuitUpgrade.RADIATION_SHIELD);
    public static final DeferredItem<SuitModuleItem> EXTRA_OXYGEN_MODULE = module("extra_oxygen_module", SuitUpgrade.EXTRA_OXYGEN);
    public static final DeferredItem<SuitModuleItem> JETPACK_BOOSTER_MODULE = module("jetpack_booster_module", SuitUpgrade.JETPACK_BOOSTER);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Spacebreaker.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SuitUpgradeRecipe>> SUIT_UPGRADE_RECIPE =
            RECIPE_SERIALIZERS.register("suit_upgrade", () -> SuitUpgradeRecipe.SERIALIZER);

    private static DeferredItem<SuitModuleItem> module(String name, SuitUpgrade upgrade) {
        return ITEMS.registerItem(name, p -> new SuitModuleItem(upgrade, p), new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    private static DeferredItem<SpaceSuitItem> tierPiece(SuitTier tier, ArmorItem.Type type) {
        String piece = switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            default -> "boots";
        };
        String texture = switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "torso";
            case LEGGINGS -> "legs";
            default -> "boots";
        };
        boolean heavy = tier == SuitTier.RADIATION;
        return ITEMS.registerItem(tier.id() + "_" + piece, p -> new SpaceSuitItem(heavy ? HEAVY_SUIT : EVA_SUIT, type, tier, tier.id() + "_" + texture, p),
                new Item.Properties().durability(type.getDurability(heavy ? 33 : 26)).rarity(tier.level() >= 3 ? Rarity.RARE : Rarity.UNCOMMON));
    }
    public static final DeferredItem<ManeuveringUnitItem> MANEUVERING_UNIT = ITEMS.registerItem("maneuvering_unit",
            ManeuveringUnitItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<EnvironmentSensorItem> ENVIRONMENT_SENSOR = ITEMS.registerItem("environment_sensor",
            EnvironmentSensorItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<SafetyTetherItem> SAFETY_TETHER = ITEMS.registerItem("safety_tether",
            SafetyTetherItem::new, new Item.Properties().stacksTo(1));

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
    public static final DeferredBlock<com.pizzaparker07.spacebreaker.content.suit.block.SuitStationBlock> SUIT_STATION = BLOCKS.registerBlock("suit_station",
            com.pizzaparker07.spacebreaker.content.suit.block.SuitStationBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> OXYGEN_FILLER_ITEM = ITEMS.registerSimpleBlockItem(OXYGEN_FILLER);
    public static final DeferredItem<BlockItem> SUIT_STATION_ITEM = ITEMS.registerSimpleBlockItem(SUIT_STATION);
    public static final DeferredItem<BlockItem> OXYGEN_DISTRIBUTOR_ITEM = ITEMS.registerSimpleBlockItem(OXYGEN_DISTRIBUTOR);
    public static final DeferredItem<DoubleHighBlockItem> AIRLOCK_DOOR_ITEM = ITEMS.registerItem("airlock_door",
            p -> new DoubleHighBlockItem(AIRLOCK_DOOR.get(), p));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OxygenFillerBlockEntity>> OXYGEN_FILLER_BE =
            BLOCK_ENTITIES.register("oxygen_filler", () -> BlockEntityType.Builder.of(OxygenFillerBlockEntity::new, OXYGEN_FILLER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pizzaparker07.spacebreaker.content.suit.block.SuitStationBlockEntity>> SUIT_STATION_BE =
            BLOCK_ENTITIES.register("suit_station", () -> BlockEntityType.Builder.of(
                    com.pizzaparker07.spacebreaker.content.suit.block.SuitStationBlockEntity::new, SUIT_STATION.get()).build(null));
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
        RECIPE_SERIALIZERS.register(modBus);
        SbTabs.EQUIPMENT.icon(EVA_HELMET);
        ALL_PIECES.forEach(SbTabs.EQUIPMENT::add);
        SbTabs.EQUIPMENT.add(SUIT_UPGRADE_TEMPLATE).add(THERMAL_REGULATOR_MODULE).add(RADIATION_SHIELD_MODULE).add(EXTRA_OXYGEN_MODULE)
                .add(JETPACK_BOOSTER_MODULE)
                .addStack(() -> OxygenTankItem.filled(OXYGEN_TANK.get()))
                .addStack(() -> OxygenTankItem.filled(LARGE_OXYGEN_TANK.get()))
                .addStack(() -> ManeuveringUnitItem.filled(MANEUVERING_UNIT.get())).add(SAFETY_TETHER).add(ENVIRONMENT_SENSOR)
                .add(OXYGEN_FILLER_ITEM).add(SUIT_STATION_ITEM).add(OXYGEN_DISTRIBUTOR_ITEM).add(AIRLOCK_DOOR_ITEM);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        for (var type : List.of(OXYGEN_FILLER_BE.get(), OXYGEN_DISTRIBUTOR_BE.get(), SUIT_STATION_BE.get())) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> be.getItems());
        }
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, OXYGEN_FILLER_BE.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, SUIT_STATION_BE.get(), (be, side) -> be.getEnergy());
        for (var type : List.of(OXYGEN_FILLER_BE.get(), OXYGEN_DISTRIBUTOR_BE.get(), SUIT_STATION_BE.get())) {
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> be.getOxygenInput());
        }
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.add("itemGroup.spacebreaker.equipment", "Spacebreaker: Equipment", "Spacebreaker: Ekipman");
        lang.item(OXYGEN_TANK, "Oxygen Tank", "Oksijen Tüpü");
        lang.block(OXYGEN_FILLER, "Oxygen Filler", "Oksijen Dolum İstasyonu");
        lang.block(SUIT_STATION, "Suit Maintenance Station", "Giysi Bakım İstasyonu");
        lang.add("tooltip.spacebreaker.suit_station", "Stand on or beside it in a suit: tops up tanks and the Maneuvering Unit, slowly mends wear. Needs FE",
                "Giysiyle üstünde ya da yanında dur: tüpleri ve Manevra Ünitesini doldurur, aşınmayı yavaşça onarır. FE gerekir");
        lang.block(OXYGEN_DISTRIBUTOR, "Oxygen Distributor", "Oksijen Dağıtıcı");
        lang.block(AIRLOCK_DOOR, "Airlock Door", "Hava Kilidi Kapısı");
        lang.add("message.spacebreaker.machine.empty", "No tank inserted · %s / %s FE · piped oxygen %s",
                "Tüp takılı değil · %s / %s FE · borudan gelen oksijen %s");
        lang.add("message.spacebreaker.distributor.pressurising", "Pressurising room of %s blocks: %s%%", "%s bloklu oda basınçlandırılıyor: %%%s");
        lang.add("message.spacebreaker.distributor.scanning", "Checking the room for leaks...", "Oda sızıntıya karşı kontrol ediliyor...");
        lang.add("message.spacebreaker.distributor.leak", "Room is not sealed", "Oda sızdırmaz değil");
        lang.add("message.spacebreaker.distributor.leak_at", "Leak near %s %s %s (or the room is too large)", "Sızıntı: %s %s %s yakınında (ya da oda çok büyük)");
        lang.add("message.spacebreaker.distributor.no_oxygen", "Sealed room of %s blocks: insert an oxygen tank or pipe in oxygen",
                "%s bloklu sızdırmaz oda: oksijen tüpü tak ya da boruyla oksijen ver");
        lang.add("message.spacebreaker.distributor.sealed", "Sealed room of %s blocks · oxygen for %s", "%s bloklu sızdırmaz oda · %s yetecek oksijen");
        lang.add("tooltip.spacebreaker.oxygen_filler", "Refills tanks from a planet's breathable air, or from piped oxygen / liquid oxygen. Needs FE",
                "Tüpleri gezegenin solunabilir havasından ya da borudan gelen oksijen / sıvı oksijenden doldurur. FE gerekir");
        lang.add("tooltip.spacebreaker.oxygen_distributor", "Pressurises the sealed room around it from a tank or piped oxygen",
                "Çevresindeki sızdırmaz odayı tüpten ya da borudan gelen oksijenle doldurur");
        lang.add("tooltip.spacebreaker.airlock_door", "Airtight when closed. Opens by hand or redstone", "Kapalıyken hava geçirmez. Elle ya da kızıltaşla açılır");
        lang.item(LARGE_OXYGEN_TANK, "Large Oxygen Tank", "Büyük Oksijen Tüpü");
        lang.item(EVA_HELMET, "EVA Helmet", "EVA Kaskı");
        lang.item(EVA_CHESTPLATE, "EVA Torso", "EVA Gövdesi");
        lang.item(EVA_LEGGINGS, "EVA Leggings", "EVA Pantolonu");
        lang.item(EVA_BOOTS, "EVA Boots", "EVA Botları");
        lang.item(MAGNETIC_BOOTS, "Magnetic EVA Boots", "Manyetik EVA Botları");
        lang.item(MANEUVERING_UNIT, "Maneuvering Unit", "Manevra Ünitesi");
        lang.item(SAFETY_TETHER, "Safety Tether", "Güvenlik Halatı");
        lang.item(ENVIRONMENT_SENSOR, "Environment Sensor", "Çevre Sensörü");
        lang.add("tooltip.spacebreaker.environment_sensor", "Use to read pressure, air, temperature, radiation, gravity and ground",
                "Basınç, hava, sıcaklık, radyasyon, yerçekimi ve zemini okumak için kullan");
        lang.add("sensor.spacebreaker.title", "Environment reading", "Çevre ölçümü");
        lang.add("sensor.spacebreaker.title_at", "Environment reading: %s", "Çevre ölçümü: %s");
        lang.add("sensor.spacebreaker.pressure", "Pressure: %s kPa (%s bar)", "Basınç: %s kPa (%s bar)");
        lang.add("sensor.spacebreaker.no_air", "Air: none (vacuum)", "Hava: yok (vakum)");
        lang.add("sensor.spacebreaker.air", "Air: %s", "Hava: %s");
        lang.add("sensor.spacebreaker.temperature", "Temperature: %s °C (%s)", "Sıcaklık: %s °C (%s)");
        lang.add("sensor.spacebreaker.sunlit", "in sunlight", "güneşte");
        lang.add("sensor.spacebreaker.shade", "in shade", "gölgede");
        lang.add("sensor.spacebreaker.radiation", "Radiation: %s mSv/day", "Radyasyon: %s mSv/gün");
        lang.add("sensor.spacebreaker.gravity", "Gravity: %s g", "Yerçekimi: %s g");
        lang.add("sensor.spacebreaker.ground", "Ground: %s", "Zemin: %s");
        lang.add("sensor.spacebreaker.dust", "Panel dust: %s %%/day", "Panel tozu: günde %%%s");
        lang.add("tooltip.spacebreaker.safety_tether", "A %s-block line from your belt to a block: you cannot drift away",
                "Kemerden bir bloğa %s blokluk halat: uzaklaşıp sürüklenemezsin");
        lang.add("tooltip.spacebreaker.safety_tether.hint", "Use on a block to clip on, use again to unclip",
                "Takmak için bir bloğa kullan, çözmek için yeniden kullan");
        lang.add("hud.spacebreaker.mag", "MAG", "MANYETİK");
        lang.add("hud.spacebreaker.micro_g", "µG: TETHER!", "µG: İP TAK!");
        lang.add("message.spacebreaker.suit.micro_g", "Microgravity: use a tether or magnetic boots, or one step sends you drifting off",
                "Mikro yerçekimi: ip ya da manyetik bot kullan, yoksa tek adım seni uzaya savurur");
        lang.add("hud.spacebreaker.warn.strain", "%s BAR: SUIT UNDER STRAIN", "%s BAR: GİYSİ ZORLANIYOR");
        lang.add("key.spacebreaker.helmet_lamp", "Helmet lamps: wide / long beam", "Kask lambaları: geniş / uzun huzme");
        lang.add("message.spacebreaker.lamp.long", "Helmet lamps: long beam", "Kask lambaları: uzun huzme");
        lang.add("message.spacebreaker.lamp.wide", "Helmet lamps: wide beam", "Kask lambaları: geniş huzme");
        lang.add("tooltip.spacebreaker.suit.long_beam", "Lamps set to long beam", "Lambalar uzun huzmede");
        lang.add("hud.spacebreaker.cold_trap", "COLD TRAP", "SOĞUK TUZAK");
        lang.add("hud.spacebreaker.warn.cold_trap", "COLD TRAP: BEYOND SUIT RATING", "SOĞUK TUZAK: GİYSİ DAYANIMI AŞILDI");
        lang.add("hud.spacebreaker.warn.mmu_low", "MANEUVERING GAS LOW", "MANEVRA GAZI AZALDI");
        lang.add("hud.spacebreaker.pressurised", "Pressurised · helmet may come off", "Basınçlı ortam · kask çıkarılabilir");
        lang.add("message.spacebreaker.suit.pressurised", "Pressurised module: breathable air", "Basınçlı modül: solunabilir hava");
        lang.add("hud.spacebreaker.tether", "TETHER", "HALAT");
        lang.add("message.spacebreaker.tether.clipped", "Tether clipped · %s blocks of line", "Halat takıldı · %s blok uzunluk");
        lang.add("message.spacebreaker.tether.released", "Tether unclipped", "Halat çözüldü");
        lang.add("message.spacebreaker.tether.anchor_lost", "Tether anchor lost!", "Halatın bağlı olduğu blok gitti!");
        lang.add("message.spacebreaker.tether.snapped", "The tether clip let go!", "Halat kancası boşaldı!");
        String[][] tiers = {{"thermal", "Thermal", "Termal"}, {"heated", "Heated", "Isıtmalı"}, {"radiation", "Radiation-Hardened", "Radyasyon Zırhlı"}};
        String[][] pieces = {{"helmet", "Helmet", "Kaskı"}, {"chestplate", "Torso", "Gövdesi"}, {"leggings", "Leggings", "Pantolonu"}, {"boots", "Boots", "Botları"}};
        for (String[] t : tiers) {
            for (String[] p : pieces) {
                lang.add("item.spacebreaker." + t[0] + "_" + p[0], t[1] + " Suit " + p[1], t[2] + " Giysi " + p[2]);
            }
        }
        lang.item(SUIT_UPGRADE_TEMPLATE, "Suit Upgrade Template", "Giysi Yükseltme Şablonu");
        lang.item(THERMAL_REGULATOR_MODULE, "Thermal Regulator Module", "Termal Regülatör Modülü");
        lang.item(RADIATION_SHIELD_MODULE, "Radiation Shield Module", "Radyasyon Kalkanı Modülü");
        lang.item(EXTRA_OXYGEN_MODULE, "Extra Oxygen Module", "Ek Oksijen Modülü");
        lang.item(JETPACK_BOOSTER_MODULE, "Jetpack Booster Module", "Jetpack Güçlendirici Modülü");
        lang.add("upgrade.spacebreaker.thermal_regulator", "Thermal Regulator", "Termal Regülatör");
        lang.add("upgrade.spacebreaker.thermal_regulator.desc", "Widens the suit's rated range by 15 °C each way (stacks)",
                "Giysinin dayanım aralığını her iki yönde 15 °C genişletir (birikir)");
        lang.add("upgrade.spacebreaker.radiation_shield", "Radiation Shield", "Radyasyon Kalkanı");
        lang.add("upgrade.spacebreaker.radiation_shield.desc", "Cuts the radiation dose by a quarter (stacks)", "Radyasyon dozunu dörtte bir azaltır (birikir)");
        lang.add("upgrade.spacebreaker.extra_oxygen", "Extra Oxygen", "Ek Oksijen");
        lang.add("upgrade.spacebreaker.extra_oxygen.desc", "Torso only: a third tank slot in the backpack", "Yalnız gövde: sırt çantasına üçüncü tüp yuvası");
        lang.add("upgrade.spacebreaker.jetpack_booster", "Jetpack Booster", "Jetpack Güçlendirici");
        lang.add("upgrade.spacebreaker.jetpack_booster.desc", "Torso only: stronger Maneuvering Unit thrust for the same gas", "Yalnız gövde: aynı gazla daha güçlü Manevra Ünitesi itkisi");
        lang.add("tooltip.spacebreaker.module.hint", "Smithing table: Suit Upgrade Template + suit piece + module",
                "Demirci masası: Giysi Yükseltme Şablonu + giysi parçası + modül");
        lang.add("item.spacebreaker.suit_upgrade_template.desc", "Fits an upgrade module to a suit piece", "Giysi parçasına yükseltme modülü takar");
        lang.add("tooltip.spacebreaker.suit.shielding", "Radiation shielding: %s%%", "Radyasyon kalkanı: %%%s");
        lang.add("tooltip.spacebreaker.suit.upgrade", "Module: %s", "Modül: %s");
        lang.add("tooltip.spacebreaker.suit.needs_tech", "Full protection needs: %s", "Tam koruma için gereken: %s");
        lang.add("message.spacebreaker.suit.sealed", "Suit sealed · life support online", "Giysi sızdırmaz · yaşam desteği çalışıyor");
        lang.add("message.spacebreaker.suit.unsealed", "Suit unsealed", "Giysi açıldı");
        lang.add("message.spacebreaker.suit.unsealed_vacuum", "Suit unsealed: no air out here!", "Giysi açıldı: burada hava yok!");
        lang.add("hud.spacebreaker.warn.rating", "BEYOND SUIT RATING", "GİYSİ DAYANIMI AŞILDI");
        lang.add("tech.spacebreaker.thermal_suits", "Thermal Suits", "Termal Giysiler");
        lang.add("tech.spacebreaker.thermal_suits.desc", "Reflective gold-foil layers and liquid cooling for Mercury's day and the surface of Venus.",
                "Merkür gündüzü ve Venüs yüzeyi için yansıtıcı altın folyo katmanları ve sıvı soğutma.");
        lang.add("tech.spacebreaker.heated_suits", "Heated Suits", "Isıtmalı Giysiler");
        lang.add("tech.spacebreaker.heated_suits.desc", "Thick foam insulation and battery heaters for Titan's freezing nitrogen air.",
                "Titan'ın donduran azot havası için kalın köpük yalıtım ve bataryalı ısıtıcılar.");
        lang.add("tech.spacebreaker.radiation_hardened_suits", "Radiation-Hardened Suits", "Radyasyon Zırhlı Giysiler");
        lang.add("tech.spacebreaker.radiation_hardened_suits.desc", "Polyethylene and tungsten layers against the radiation belts of Jupiter.",
                "Jüpiter'in radyasyon kuşaklarına karşı polietilen ve tungsten katmanları.");
        lang.add("tech.spacebreaker.suit_upgrades", "Suit Upgrades", "Giysi Yükseltmeleri");
        lang.add("tech.spacebreaker.suit_upgrades.desc", "Upgrade modules fitted to suit pieces at a smithing table.",
                "Demirci masasında giysi parçalarına takılan yükseltme modülleri.");
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
        lang.add("hud.spacebreaker.open", "OPEN", "AÇIK");
        lang.add("hud.spacebreaker.warn.unsealed", "SUIT NOT SEALED", "GİYSİ SIZDIRMAZ DEĞİL");
        lang.add("hud.spacebreaker.warn.no_oxygen", "OXYGEN DEPLETED", "OKSİJEN BİTTİ");
        lang.add("hud.spacebreaker.warn.low_oxygen", "LOW OXYGEN", "OKSİJEN AZ");
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
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, SUIT_STATION.get(), OXYGEN_FILLER.get(), OXYGEN_DISTRIBUTOR.get(), AIRLOCK_DOOR.get());
        // station modules (rocketry): hull, windows and docking ports always hold air
        tags.block(AIRTIGHT, Blocks.IRON_TRAPDOOR, Blocks.IRON_DOOR,
                com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule.STATION_HULL.get(),
                com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule.STATION_WINDOW.get(),
                com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule.DOCKING_PORT.get());
        tags.blockTag(LEAKY, BlockTags.LEAVES);
        tags.item(ItemTags.HEAD_ARMOR, EVA_HELMET.get());
        tags.item(ItemTags.CHEST_ARMOR, EVA_CHESTPLATE.get());
        tags.item(ItemTags.LEG_ARMOR, EVA_LEGGINGS.get());
        tags.item(ItemTags.HEAD_ARMOR, THERMAL_HELMET.get(), HEATED_HELMET.get(), RADIATION_HELMET.get());
        tags.item(ItemTags.CHEST_ARMOR, THERMAL_CHESTPLATE.get(), HEATED_CHESTPLATE.get(), RADIATION_CHESTPLATE.get());
        tags.item(ItemTags.LEG_ARMOR, THERMAL_LEGGINGS.get(), HEATED_LEGGINGS.get(), RADIATION_LEGGINGS.get());
        tags.item(ItemTags.FOOT_ARMOR, EVA_BOOTS.get(), MAGNETIC_BOOTS.get(), THERMAL_BOOTS.get(), HEATED_BOOTS.get(), RADIATION_BOOTS.get());
        tags.item(ItemTags.DYEABLE, ALL_PIECES.stream().map(DeferredItem::get).toArray(Item[]::new));
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        loot.self(OXYGEN_FILLER.get());
        loot.self(SUIT_STATION.get());
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
