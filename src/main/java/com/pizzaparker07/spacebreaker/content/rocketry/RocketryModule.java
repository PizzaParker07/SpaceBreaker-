package com.pizzaparker07.spacebreaker.content.rocketry;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign;
import com.pizzaparker07.spacebreaker.content.rocketry.item.RocketItem;
import com.pizzaparker07.spacebreaker.content.rocketry.item.RocketPartItem;
import com.pizzaparker07.spacebreaker.content.rocketry.part.RocketParts;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import com.pizzaparker07.spacebreaker.content.rocketry.assembler.RocketAssemblerBlock;
import com.pizzaparker07.spacebreaker.content.rocketry.assembler.RocketAssemblerBlockEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.assembler.RocketAssemblerMenu;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.SpentStageEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.item.EmergencyBeaconItem;
import com.pizzaparker07.spacebreaker.content.rocketry.item.PropellantCartridgeItem;
import com.pizzaparker07.spacebreaker.content.rocketry.net.RocketActionPayload;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.CrewArmBlock;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.CrewArmBlockEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.FuelUmbilicalBlock;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.FuelUmbilicalBlockEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchConsoleBlock;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchConsoleBlockEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchConsoleMenu;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.PadBlock;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.ServiceTowerBlock;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/** Rocket parts, the assembler, the launch facility, rockets in flight and the orbit dimensions. */
public final class RocketryModule implements SbModule {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Spacebreaker.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Spacebreaker.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Spacebreaker.MOD_ID);

    public static final TagKey<Fluid> RP1_TAG = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "rp1"));
    public static final TagKey<Fluid> LOX_TAG = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "liquid_oxygen"));
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Spacebreaker.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RocketDesign>> ROCKET_DESIGN =
            COMPONENTS.registerComponentType("rocket_design", b -> b.persistent(RocketDesign.CODEC).networkSynchronized(RocketDesign.STREAM_CODEC));

    /** Every part item in creative-tab order. */
    public static final List<DeferredItem<RocketPartItem>> PARTS = new ArrayList<>();

    public static final DeferredItem<RocketPartItem> NOSE_CONE = part("nose_cone");
    public static final DeferredItem<RocketPartItem> CREW_CAPSULE = part("crew_capsule");
    public static final DeferredItem<RocketPartItem> HEAT_SHIELD = part("heat_shield");
    public static final DeferredItem<RocketPartItem> PARACHUTE = part("parachute");
    public static final DeferredItem<RocketPartItem> FUEL_TANK_SMALL = part("fuel_tank_small");
    public static final DeferredItem<RocketPartItem> FUEL_TANK_MEDIUM = part("fuel_tank_medium");
    public static final DeferredItem<RocketPartItem> FUEL_TANK_LARGE = part("fuel_tank_large");
    public static final DeferredItem<RocketPartItem> KEROSENE_ENGINE = part("kerosene_engine");
    public static final DeferredItem<RocketPartItem> VACUUM_ENGINE = part("vacuum_engine");
    public static final DeferredItem<RocketPartItem> METHALOX_ENGINE = part("methalox_engine");
    public static final DeferredItem<RocketPartItem> METHALOX_VACUUM_ENGINE = part("methalox_vacuum_engine");
    public static final DeferredItem<RocketPartItem> HYDROLOX_ENGINE = part("hydrolox_engine");
    public static final DeferredItem<RocketPartItem> HYDROLOX_VACUUM_ENGINE = part("hydrolox_vacuum_engine");
    public static final DeferredItem<RocketPartItem> CREW_CAPSULE_LARGE = part("crew_capsule_large");
    public static final DeferredItem<RocketPartItem> COMM_SATELLITE = part("comm_satellite");
    public static final DeferredItem<RocketPartItem> OBSERVATION_SATELLITE = part("observation_satellite");
    public static final DeferredItem<RocketPartItem> SPACE_PROBE = part("space_probe");
    public static final DeferredItem<RocketPartItem> DECOUPLER = part("decoupler");
    public static final DeferredItem<RocketPartItem> INTERSTAGE = part("interstage");
    public static final DeferredItem<RocketPartItem> FINS = part("fins");
    public static final DeferredItem<RocketPartItem> GRID_FINS = part("grid_fins");
    public static final DeferredItem<RocketPartItem> LANDING_LEGS = part("landing_legs");
    public static final DeferredItem<RocketPartItem> RCS_THRUSTERS = part("rcs_thrusters");

    public static final DeferredItem<RocketItem> ROCKET = ITEMS.registerItem("rocket", RocketItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<PropellantCartridgeItem> PROPELLANT_CARTRIDGE = ITEMS.registerItem("propellant_cartridge",
            PropellantCartridgeItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<EmergencyBeaconItem> EMERGENCY_BEACON = ITEMS.registerItem("emergency_beacon",
            EmergencyBeaconItem::new, new Item.Properties().stacksTo(4).rarity(Rarity.UNCOMMON));

    // ------------------------------------------------------------------ blocks
    private static BlockBehaviour.Properties steel() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<RocketAssemblerBlock> ROCKET_ASSEMBLER = BLOCKS.registerBlock("rocket_assembler", RocketAssemblerBlock::new,
            steel().noOcclusion());
    public static final DeferredBlock<PadBlock> LAUNCH_PAD = BLOCKS.registerBlock("launch_pad", PadBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(5.0F, 12.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<PadBlock> FLAME_TRENCH = BLOCKS.registerBlock("flame_trench", PadBlock::new, steel());
    public static final DeferredBlock<ServiceTowerBlock> SERVICE_TOWER = BLOCKS.registerBlock("service_tower", ServiceTowerBlock::new,
            steel().noOcclusion());
    public static final DeferredBlock<CrewArmBlock> CREW_ARM = BLOCKS.registerBlock("crew_access_arm", CrewArmBlock::new, steel().noOcclusion());
    public static final DeferredBlock<FuelUmbilicalBlock> FUEL_UMBILICAL = BLOCKS.registerBlock("fuel_umbilical", FuelUmbilicalBlock::new, steel().noOcclusion());
    public static final DeferredBlock<LaunchConsoleBlock> LAUNCH_CONSOLE = BLOCKS.registerBlock("launch_control_console", LaunchConsoleBlock::new,
            steel().noOcclusion().lightLevel(s -> 5));

    // space station
    private static BlockBehaviour.Properties hull() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    /** Pressurised hull panel: a full block, so rooms built of it hold air (oxygen distributor). */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> STATION_HULL = BLOCKS.registerSimpleBlock("station_hull", hull());
    /** Hull window: see out, air stays in. */
    public static final DeferredBlock<net.minecraft.world.level.block.TransparentBlock> STATION_WINDOW = BLOCKS.registerBlock("station_window",
            net.minecraft.world.level.block.TransparentBlock::new, hull().noOcclusion().isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
    public static final DeferredBlock<com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPortBlock> DOCKING_PORT = BLOCKS.registerBlock(
            "docking_port", com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPortBlock::new, hull().lightLevel(s -> 3));
    public static final DeferredItem<BlockItem> STATION_HULL_ITEM = ITEMS.registerSimpleBlockItem(STATION_HULL);
    public static final DeferredItem<BlockItem> STATION_WINDOW_ITEM = ITEMS.registerSimpleBlockItem(STATION_WINDOW);
    public static final DeferredItem<BlockItem> DOCKING_PORT_ITEM = ITEMS.registerSimpleBlockItem(DOCKING_PORT);
    public static final DeferredItem<com.pizzaparker07.spacebreaker.content.rocketry.station.StationKitItem> STATION_KIT = ITEMS.registerItem(
            "station_kit", com.pizzaparker07.spacebreaker.content.rocketry.station.StationKitItem::new, new Item.Properties().stacksTo(16));
    /** Strapped-down sleeping bag: sets the respawn point anywhere, beds do not work off Earth. */
    public static final DeferredBlock<com.pizzaparker07.spacebreaker.content.rocketry.station.StationBunkBlock> STATION_BUNK = BLOCKS.registerBlock(
            "station_bunk", com.pizzaparker07.spacebreaker.content.rocketry.station.StationBunkBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.8F).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredBlock<com.pizzaparker07.spacebreaker.content.rocketry.station.FuelDepotBlock> FUEL_DEPOT = BLOCKS.registerBlock(
            "fuel_depot", com.pizzaparker07.spacebreaker.content.rocketry.station.FuelDepotBlock::new, hull());
    public static final DeferredItem<BlockItem> STATION_BUNK_ITEM = ITEMS.registerSimpleBlockItem(STATION_BUNK);
    public static final DeferredItem<BlockItem> FUEL_DEPOT_ITEM = ITEMS.registerSimpleBlockItem(FUEL_DEPOT);
    public static final DeferredItem<com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem> HABITAT_KIT = ITEMS.registerItem(
            "habitat_kit", com.pizzaparker07.spacebreaker.content.rocketry.station.HabitatKitItem::new, new Item.Properties().stacksTo(4));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pizzaparker07.spacebreaker.content.rocketry.station.FuelDepotBlockEntity>> FUEL_DEPOT_BE =
            BLOCK_ENTITIES.register("fuel_depot", () -> BlockEntityType.Builder.of(
                    com.pizzaparker07.spacebreaker.content.rocketry.station.FuelDepotBlockEntity::new, FUEL_DEPOT.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPortBlockEntity>> DOCKING_PORT_BE =
            BLOCK_ENTITIES.register("docking_port", () -> BlockEntityType.Builder.of(
                    com.pizzaparker07.spacebreaker.content.rocketry.station.DockingPortBlockEntity::new, DOCKING_PORT.get()).build(null));

    public static final DeferredItem<BlockItem> ROCKET_ASSEMBLER_ITEM = ITEMS.registerSimpleBlockItem(ROCKET_ASSEMBLER);
    public static final DeferredItem<BlockItem> LAUNCH_PAD_ITEM = ITEMS.registerSimpleBlockItem(LAUNCH_PAD);
    public static final DeferredItem<BlockItem> FLAME_TRENCH_ITEM = ITEMS.registerSimpleBlockItem(FLAME_TRENCH);
    public static final DeferredItem<BlockItem> SERVICE_TOWER_ITEM = ITEMS.registerSimpleBlockItem(SERVICE_TOWER);
    public static final DeferredItem<BlockItem> CREW_ARM_ITEM = ITEMS.registerSimpleBlockItem(CREW_ARM);
    public static final DeferredItem<BlockItem> FUEL_UMBILICAL_ITEM = ITEMS.registerSimpleBlockItem(FUEL_UMBILICAL);
    public static final DeferredItem<BlockItem> LAUNCH_CONSOLE_ITEM = ITEMS.registerSimpleBlockItem(LAUNCH_CONSOLE);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RocketAssemblerBlockEntity>> ASSEMBLER_BE = BLOCK_ENTITIES.register("rocket_assembler",
            () -> BlockEntityType.Builder.of(RocketAssemblerBlockEntity::new, ROCKET_ASSEMBLER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrewArmBlockEntity>> CREW_ARM_BE = BLOCK_ENTITIES.register("crew_access_arm",
            () -> BlockEntityType.Builder.of(CrewArmBlockEntity::new, CREW_ARM.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelUmbilicalBlockEntity>> FUEL_UMBILICAL_BE = BLOCK_ENTITIES.register("fuel_umbilical",
            () -> BlockEntityType.Builder.of(FuelUmbilicalBlockEntity::new, FUEL_UMBILICAL.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaunchConsoleBlockEntity>> LAUNCH_CONSOLE_BE = BLOCK_ENTITIES.register("launch_control_console",
            () -> BlockEntityType.Builder.of(LaunchConsoleBlockEntity::new, LAUNCH_CONSOLE.get()).build(null));

    // ------------------------------------------------------------------ entities and menus
    public static final DeferredHolder<EntityType<?>, EntityType<RocketEntity>> ROCKET_ENTITY = ENTITIES.register("rocket",
            () -> EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC).sized(1.8F, 4F)
                    .clientTrackingRange(32).updateInterval(1).fireImmune().build("rocket"));
    public static final DeferredHolder<EntityType<?>, EntityType<SpentStageEntity>> SPENT_STAGE = ENTITIES.register("spent_stage",
            () -> EntityType.Builder.<SpentStageEntity>of(SpentStageEntity::new, MobCategory.MISC).sized(1.8F, 2F)
                    .clientTrackingRange(24).updateInterval(2).fireImmune().noSave().build("spent_stage"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.pizzaparker07.spacebreaker.content.rocketry.mission.SatelliteEntity>> SATELLITE_ENTITY =
            ENTITIES.register("satellite", () -> EntityType.Builder.<com.pizzaparker07.spacebreaker.content.rocketry.mission.SatelliteEntity>of(
                    com.pizzaparker07.spacebreaker.content.rocketry.mission.SatelliteEntity::new, MobCategory.MISC).sized(1.5F, 1.5F)
                    .clientTrackingRange(16).updateInterval(20).fireImmune().build("satellite"));

    public static final DeferredHolder<MenuType<?>, MenuType<RocketAssemblerMenu>> ASSEMBLER_MENU = MENUS.register("rocket_assembler",
            () -> IMenuTypeExtension.create(RocketAssemblerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<LaunchConsoleMenu>> LAUNCH_CONSOLE_MENU = MENUS.register("launch_control_console",
            () -> IMenuTypeExtension.create(LaunchConsoleMenu::new));

    private static DeferredItem<RocketPartItem> part(String name) {
        DeferredItem<RocketPartItem> item = ITEMS.registerItem(name, RocketPartItem::new, new Item.Properties().stacksTo(16));
        PARTS.add(item);
        return item;
    }

    @Override
    public String id() {
        return "rocketry";
    }

    @Override
    public void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(RocketParts::registerRegistry);
        modBus.addListener(RocketryModule::capabilities);
        modBus.addListener(RocketryModule::payloads);
        NeoForge.EVENT_BUS.addListener(RocketryModule::onMount);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
                var data = com.pizzaparker07.spacebreaker.content.rocketry.mission.SatelliteData.get(sp.server);
                data.flush(sp);
                com.pizzaparker07.spacebreaker.content.rocketry.mission.CoveragePayload.send(sp,
                        com.pizzaparker07.spacebreaker.content.rocketry.mission.CoveragePayload.of(sp.server));
                com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.send(sp,
                        new com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.Payload(
                                com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.of(sp.server)));
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) ->
                com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) ->
                com.pizzaparker07.spacebreaker.content.rocketry.nav.AstroTransferModel.install(e.getServer().registryAccess()));
        SbTabs.SPACEFLIGHT.icon(CREW_CAPSULE);
        SbTabs.SPACEFLIGHT.add(ROCKET_ASSEMBLER_ITEM).add(LAUNCH_PAD_ITEM).add(FLAME_TRENCH_ITEM).add(SERVICE_TOWER_ITEM)
                .add(CREW_ARM_ITEM).add(FUEL_UMBILICAL_ITEM).add(LAUNCH_CONSOLE_ITEM);
        PARTS.forEach(SbTabs.SPACEFLIGHT::add);
        SbTabs.SPACEFLIGHT.add(STATION_KIT).add(HABITAT_KIT).add(STATION_HULL_ITEM).add(STATION_WINDOW_ITEM).add(DOCKING_PORT_ITEM)
                .add(STATION_BUNK_ITEM).add(FUEL_DEPOT_ITEM);
        SbTabs.SPACEFLIGHT.addStack(RocketryModule::exampleRocket);
        SbTabs.SPACEFLIGHT.add(PROPELLANT_CARTRIDGE).add(EMERGENCY_BEACON);
    }

    /** A ready-made three-stage Moon rocket (creative tab, showcase, tests). */
    public static final com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign MOON_ROCKET =
            new com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign(java.util.List.of(
                    Spacebreaker.id("kerosene_engine"), Spacebreaker.id("fuel_tank_large"), Spacebreaker.id("fuel_tank_large"),
                    Spacebreaker.id("fuel_tank_large"), Spacebreaker.id("fins"), Spacebreaker.id("interstage"),
                    Spacebreaker.id("vacuum_engine"), Spacebreaker.id("fuel_tank_large"), Spacebreaker.id("decoupler"),
                    Spacebreaker.id("vacuum_engine"), Spacebreaker.id("fuel_tank_small"), Spacebreaker.id("landing_legs"),
                    Spacebreaker.id("heat_shield"), Spacebreaker.id("crew_capsule"), Spacebreaker.id("parachute")));

    private static net.minecraft.world.item.ItemStack exampleRocket() {
        return com.pizzaparker07.spacebreaker.content.rocketry.item.RocketItem.of(MOON_ROCKET);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FUEL_UMBILICAL_BE.get(), (be, side) -> be.handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DOCKING_PORT_BE.get(), (be, side) -> be.handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FUEL_DEPOT_BE.get(), (be, side) -> be.fluids());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FUEL_DEPOT_BE.get(), (be, side) -> be.energy());
        event.registerEntity(Capabilities.FluidHandler.ENTITY, ROCKET_ENTITY.get(), (rocket, ctx) -> new com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketFluidHandler(rocket));
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        var reg = event.registrar("1");
        reg.playToServer(RocketActionPayload.TYPE, RocketActionPayload.STREAM_CODEC, RocketActionPayload::handle);
        reg.playToServer(com.pizzaparker07.spacebreaker.content.rocketry.net.NearbyStarsPayload.Request.TYPE,
                com.pizzaparker07.spacebreaker.content.rocketry.net.NearbyStarsPayload.Request.STREAM_CODEC,
                com.pizzaparker07.spacebreaker.content.rocketry.net.NearbyStarsPayload.Request::handle);
        reg.playToClient(com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.Payload.TYPE,
                com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.Payload.STREAM_CODEC,
                com.pizzaparker07.spacebreaker.content.rocketry.station.Stations.Payload::handle);
        reg.playToClient(com.pizzaparker07.spacebreaker.content.rocketry.mission.CoveragePayload.TYPE,
                com.pizzaparker07.spacebreaker.content.rocketry.mission.CoveragePayload.STREAM_CODEC,
                com.pizzaparker07.spacebreaker.content.rocketry.mission.CoveragePayload::handle);
        reg.playToClient(com.pizzaparker07.spacebreaker.content.rocketry.net.NearbyStarsPayload.TYPE,
                com.pizzaparker07.spacebreaker.content.rocketry.net.NearbyStarsPayload.STREAM_CODEC,
                com.pizzaparker07.spacebreaker.content.rocketry.net.NearbyStarsPayload::handle);
    }

    /** Nobody climbs out of a rocket in flight (there is nothing to stand on up there). */
    private static void onMount(EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityBeingMounted() instanceof RocketEntity rocket
                && !event.getEntityMounting().isRemoved() && event.getEntityMounting().isAlive()
                && !rocket.isRemoved() && !rocket.canDismount()
                && !(event.getEntityMounting() instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator()))) {
            event.setCanceled(true);
        }
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        loot.self(ROCKET_ASSEMBLER.get());
        loot.self(LAUNCH_PAD.get());
        loot.self(FLAME_TRENCH.get());
        loot.self(SERVICE_TOWER.get());
        loot.self(CREW_ARM.get());
        loot.self(FUEL_UMBILICAL.get());
        loot.self(LAUNCH_CONSOLE.get());
        loot.self(STATION_HULL.get());
        loot.self(STATION_WINDOW.get());
        loot.self(DOCKING_PORT.get());
        loot.self(STATION_BUNK.get());
        loot.self(FUEL_DEPOT.get());
    }

    @Override
    public void addTags(SbTags tags) {
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, ROCKET_ASSEMBLER.get(), LAUNCH_PAD.get(), FLAME_TRENCH.get(), SERVICE_TOWER.get(),
                CREW_ARM.get(), FUEL_UMBILICAL.get(), LAUNCH_CONSOLE.get(), STATION_HULL.get(), STATION_WINDOW.get(), DOCKING_PORT.get(), FUEL_DEPOT.get());
        tags.block(BlockTags.NEEDS_STONE_TOOL, LAUNCH_PAD.get(), FLAME_TRENCH.get(), SERVICE_TOWER.get());
    }

    @Override
    public void addTranslations(Lang lang) {
        RocketryLang.add(lang);
    }
}
