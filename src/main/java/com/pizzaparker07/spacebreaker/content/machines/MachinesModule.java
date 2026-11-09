package com.pizzaparker07.spacebreaker.content.machines;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineLayout;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineMenu;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineSpec;
import com.pizzaparker07.spacebreaker.content.machines.base.ProcessingMachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.base.TankSpec;
import com.pizzaparker07.spacebreaker.content.machines.machine.ArcFurnaceBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.BatteryBankBlock;
import com.pizzaparker07.spacebreaker.content.machines.machine.BatteryBankBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.CombustionGeneratorBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.TankBlock;
import com.pizzaparker07.spacebreaker.content.machines.machine.AtmosphereProcessorBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.SabatierReactorBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.PumpjackBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.RefineryBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.multiblock.MultiblockMachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.multiblock.PartBlock;
import com.pizzaparker07.spacebreaker.content.machines.multiblock.PartBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.multiblock.PartShape;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.machines.machine.TankBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitBlock;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.net.ConduitType;
import com.pizzaparker07.spacebreaker.content.machines.recipe.ProcessingRecipe;
import com.pizzaparker07.spacebreaker.core.SbTabs;
import com.pizzaparker07.spacebreaker.core.data.SbBlockLoot;
import com.pizzaparker07.spacebreaker.core.data.SbTags;
import com.pizzaparker07.spacebreaker.core.lang.Lang;
import com.pizzaparker07.spacebreaker.core.module.SbModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Power generation, storage, transport and the processing chain from ore to rocket propellant. */
public final class MachinesModule implements SbModule {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Spacebreaker.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Spacebreaker.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Spacebreaker.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Spacebreaker.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Spacebreaker.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Spacebreaker.MOD_ID);
    /** Burn left in a partly used nuclear fuel element (ticks of full reactor power); absent on fresh elements. */
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Integer>> FUEL_BURN_LEFT =
            COMPONENTS.registerComponentType("fuel_burn_left", b -> b.persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    /** Game time an RTG pellet started decaying. */
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Long>> DECAY_START =
            COMPONENTS.registerComponentType("decay_start", b -> b.persistent(com.mojang.serialization.Codec.LONG)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG));

    /** Every machine with a block entity, for capability registration and lang. */
    public static final List<MachineEntry<?, ?>> MACHINES = new ArrayList<>();

    // ------------------------------------------------------------------ recipe types (public API for other areas)
    public static final ProcessingRecipe.Kind CRUSHING = kind("crushing");
    public static final ProcessingRecipe.Kind ARC_SMELTING = kind("arc_smelting");
    public static final ProcessingRecipe.Kind PRESSING = kind("pressing");
    /** Rocket and suit parts from plates and circuits. Rocketry and Suit add their own recipes of this type. */
    public static final ProcessingRecipe.Kind FABRICATING = kind("fabricating");
    public static final ProcessingRecipe.Kind REFINING = kind("refining");
    public static final ProcessingRecipe.Kind ELECTROLYSIS = kind("electrolysis");
    public static final ProcessingRecipe.Kind LIQUEFACTION = kind("liquefaction");
    /** In-situ resource utilisation (off-world industry). */
    public static final ProcessingRecipe.Kind SABATIER = kind("sabatier");
    public static final ProcessingRecipe.Kind REGOLITH_PROCESSING = kind("regolith_processing");
    public static final ProcessingRecipe.Kind MELTING = kind("melting");
    /** General chemistry: one item and up to two fluids in. Planet areas add their own recipes of this type. */
    public static final ProcessingRecipe.Kind CHEMICAL_REACTION = kind("chemical_reaction");
    /** Regolith fused into ceramic building blocks (no air needed, unlike a furnace's fire). */
    public static final ProcessingRecipe.Kind SINTERING = kind("sintering");
    public static final ProcessingRecipe.Kind CENTRIFUGING = kind("centrifuging");

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE_MENU =
            MENUS.register("machine", () -> IMenuTypeExtension.create(MachineMenu::client));

    // ------------------------------------------------------------------ power
    public static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL = BLOCKS.registerBlock("solar_panel",
            SolarPanelBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(2.0F, 6.0F)
                    .sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> SOLAR_PANEL_ITEM = ITEMS.registerSimpleBlockItem(SOLAR_PANEL);
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_BE =
            BLOCK_ENTITIES.register("solar_panel", () -> BlockEntityType.Builder.of(SolarPanelBlockEntity::new, SOLAR_PANEL.get()).build(null));

    public static final DeferredBlock<SolarArrayBlock> SOLAR_ARRAY = BLOCKS.registerBlock("solar_array",
            SolarArrayBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(2.0F, 6.0F)
                    .sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> SOLAR_ARRAY_ITEM = ITEMS.registerSimpleBlockItem(SOLAR_ARRAY);
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_ARRAY_BE =
            BLOCK_ENTITIES.register("solar_array", () -> BlockEntityType.Builder.<SolarPanelBlockEntity>of(
                    (pos, state) -> new SolarPanelBlockEntity(MachinesModule.SOLAR_ARRAY_BE.get(), pos, state, SolarArrayBlock.PEAK_OUTPUT),
                    SOLAR_ARRAY.get()).build(null));

    public static final DeferredBlock<PowerMonitorBlock> POWER_MONITOR = BLOCKS.registerBlock("power_monitor", PowerMonitorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> POWER_MONITOR_ITEM = ITEMS.registerSimpleBlockItem(POWER_MONITOR);
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerMonitorBlockEntity>> POWER_MONITOR_BE =
            BLOCK_ENTITIES.register("power_monitor", () -> BlockEntityType.Builder.of(PowerMonitorBlockEntity::new, POWER_MONITOR.get()).build(null));

    public static final MachineEntry<MachineBlock, CombustionGeneratorBlockEntity> COMBUSTION_GENERATOR =
            machine("combustion_generator", CombustionGeneratorBlockEntity::new, null, 13);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR_BE = COMBUSTION_GENERATOR.type();

    /** Radioisotope thermoelectric generator: small, steady power where the sun is weak or gone. */
    public static final MachineEntry<MachineBlock, com.pizzaparker07.spacebreaker.content.machines.machine.RtgBlockEntity> RTG =
            machine("rtg", com.pizzaparker07.spacebreaker.content.machines.machine.RtgBlockEntity::new,
                    net.minecraft.world.phys.shapes.Shapes.or(Block.box(0, 0, 0, 16, 2, 16), Block.box(1, 2, 1, 15, 14, 15), Block.box(3, 14, 3, 13, 16, 13)), 0);
    /** Fission surface power: a buried kilowatt-class fission reactor for big outposts; radiates while it runs. */
    public static final MachineEntry<MachineBlock, com.pizzaparker07.spacebreaker.content.machines.machine.FissionReactorBlockEntity> FISSION_REACTOR =
            machine("fission_reactor", com.pizzaparker07.spacebreaker.content.machines.machine.FissionReactorBlockEntity::new,
                    net.minecraft.world.phys.shapes.Shapes.or(Block.box(2, 0, 2, 14, 9, 14), Block.box(0, 9, 7, 16, 16, 9), Block.box(7, 9, 0, 9, 16, 16)), 0);
    /** Uranium oxide concentrate leached from crushed uraninite: the feed for enrichment and for RTG pellets. */
    public static final DeferredItem<net.minecraft.world.item.Item> YELLOWCAKE = ITEMS.registerSimpleItem("yellowcake");
    /** Uranium with more of the fissile isotope: reactor fuel needs it. */
    public static final DeferredItem<net.minecraft.world.item.Item> ENRICHED_URANIUM = ITEMS.registerSimpleItem("enriched_uranium",
            new net.minecraft.world.item.Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    /** What the centrifuge leaves behind: dense, barely radioactive (armour, ballast). */
    public static final DeferredItem<net.minecraft.world.item.Item> DEPLETED_URANIUM = ITEMS.registerSimpleItem("depleted_uranium");
    /** Pressed depleted uranium: about twice as dense as lead, so a thin plate stops what a thick lead one would. */
    public static final DeferredItem<net.minecraft.world.item.Item> DEPLETED_URANIUM_PLATE = ITEMS.registerSimpleItem("depleted_uranium_plate");
    /** Fuel for a nuclear thermal rocket engine (rocketry); listed in {@link IsruRecipes#REACTOR_FUEL}. */
    public static final DeferredItem<NuclearFuelElementItem> NUCLEAR_FUEL_ELEMENT = ITEMS.registerItem("nuclear_fuel_element", NuclearFuelElementItem::new,
            new net.minecraft.world.item.Item.Properties().stacksTo(4).rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<RtgFuelPelletItem> RTG_FUEL_PELLET = ITEMS.registerItem("rtg_fuel_pellet", RtgFuelPelletItem::new,
            new net.minecraft.world.item.Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON));

    public static final MachineEntry<BatteryBankBlock, BatteryBankBlockEntity> BATTERY_BANK =
            entry("battery_bank", BatteryBankBlock::new, BatteryBankBlockEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBankBlockEntity>> BATTERY_BANK_BE = BATTERY_BANK.type();

    // ------------------------------------------------------------------ processing
    public static final MachineEntry<MachineBlock, ProcessingMachineBlockEntity> CRUSHER = processing("crusher", () -> CRUSHING,
            new MachineLayout().energy(10, 17).slot(52, 34, MachineLayout.Role.INPUT).progress(78, 35, MachineLayout.Progress.ARROW)
                    .slot(110, 34, MachineLayout.Role.OUTPUT).slot(132, 34, MachineLayout.Role.OUTPUT),
            20_000, List.of(), 0);
    public static final MachineEntry<MachineBlock, ProcessingMachineBlockEntity> ARC_FURNACE = processingWith("arc_furnace", () -> ARC_SMELTING,
            new MachineLayout().energy(10, 17).slot(30, 34, MachineLayout.Role.INPUT).slot(48, 34, MachineLayout.Role.INPUT)
                    .slot(66, 34, MachineLayout.Role.INPUT).progress(90, 35, MachineLayout.Progress.ARROW).slot(124, 34, MachineLayout.Role.OUTPUT),
            60_000, List.of(), 14, ArcFurnaceBlockEntity::new);
    public static final MachineEntry<MachineBlock, ProcessingMachineBlockEntity> PRESS = processing("press", () -> PRESSING,
            new MachineLayout().energy(10, 17).slot(52, 24, MachineLayout.Role.INPUT).slot(52, 46, MachineLayout.Role.TOOL)
                    .progress(80, 35, MachineLayout.Progress.ARROW).slot(116, 34, MachineLayout.Role.OUTPUT),
            20_000, List.of(), 0);
    public static final MachineEntry<MachineBlock, ProcessingMachineBlockEntity> FABRICATOR = processing("fabricator", () -> FABRICATING,
            grid(new MachineLayout().energy(10, 17), 30, 16).progress(94, 35, MachineLayout.Progress.ARROW).slot(128, 34, MachineLayout.Role.OUTPUT),
            40_000, List.of(), 0);
    public static final MachineEntry<MachineBlock, ProcessingMachineBlockEntity> ELECTROLYZER = processingWith("electrolyzer", () -> ELECTROLYSIS,
            new MachineLayout().energy(10, 17).tank(44, 17).progress(70, 35, MachineLayout.Progress.ARROW).tank(104, 17).tank(128, 17),
            40_000, List.of(TankSpec.in(8_000, f -> f.is(Tags.Fluids.WATER)), TankSpec.out(8_000), TankSpec.out(8_000)), 0,
            com.pizzaparker07.spacebreaker.content.machines.machine.ElectrolyzerBlockEntity::new);
    public static final MachineEntry<MachineBlock, ProcessingMachineBlockEntity> LIQUEFIER = processing("cryogenic_liquefier", () -> LIQUEFACTION,
            new MachineLayout().energy(10, 17).tank(50, 17).progress(78, 35, MachineLayout.Progress.ARROW).tank(110, 17),
            // only gases it can liquefy: a stray gas (helium-3 from a regolith heater) must not jam the input tank
            60_000, List.of(TankSpec.in(8_000, f -> f.is(FluidsModule.TAG_OXYGEN) || f.is(FluidsModule.TAG_HYDROGEN) || f.is(FluidsModule.TAG_METHANE)),
                    TankSpec.out(8_000)), 0);

    // ------------------------------------------------------------------ multiblocks
    public static final DeferredBlock<PartBlock> PART = BLOCKS.registerBlock("machine_part", PartBlock::new,
            machineProps().noLootTable());
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PartBlockEntity>> PART_BE =
            BLOCK_ENTITIES.register("machine_part", () -> BlockEntityType.Builder.of(PartBlockEntity::new, PART.get()).build(null));

    public static final MachineEntry<MultiblockMachineBlock, PumpjackBlockEntity> PUMPJACK = multiblock("pumpjack",
            PumpjackBlockEntity::new, net.minecraft.world.level.block.Block.box(2, 0, 0, 14, 16, 16), List.of(
                    new MultiblockMachineBlock.Part(0, 0, -1, PartShape.PUMPJACK_WELL),
                    new MultiblockMachineBlock.Part(0, 0, 1, PartShape.PUMPJACK_REAR),
                    new MultiblockMachineBlock.Part(0, 1, 0, PartShape.PUMPJACK_POST),
                    new MultiblockMachineBlock.Part(0, 2, 0, PartShape.PUMPJACK_BEAM),
                    new MultiblockMachineBlock.Part(0, 2, -1, PartShape.PUMPJACK_HEAD),
                    new MultiblockMachineBlock.Part(0, 2, 1, PartShape.PUMPJACK_BEAM)));

    @SuppressWarnings("unchecked")
    public static final MachineEntry<MultiblockMachineBlock, ProcessingMachineBlockEntity> REFINERY = refinery();

    // ------------------------------------------------------------------ tanks
    public static final DeferredBlock<TankBlock> FLUID_TANK = tank("fluid_tank", TankBlock.Kind.FLUID, MapColor.METAL);
    public static final DeferredBlock<TankBlock> PRESSURIZED_TANK = tank("pressurized_tank", TankBlock.Kind.PRESSURIZED, MapColor.SNOW);
    public static final DeferredBlock<TankBlock> CRYOGENIC_TANK = tank("cryogenic_tank", TankBlock.Kind.CRYOGENIC, MapColor.COLOR_ORANGE);
    public static final List<DeferredBlock<TankBlock>> TANKS = List.of(FLUID_TANK, PRESSURIZED_TANK, CRYOGENIC_TANK);
    public static final List<DeferredItem<BlockItem>> TANK_ITEMS = TANKS.stream().map(ITEMS::registerSimpleBlockItem).toList();
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK_BE =
            BLOCK_ENTITIES.register("tank", () -> BlockEntityType.Builder.of(TankBlockEntity::new,
                    TANKS.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    // ------------------------------------------------------------------ off-world industry (ISRU)
    public static final MachineEntry<MachineBlock, AtmosphereProcessorBlockEntity> ATMOSPHERE_PROCESSOR =
            machine("atmosphere_processor", AtmosphereProcessorBlockEntity::new, null, 0);
    public static final MachineEntry<MachineBlock, ProcessingMachineBlockEntity> SABATIER_REACTOR = processingWith("sabatier_reactor", () -> SABATIER,
            new MachineLayout().energy(10, 17).tank(30, 17).tank(52, 17).progress(76, 35, MachineLayout.Progress.ARROW).tank(108, 17).tank(130, 17),
            40_000, List.of(TankSpec.in(8_000, f -> f.getFluidType().isLighterThanAir()), TankSpec.in(8_000, f -> f.getFluidType().isLighterThanAir()),
                    TankSpec.out(8_000), TankSpec.out(8_000)), 9, SabatierReactorBlockEntity::new);
    public static final MachineEntry<MachineBlock, ProcessingMachineBlockEntity> REGOLITH_HEATER = processing("regolith_heater", () -> REGOLITH_PROCESSING,
            new MachineLayout().energy(10, 17).slot(40, 34, MachineLayout.Role.INPUT).progress(64, 35, MachineLayout.Progress.ARROW)
                    .slot(94, 34, MachineLayout.Role.OUTPUT).tank(120, 17).tank(142, 17),
            60_000, List.of(TankSpec.out(8_000), TankSpec.out(8_000)), 12);
    public static final MachineEntry<MachineBlock, ProcessingMachineBlockEntity> SINTERING_FURNACE = processing("sintering_furnace", () -> SINTERING,
            new MachineLayout().energy(10, 17).slot(52, 34, MachineLayout.Role.INPUT).progress(78, 35, MachineLayout.Progress.FLAME)
                    .slot(110, 34, MachineLayout.Role.OUTPUT),
            30_000, List.of(), 13);
    /** Uranium enrichment: yellowcake in, a little enriched and a lot of depleted uranium out; faster in a cascade. */
    public static final MachineEntry<MachineBlock, ProcessingMachineBlockEntity> GAS_CENTRIFUGE = processingWith("gas_centrifuge", () -> CENTRIFUGING,
            new MachineLayout().energy(10, 17).slot(52, 34, MachineLayout.Role.INPUT).progress(78, 35, MachineLayout.Progress.ARROW)
                    .slot(110, 34, MachineLayout.Role.OUTPUT).slot(132, 34, MachineLayout.Role.OUTPUT),
            120_000, List.of(), 0, com.pizzaparker07.spacebreaker.content.machines.machine.GasCentrifugeBlockEntity::new);
    /** Digs the ground below and feeds the machines beside it: the start of an automatic ISRU line. */
    public static final MachineEntry<MachineBlock, com.pizzaparker07.spacebreaker.content.machines.machine.RegolithExcavatorBlockEntity> REGOLITH_EXCAVATOR =
            machine("regolith_excavator", com.pizzaparker07.spacebreaker.content.machines.machine.RegolithExcavatorBlockEntity::new, null, 0);
    public static final MachineEntry<MachineBlock, ProcessingMachineBlockEntity> ICE_MELTER = processing("ice_melter", () -> MELTING,
            new MachineLayout().energy(10, 17).slot(52, 34, MachineLayout.Role.INPUT).progress(78, 35, MachineLayout.Progress.ARROW)
                    .tank(110, 17).tank(132, 17),
            20_000, List.of(TankSpec.out(16_000), TankSpec.out(8_000)), 0);

    public static final MachineEntry<MachineBlock, ProcessingMachineBlockEntity> CHEMICAL_REACTOR = processing("chemical_reactor", () -> CHEMICAL_REACTION,
            new MachineLayout().energy(10, 17).slot(30, 34, MachineLayout.Role.INPUT).tank(52, 17).tank(72, 17)
                    .progress(94, 35, MachineLayout.Progress.ARROW).tank(122, 17).slot(146, 34, MachineLayout.Role.OUTPUT),
            40_000, List.of(TankSpec.in(8_000, f -> true), TankSpec.in(8_000, f -> true), TankSpec.out(8_000)), 0);

    // ------------------------------------------------------------------ cables and pipes
    public static final List<DeferredBlock<ConduitBlock>> CONDUITS = new ArrayList<>();
    public static final DeferredBlock<ConduitBlock> COPPER_CABLE = conduit(ConduitType.COPPER_CABLE, SoundType.COPPER);
    public static final DeferredBlock<ConduitBlock> HV_CABLE = conduit(ConduitType.HV_CABLE, SoundType.METAL);
    public static final DeferredBlock<ConduitBlock> FLUID_PIPE = conduit(ConduitType.FLUID_PIPE, SoundType.METAL);
    public static final DeferredBlock<ConduitBlock> GAS_PIPE = conduit(ConduitType.GAS_PIPE, SoundType.COPPER);
    public static final DeferredBlock<ConduitBlock> ITEM_DUCT = conduit(ConduitType.ITEM_DUCT, SoundType.METAL);
    public static final List<DeferredItem<BlockItem>> CONDUIT_ITEMS = CONDUITS.stream().map(ITEMS::registerSimpleBlockItem).toList();
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConduitBlockEntity>> CONDUIT_BE =
            BLOCK_ENTITIES.register("conduit", () -> BlockEntityType.Builder.of(ConduitBlockEntity::new,
                    CONDUITS.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    // ------------------------------------------------------------------ registration helpers

    private static ProcessingRecipe.Kind kind(String name) {
        ProcessingRecipe.Kind[] holder = new ProcessingRecipe.Kind[1];
        DeferredHolder<RecipeType<?>, RecipeType<ProcessingRecipe>> type =
                RECIPE_TYPES.register(name, () -> RecipeType.simple(Spacebreaker.id(name)));
        DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ProcessingRecipe>> serializer =
                RECIPE_SERIALIZERS.register(name, () -> new ProcessingRecipe.Serializer(() -> holder[0]));
        holder[0] = new ProcessingRecipe.Kind(name, type, serializer);
        return holder[0];
    }

    private static MachineLayout grid(MachineLayout layout, int x0, int y0) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                layout.slot(x0 + col * 18, y0 + row * 18, MachineLayout.Role.INPUT);
            }
        }
        return layout;
    }

    static BlockBehaviour.Properties machineProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 6.0F).sound(SoundType.METAL)
                .requiresCorrectToolForDrops().noOcclusion();
    }

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    private static <B extends Block, T extends BlockEntity> MachineEntry<B, T> entry(String id, Function<BlockBehaviour.Properties, B> block,
                                                                                    BlockEntityType.BlockEntitySupplier<T> be) {
        DeferredBlock<B> b = BLOCKS.registerBlock(id, block, machineProps());
        DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(b);
        DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> type = BLOCK_ENTITIES.register(id, () -> BlockEntityType.Builder.of(be, b.get()).build(null));
        MachineEntry<B, T> e = new MachineEntry<>(id, b, item, type);
        MACHINES.add(e);
        return e;
    }

    @SuppressWarnings("unchecked")
    private static <T extends MachineBlockEntity> MachineEntry<MachineBlock, T> machine(String id, BlockEntityType.BlockEntitySupplier<T> be,
                                                                                       VoxelShape shape, int light) {
        DeferredHolder<BlockEntityType<?>, BlockEntityType<T>>[] type = new DeferredHolder[1];
        MachineEntry<MachineBlock, T> e = entry(id, p -> new MachineBlock(p, () -> type[0].get(), be::create, shape, light), be);
        type[0] = e.type();
        return e;
    }

    private static MachineEntry<MachineBlock, ProcessingMachineBlockEntity> processing(String id, java.util.function.Supplier<ProcessingRecipe.Kind> kind,
                                                                                      MachineLayout layout, int energy, List<TankSpec> tanks, int light) {
        return processingWith(id, kind, layout, energy, tanks, light, ProcessingMachineBlockEntity::new);
    }

    @SuppressWarnings("unchecked")
    private static MachineEntry<MachineBlock, ProcessingMachineBlockEntity> processingWith(String id, java.util.function.Supplier<ProcessingRecipe.Kind> kind,
                                                                                          MachineLayout layout, int energy, List<TankSpec> tanks, int light,
                                                                                          SpecFactory factory) {
        DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>>[] type = new DeferredHolder[1];
        MachineSpec spec = new MachineSpec(() -> type[0].get(), kind, layout, energy, Math.max(512, energy / 20), tanks);
        MachineEntry<MachineBlock, ProcessingMachineBlockEntity> e = machine(id, (p, s) -> factory.create(spec, p, s), null, light);
        type[0] = e.type();
        return e;
    }

    @FunctionalInterface
    public interface SpecFactory {
        ProcessingMachineBlockEntity create(MachineSpec spec, BlockPos pos, BlockState state);
    }

    @SuppressWarnings("unchecked")
    private static <T extends MachineBlockEntity> MachineEntry<MultiblockMachineBlock, T> multiblock(String id, BlockEntityType.BlockEntitySupplier<T> be,
                                                                                                    VoxelShape shape, List<MultiblockMachineBlock.Part> parts) {
        DeferredHolder<BlockEntityType<?>, BlockEntityType<T>>[] type = new DeferredHolder[1];
        MachineEntry<MultiblockMachineBlock, T> e = entry(id, p -> new MultiblockMachineBlock(p, () -> type[0].get(), be::create, shape, parts), be);
        type[0] = e.type();
        return e;
    }

    @SuppressWarnings("unchecked")
    private static MachineEntry<MultiblockMachineBlock, ProcessingMachineBlockEntity> refinery() {
        DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>>[] type = new DeferredHolder[1];
        MachineSpec spec = new MachineSpec(() -> type[0].get(), () -> REFINING,
                new MachineLayout().energy(10, 17).tank(36, 17).progress(60, 35, MachineLayout.Progress.ARROW).tank(90, 17).tank(112, 17)
                        .slot(140, 34, MachineLayout.Role.OUTPUT),
                40_000, 2_000, List.of(TankSpec.in(16_000, f -> f.is(FluidsModule.TAG_CRUDE_OIL)), TankSpec.out(16_000), TankSpec.out(16_000)));
        BlockEntityType.BlockEntitySupplier<ProcessingMachineBlockEntity> be = (p, s) -> new RefineryBlockEntity(spec, p, s);
        MachineEntry<MultiblockMachineBlock, ProcessingMachineBlockEntity> e = multiblock("refinery", be, null, List.of(
                new MultiblockMachineBlock.Part(0, 1, 0, PartShape.REFINERY_COLUMN),
                new MultiblockMachineBlock.Part(0, 2, 0, PartShape.REFINERY_COLUMN),
                new MultiblockMachineBlock.Part(0, 3, 0, PartShape.REFINERY_TOP)));
        type[0] = e.type();
        return e;
    }

    private static DeferredBlock<TankBlock> tank(String id, TankBlock.Kind kind, MapColor color) {
        return BLOCKS.registerBlock(id, p -> new TankBlock(kind, p),
                BlockBehaviour.Properties.of().mapColor(color).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    }

    private static DeferredBlock<ConduitBlock> conduit(ConduitType type, SoundType sound) {
        DeferredBlock<ConduitBlock> b = BLOCKS.registerBlock(type.id, p -> new ConduitBlock(type, p),
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.8F).sound(sound).noOcclusion());
        CONDUITS.add(b);
        return b;
    }

    // ------------------------------------------------------------------ module

    @Override
    public String id() {
        return "machines";
    }

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener(MachinesModule::capabilities);
        com.pizzaparker07.spacebreaker.core.env.Environment.addRadiationSource(RtgFuelPelletItem::carriedDose);
        com.pizzaparker07.spacebreaker.core.env.Environment.addRadiationSource(
                com.pizzaparker07.spacebreaker.content.machines.machine.FissionReactorBlockEntity::doseMsvDay);
        SbTabs.Tab tab = SbTabs.MACHINES.icon(SOLAR_PANEL_ITEM).add(SOLAR_PANEL_ITEM).add(SOLAR_ARRAY_ITEM).add(POWER_MONITOR_ITEM);
        for (MachineEntry<?, ?> m : MACHINES) {
            tab.add(m.item());
        }
        tab.add(YELLOWCAKE);
        tab.add(ENRICHED_URANIUM);
        tab.add(DEPLETED_URANIUM);
        tab.add(DEPLETED_URANIUM_PLATE);
        tab.add(RTG_FUEL_PELLET);
        tab.add(NUCLEAR_FUEL_ELEMENT);
        TANK_ITEMS.forEach(tab::add);
        CONDUIT_ITEMS.forEach(tab::add);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, SOLAR_PANEL_BE.get(),
                (be, side) -> side != Direction.UP ? be.getEnergy() : null);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, SOLAR_ARRAY_BE.get(),
                (be, side) -> side != Direction.UP ? be.getEnergy() : null);
        for (MachineEntry<?, ?> m : MACHINES) {
            BlockEntityType type = m.type().get();
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (be, side) -> ((MachineBlockEntity) be).energyCapability((Direction) side));
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> ((MachineBlockEntity) be).itemCapability((Direction) side));
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> ((MachineBlockEntity) be).fluidCapability((Direction) side));
        }
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TANK_BE.get(), (be, side) -> be.tank());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PART_BE.get(), (be, side) -> forward(be, Capabilities.EnergyStorage.BLOCK, side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PART_BE.get(), (be, side) -> forward(be, Capabilities.FluidHandler.BLOCK, side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PART_BE.get(), (be, side) -> forward(be, Capabilities.ItemHandler.BLOCK, side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CONDUIT_BE.get(), ConduitBlockEntity::energy);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CONDUIT_BE.get(), ConduitBlockEntity::fluid);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONDUIT_BE.get(), ConduitBlockEntity::items);
    }

    private static <T> T forward(PartBlockEntity part, net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> cap, Direction side) {
        BlockPos c = part.controller();
        if (c == null || part.getLevel() == null || part.getLevel().getBlockEntity(c) instanceof PartBlockEntity) {
            return null;
        }
        return part.getLevel().getCapability(cap, c, side);
    }

    @Override
    public void addTranslations(Lang lang) {
        lang.add("itemGroup.spacebreaker.machines", "Spacebreaker: Machines", "Spacebreaker: Makineler");
        lang.block(SOLAR_PANEL, "Solar Tracker", "Güneş Takipli Panel");
        lang.block(SOLAR_ARRAY, "Solar Array", "Güneş Paneli Dizisi");
        lang.block(POWER_MONITOR, "Power Monitor", "Güç Göstergesi");
        lang.add("tooltip.spacebreaker.power_monitor", "Place against a battery bank: charge, net flow and time to full or empty",
                "Batarya bankasına daya: doluluk, net akış ve dolma/boşalma süresi");
        lang.add("gui.spacebreaker.power_monitor.none", "No energy store behind the monitor", "Göstergenin arkasında enerji deposu yok");
        lang.add("gui.spacebreaker.power_monitor.level", "%s%%", "%%%s");
        lang.add("gui.spacebreaker.power_monitor.flow", "%s FE/t", "%s FE/t");
        lang.add("gui.spacebreaker.power_monitor.full_in", "full in %s", "%s sonra dolu");
        lang.add("gui.spacebreaker.power_monitor.empty_in", "empty in %s", "%s sonra boş");
        lang.add("gui.spacebreaker.power_monitor.full", "full", "dolu");
        lang.add("gui.spacebreaker.power_monitor.empty", "empty", "boş");
        lang.add("tooltip.spacebreaker.solar_panel", "Follows the sun. Peak output: %s FE/t", "Güneşi takip eder. En yüksek üretim: %s FE/t");
        lang.add("tooltip.spacebreaker.solar_panel.here", "Sunlight here: %s%% of Earth's (noon peak %s FE/t)",
                "Burada güneş ışığı: Dünya'nın %%%s'i (öğle tepe değeri %s FE/t)");
        lang.add("message.spacebreaker.solar_panel", "Sunlight here: %s%% of Earth's - making %s FE/t now",
                "Burada güneş ışığı: Dünya'nın %%%s'i - şu an %s FE/t üretiyor");
        lang.add("message.spacebreaker.solar_panel.dusty", "Cells %s%% covered in dust - making %s FE/t. Sneak-use to wipe them",
                "Hücrelerin %%%s'i tozlu - %s FE/t üretiyor. Silmek için eğilip kullan");
        lang.add("message.spacebreaker.solar_panel.wiped", "Wiped the dust off the cells", "Hücrelerdeki toz silindi");
        MachinesLang.add(lang);
    }

    @Override
    public void addBlockLoot(SbBlockLoot loot) {
        loot.self(SOLAR_PANEL.get());
        loot.self(SOLAR_ARRAY.get());
        loot.self(POWER_MONITOR.get());
        for (MachineEntry<?, ?> m : MACHINES) {
            loot.self(m.block().get());
        }
        CONDUITS.forEach(c -> loot.self(c.get()));
        // tanks keep what is in them: carry fuel to a station in its tank instead of canister by canister
        TANKS.forEach(t -> loot.custom(t.get(), b -> net.minecraft.world.level.storage.loot.LootTable.lootTable()
                .withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool()
                        .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                        .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(b)
                                .apply(net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction
                                        .copyComponents(net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source.BLOCK_ENTITY)
                                        .include(com.pizzaparker07.spacebreaker.content.fluids.FluidsModule.FLUID_CONTENT.get())))
                        .when(net.minecraft.world.level.storage.loot.predicates.ExplosionCondition.survivesExplosion()))));
    }

    @Override
    public void addTags(SbTags tags) {
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, SOLAR_PANEL.get());
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, SOLAR_ARRAY.get());
        tags.block(BlockTags.MINEABLE_WITH_PICKAXE, POWER_MONITOR.get());
        for (MachineEntry<?, ?> m : MACHINES) {
            tags.block(BlockTags.MINEABLE_WITH_PICKAXE, m.block().get());
        }
        CONDUITS.forEach(c -> tags.block(BlockTags.MINEABLE_WITH_PICKAXE, c.get()));
        IsruRecipes.tags(tags);
        TANKS.forEach(t -> tags.block(BlockTags.MINEABLE_WITH_PICKAXE, t.get()));
    }

    @Override
    public void addRecipes(RecipeOutput out, HolderLookup.Provider registries) {
        MachineRecipes.add(out);
        IsruRecipes.add(out);
    }
}
