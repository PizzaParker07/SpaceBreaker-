package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.earth.EarthModule;
import com.pizzaparker07.spacebreaker.content.earth.Metal;
import com.pizzaparker07.spacebreaker.content.fluids.FluidsModule;
import com.pizzaparker07.spacebreaker.content.machines.MachineEntry;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlockEntity;
import com.pizzaparker07.spacebreaker.content.machines.machine.TankBlockEntity;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;

/** Industry showcase: ore gallery, machine hall (machines really running), tanks and conduits, screens. */
final class IndustryScenario implements ShowcaseScenario {
    static final int F = ShowcaseBuilder.FLOOR;
    static final double G = F + 1;
    static final int HALL_Z = 0;
    static final int GALLERY_X = 30;

    @Override
    public void build(ServerLevel level) {
        clear(level, -34, -8, 44, 22);
        machineHall(level);
        oilField(level);
        oreGallery(level);
        isruBay(level);
    }

    static void clear(ServerLevel level, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = F + 1; y <= F + 14; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                level.setBlock(new BlockPos(x, F, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_ALL);
    }

    private static void machineHall(ServerLevel level) {
        for (int x = -12; x <= 12; x++) {
            for (int z = -5; z <= 9; z++) {
                set(level, x, F, z, Blocks.POLISHED_ANDESITE.defaultBlockState());
            }
            for (int y = F + 1; y <= F + 5; y++) {
                set(level, x, y, -5, (y == F + 5 ? Blocks.SMOOTH_STONE : Blocks.STONE_BRICKS).defaultBlockState());
            }
        }
        List<MachineEntry<?, ?>> row = List.of(MachinesModule.COMBUSTION_GENERATOR, MachinesModule.BATTERY_BANK, MachinesModule.CRUSHER,
                MachinesModule.ARC_FURNACE, MachinesModule.PRESS, MachinesModule.FABRICATOR, MachinesModule.ELECTROLYZER, MachinesModule.LIQUEFIER);
        int x = -8;
        for (MachineEntry<?, ?> m : row) {
            set(level, x, F + 1, HALL_Z, m.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
            x += 2;
        }
        // copper cable behind the row, HV cable in a second line
        for (int cx = -8; cx <= 6; cx++) {
            set(level, cx, F + 1, HALL_Z - 1, MachinesModule.COPPER_CABLE.get().defaultBlockState());
        }
        // feed the machines so they really run
        feed(level, MachinesModule.COMBUSTION_GENERATOR, -8, be -> be.items().setStackInSlot(0, new ItemStack(Items.COAL_BLOCK, 16)));
        feed(level, MachinesModule.BATTERY_BANK, -6, be -> be.energy().generate(700_000));
        feed(level, MachinesModule.CRUSHER, -4, be -> {
            be.energy().generate(20_000);
            be.items().setStackInSlot(0, new ItemStack(EarthModule.ALUMINUM.raw.get(), 64));
        });
        feed(level, MachinesModule.ARC_FURNACE, -2, be -> {
            be.energy().generate(60_000);
            be.items().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
            be.items().setStackInSlot(1, new ItemStack(EarthModule.COAL_DUST.get(), 64));
        });
        feed(level, MachinesModule.PRESS, 0, be -> {
            be.energy().generate(20_000);
            be.items().setStackInSlot(0, new ItemStack(EarthModule.STEEL.ingotItem(), 64));
            be.items().setStackInSlot(1, new ItemStack(EarthModule.PLATE_DIE.get()));
        });
        feed(level, MachinesModule.FABRICATOR, 2, be -> {
            be.energy().generate(40_000);
            be.items().setStackInSlot(0, new ItemStack(EarthModule.GOLD.plate.get(), 64));
            be.items().setStackInSlot(1, new ItemStack(EarthModule.PLASTIC_SHEET.get(), 64));
        });
        feed(level, MachinesModule.ELECTROLYZER, 4, be -> {
            be.energy().generate(40_000);
            be.tank(0).fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 8000), IFluidHandler.FluidAction.EXECUTE);
        });
        feed(level, MachinesModule.LIQUEFIER, 6, be -> {
            be.energy().generate(60_000);
            be.tank(0).fill(FluidsModule.OXYGEN.stack(8000), IFluidHandler.FluidAction.EXECUTE);
        });
        // tanks with fluids and pipes
        int tz = HALL_Z + 5;
        set(level, -6, F + 1, tz, MachinesModule.FLUID_TANK.get().defaultBlockState());
        set(level, -4, F + 1, tz, MachinesModule.FLUID_TANK.get().defaultBlockState());
        set(level, -1, F + 1, tz, MachinesModule.PRESSURIZED_TANK.get().defaultBlockState());
        set(level, 2, F + 1, tz, MachinesModule.CRYOGENIC_TANK.get().defaultBlockState());
        set(level, 4, F + 1, tz, MachinesModule.CRYOGENIC_TANK.get().defaultBlockState());
        fillTank(level, -6, tz, FluidsModule.CRUDE_OIL.stack(24_000));
        fillTank(level, -4, tz, FluidsModule.RP1.stack(12_000));
        fillTank(level, -1, tz, FluidsModule.OXYGEN.stack(40_000));
        fillTank(level, 2, tz, FluidsModule.LIQUID_OXYGEN.stack(28_000));
        fillTank(level, 4, tz, FluidsModule.LIQUID_HYDROGEN.stack(9_000));
        set(level, -5, F + 1, tz, MachinesModule.FLUID_PIPE.get().defaultBlockState());
        for (int px = 0; px <= 1; px++) {
            set(level, px, F + 1, tz, MachinesModule.GAS_PIPE.get().defaultBlockState());
        }
        set(level, 3, F + 1, tz, MachinesModule.FLUID_PIPE.get().defaultBlockState());
        for (int py = F + 2; py <= F + 3; py++) {
            set(level, -6, py, tz, MachinesModule.FLUID_PIPE.get().defaultBlockState());
        }
        set(level, -9, F + 1, tz, MachinesModule.HV_CABLE.get().defaultBlockState());
        set(level, -10, F + 1, tz, MachinesModule.HV_CABLE.get().defaultBlockState());
        set(level, -10, F + 2, tz, MachinesModule.HV_CABLE.get().defaultBlockState());
        set(level, 8, F + 1, tz, MachinesModule.SOLAR_PANEL.get().defaultBlockState());
        set(level, 9, F + 1, tz, MachinesModule.SOLAR_PANEL.get().defaultBlockState());
        // lamps
        for (int lx = -10; lx <= 10; lx += 5) {
            set(level, lx, F + 4, -4, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, false));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends MachineBlockEntity> void feed(ServerLevel level, MachineEntry<?, ?> m, int x, Consumer<MachineBlockEntity> c) {
        if (level.getBlockEntity(new BlockPos(x, F + 1, HALL_Z)) instanceof MachineBlockEntity be) {
            c.accept(be);
        }
    }

    private static void fillTank(ServerLevel level, int x, int z, FluidStack fluid) {
        if (level.getBlockEntity(new BlockPos(x, F + 1, z)) instanceof TankBlockEntity be) {
            be.tank().fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    static final int OIL_X = 18;

    private static void oilField(ServerLevel level) {
        for (int x = OIL_X - 4; x <= OIL_X + 9; x++) {
            for (int z = 0; z <= 9; z++) {
                set(level, x, F, z, Blocks.COARSE_DIRT.defaultBlockState());
            }
        }
        // the reservoir right under the wellhead (superflat: dirt layers above bedrock)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int y = F - 2; y <= F - 1; y++) {
                    set(level, OIL_X + 1 + dx, y, 4 + dz, FluidsModule.CRUDE_OIL.block.get().defaultBlockState());
                }
            }
        }
        set(level, OIL_X + 1, F, 6, EarthModule.OIL_SEEP.get().defaultBlockState());
        set(level, OIL_X + 2, F, 7, EarthModule.OIL_SEEP.get().defaultBlockState());
        BlockPos pj = new BlockPos(OIL_X, F + 1, 4);
        BlockState pjs = MachinesModule.PUMPJACK.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST);
        level.setBlock(pj, pjs, Block.UPDATE_ALL);
        MachinesModule.PUMPJACK.block().get().build(level, pj, pjs);
        BlockPos rf = new BlockPos(OIL_X + 5, F + 1, 4);
        BlockState rfs = MachinesModule.REFINERY.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
        level.setBlock(rf, rfs, Block.UPDATE_ALL);
        MachinesModule.REFINERY.block().get().build(level, rf, rfs);
        for (int x = OIL_X + 2; x <= OIL_X + 4; x++) {
            set(level, x, F + 1, 4, MachinesModule.FLUID_PIPE.get().defaultBlockState());
        }
        set(level, OIL_X + 6, F + 1, 4, MachinesModule.FLUID_TANK.get().defaultBlockState());
        set(level, OIL_X + 5, F + 1, 5, MachinesModule.PRESSURIZED_TANK.get().defaultBlockState());
        if (level.getBlockEntity(pj) instanceof MachineBlockEntity be) {
            be.energy().generate(30_000);
        }
        if (level.getBlockEntity(rf) instanceof MachineBlockEntity be) {
            be.energy().generate(40_000);
            be.tank(0).fill(FluidsModule.CRUDE_OIL.stack(8_000), IFluidHandler.FluidAction.EXECUTE);
        }
        // solar trackers feeding the field through a copper cable
        for (int x = OIL_X - 3; x <= OIL_X - 1; x++) {
            set(level, x, F + 1, 8, MachinesModule.SOLAR_PANEL.get().defaultBlockState());
        }
    }

    static final int ISRU_X = -26;

    /** A Mars-style fuel plant: ice and dry ice in, liquid methane and LOX out; regolith heater and air processor beside it. */
    private static void isruBay(ServerLevel level) {
        BlockState floor = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getOptional(com.pizzaparker07.spacebreaker.Spacebreaker.id("martian_regolith")).map(Block::defaultBlockState)
                .orElse(Blocks.RED_SAND.defaultBlockState());
        for (int x = ISRU_X - 5; x <= ISRU_X + 16; x++) {
            for (int z = -3; z <= 8; z++) {
                set(level, x, F, z, floor);
            }
        }
        int x0 = ISRU_X;
        place(level, MachinesModule.ICE_MELTER, x0 + 1, 0, be -> be.items().setStackInSlot(0, new ItemStack(Items.ICE, 64)));
        place(level, MachinesModule.ICE_MELTER, x0 + 2, 0, be -> be.items().setStackInSlot(0, new ItemStack(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(com.pizzaparker07.spacebreaker.Spacebreaker.id("dry_ice")).orElse(Items.SNOWBALL), 64)));
        place(level, MachinesModule.ELECTROLYZER, x0 + 1, 1, be -> { });
        place(level, MachinesModule.SABATIER_REACTOR, x0 + 2, 1, be -> { });
        place(level, MachinesModule.LIQUEFIER, x0, 1, be -> { });
        place(level, MachinesModule.LIQUEFIER, x0 + 3, 1, be -> { });
        set(level, x0 + 3, F + 1, 2, MachinesModule.CRYOGENIC_TANK.get().defaultBlockState());
        set(level, x0, F + 1, 2, MachinesModule.CRYOGENIC_TANK.get().defaultBlockState());
        place(level, MachinesModule.REGOLITH_HEATER, x0 + 5, 1, be -> be.items().setStackInSlot(0, new ItemStack(floor.getBlock().asItem(), 64)));
        place(level, MachinesModule.ATMOSPHERE_PROCESSOR, x0 + 6, 1, be -> { });
        set(level, x0 + 5, F + 1, 2, MachinesModule.PRESSURIZED_TANK.get().defaultBlockState());
        set(level, x0 + 6, F + 1, 2, MachinesModule.PRESSURIZED_TANK.get().defaultBlockState());
        for (int x = x0 - 3; x <= x0 - 1; x++) {
            set(level, x, F + 1, 5, MachinesModule.SOLAR_PANEL.get().defaultBlockState());
        }
        set(level, x0 - 4, F + 1, -1, MachinesModule.SOLAR_ARRAY.get().defaultBlockState());
        // base power: a battery bank with a monitor on its back, charged by the array above
        set(level, x0 - 4, F + 1, 2, MachinesModule.BATTERY_BANK.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        if (level.getBlockEntity(new BlockPos(x0 - 4, F + 1, 2)) instanceof MachineBlockEntity bank) {
            bank.energy().generate(640_000);
        }
        set(level, x0 - 4, F + 1, 3, MachinesModule.POWER_MONITOR.get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.machines.PowerMonitorBlock.FACING, Direction.SOUTH));
        set(level, x0 - 4, F + 1, 1, MachinesModule.COPPER_CABLE.get().defaultBlockState());
        // display row facing the camera, all running
        List<MachineEntry<?, ?>> show = List.of(MachinesModule.ATMOSPHERE_PROCESSOR, MachinesModule.SABATIER_REACTOR,
                MachinesModule.REGOLITH_HEATER, MachinesModule.ICE_MELTER, MachinesModule.CHEMICAL_REACTOR, MachinesModule.RTG,
                MachinesModule.REGOLITH_EXCAVATOR, MachinesModule.SINTERING_FURNACE);
        for (int i = 0; i < show.size(); i++) {
            BlockPos pos = new BlockPos(x0 + 1 + i * 2, F + 1, 6);
            level.setBlock(pos, show.get(i).block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
                be.energy().generate(be.energy().getMaxEnergyStored());
                switch (i) {
                    case 1 -> {
                        be.fluidCapability(null).fill(FluidsModule.HYDROGEN.stack(8_000), IFluidHandler.FluidAction.EXECUTE);
                        be.fluidCapability(null).fill(FluidsModule.CARBON_DIOXIDE.stack(8_000), IFluidHandler.FluidAction.EXECUTE);
                    }
                    case 2 -> be.items().setStackInSlot(0, new ItemStack(floor.getBlock().asItem(), 64));
                    case 3 -> be.items().setStackInSlot(0, new ItemStack(Items.PACKED_ICE, 64));
                    case 5 -> be.items().setStackInSlot(0, new ItemStack(MachinesModule.RTG_FUEL_PELLET.get()));
                    case 7 -> be.items().setStackInSlot(0, new ItemStack(floor.getBlock().asItem(), 64));
                    default -> { }
                }
            }
        }
    }

    /** Settled dust in steps across the tracker row, the station array fully covered. */
    private static void dustPanels(ServerPlayer player) {
        float[] cover = {0.3F, 0.55F, 0.8F};
        for (int i = 0; i < cover.length; i++) {
            if (player.level().getBlockEntity(new BlockPos(ISRU_X - 3 + i, F + 1, 5)) instanceof com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity be) {
                be.setDust(cover[i]);
            }
        }
        if (player.level().getBlockEntity(new BlockPos(ISRU_X - 4, F + 1, -1)) instanceof com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity be) {
            be.setDust(0.8F);
        }
    }

    /** Keeps the ISRU bay running between photos (software rendering is slow, the machines are hungry). */
    private static void rechargeIsru(ServerPlayer player) {
        for (int x = ISRU_X - 1; x <= ISRU_X + 16; x++) {
            for (int z = 0; z <= 6; z++) {
                if (player.level().getBlockEntity(new BlockPos(x, F + 1, z)) instanceof MachineBlockEntity be) {
                    be.energy().generate(be.energy().getMaxEnergyStored());
                    if (z == 6 && be.getBlockState().is(MachinesModule.SABATIER_REACTOR.block().get())) {
                        be.fluidCapability(null).fill(FluidsModule.HYDROGEN.stack(8_000), IFluidHandler.FluidAction.EXECUTE);
                        be.fluidCapability(null).fill(FluidsModule.CARBON_DIOXIDE.stack(8_000), IFluidHandler.FluidAction.EXECUTE);
                        be.tank(2).setFluid(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
                        be.tank(3).setFluid(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
                    }
                }
            }
        }
    }

    private static void place(ServerLevel level, MachineEntry<?, ?> m, int x, int z, Consumer<MachineBlockEntity> c) {
        BlockPos pos = new BlockPos(x, F + 1, z);
        level.setBlock(pos, m.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            be.energy().generate(be.energy().getMaxEnergyStored());
            c.accept(be);
        }
    }

    private static void oreGallery(ServerLevel level) {
        int x0 = GALLERY_X, z0 = 0;
        // a cave-like room: stone above, deepslate below, ores set into the back wall
        for (int x = x0 - 1; x <= x0 + 12; x++) {
            for (int z = z0 - 2; z <= z0 + 8; z++) {
                for (int y = F; y <= F + 7; y++) {
                    boolean shell = x == x0 - 1 || x == x0 + 12 || z == z0 - 2 || y == F || y == F + 7;
                    boolean open = z == z0 + 8 && y > F && y < F + 6;
                    BlockState s = y <= F + 2 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
                    set(level, x, y, z, shell && !open ? s : Blocks.AIR.defaultBlockState());
                }
            }
        }
        int i = 0;
        for (Metal m : EarthModule.ORE_METALS) {
            int x = x0 + 1 + i * 2;
            set(level, x, F + 3, z0 - 2, m.ore.get().defaultBlockState());
            set(level, x + 1, F + 4, z0 - 2, m.ore.get().defaultBlockState());
            set(level, x, F + 1, z0 - 2, m.deepslateOre.get().defaultBlockState());
            set(level, x + 1, F + 2, z0 - 2, m.deepslateOre.get().defaultBlockState());
            set(level, x, F + 1, z0 + 4, m.rawBlock.get().defaultBlockState());
            set(level, x, F + 2, z0 + 4, m.block.get().defaultBlockState());
            i++;
        }
        set(level, x0 + 11, F + 1, z0 + 2, EarthModule.OIL_SEEP.get().defaultBlockState());
        set(level, x0 + 11, F, z0 + 3, EarthModule.OIL_SEEP.get().defaultBlockState());
        for (int lx = x0 + 1; lx <= x0 + 10; lx += 3) {
            set(level, lx, F + 6, z0 + 3, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        }
        // item frames with every material
        List<ItemStack> items = new ArrayList<>();
        for (Metal m : EarthModule.METALS) {
            m.items().stream().filter(s -> !(s.get() instanceof net.minecraft.world.item.BlockItem)).forEach(s -> items.add(new ItemStack(s.get())));
        }
        int k = 0;
        for (ItemStack stack : items) {
            int fx = x0 + 11 - (k % 11), fy = F + 6 - (k / 11) - 0;
            if (fy <= F + 4) {
                break;
            }
            ItemFrame frame = new ItemFrame(level, new BlockPos(fx, fy, z0 - 1), Direction.SOUTH);
            frame.setItem(stack, false);
            level.addFreshEntity(frame);
            k++;
        }
    }

    @Override
    public List<Shot> shots() {
        List<Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        s.add(new Shot("12_isru_bay", 6000, ISRU_X + 3.5, G + 3.2, -5.5, 0F, 24F, true, fp, 400, IndustryScenario::rechargeIsru, null));
        s.add(new Shot("13_isru_close", 6000, ISRU_X + 2.0, G + 1.9, -1.9, 10F, 18F, true, fp, 60, IndustryScenario::rechargeIsru, null));
        s.add(new Shot("13b_isru_row", 6000, ISRU_X + 5.5, G + 2.2, 11.6, 180F, 14F, true, fp, 80, IndustryScenario::rechargeIsru, null));
        s.add(new Shot("13c_isru_row_night", 18000, ISRU_X + 5.5, G + 2.2, 11.6, 180F, 14F, true, fp, 40, IndustryScenario::rechargeIsru, null));
        s.add(new Shot("14_isru_night", 18000, ISRU_X + 3.5, G + 2.6, -4.0, 0F, 20F, true, fp, 60, IndustryScenario::rechargeIsru, null));
        s.add(new Shot("15_gui_sabatier", 6000, ISRU_X + 2.5, G + 1.62, -1.0, 0F, 30F, false, fp, 40,
                p -> openAt(p, new BlockPos(ISRU_X + 2, F + 1, 1)), null));
        s.add(new Shot("20_excavator_sinter", 6000, ISRU_X + 14.0, G + 2.2, 10.6, 180F, 16F, true, fp, 120, IndustryScenario::rechargeIsru, null));
        s.add(new Shot("19_power_monitor", 6000, ISRU_X - 3.5, G + 1.7, 5.3, 180F, 12F, true, fp, 120, null, null));
        s.add(new Shot("18_solar_array", 6000, ISRU_X - 0.5, G + 2.4, 2.5, 120F, 18F, true, fp, 60, null, null));
        s.add(new Shot("21_dusty_panels", 6000, ISRU_X - 0.5, G + 2.4, 2.5, 120F, 18F, true, fp, 60, IndustryScenario::dustPanels, null));
        s.add(new Shot("16_rtg", 6000, ISRU_X + 11.5, G + 2.0, 8.9, 180F, 22F, true, fp, 60, null, null));
        s.add(new Shot("17_gui_rtg", 6000, ISRU_X + 11.5, G + 1.62, 7.0, 180F, 30F, false, fp, 40,
                p -> openAt(p, new BlockPos(ISRU_X + 11, F + 1, 6)), null));
        s.add(new Shot("01_hall_wide", 6000, -1.0, G + 4.0, 11.5, 180F, 22F, true, fp, 200, null, null));
        s.add(new Shot("02_hall_left", 6000, -5.5, G + 1.9, 3.3, 180F, 14F, true, fp, 60, null, null));
        s.add(new Shot("03_hall_right", 6000, 3.5, G + 1.9, 3.3, 180F, 14F, true, fp, 60, null, null));
        s.add(new Shot("04_tanks", 6000, -1.0, G + 2.0, 9.6, 180F, 14F, true, fp, 40, null, null));
        s.add(new Shot("05_ore_gallery", 6000, GALLERY_X + 5.5, G + 2.6, 7.5, 180F, 8F, true, fp, 60, null, null));
        s.add(new Shot("06_ore_close", 6000, GALLERY_X + 3.0, G + 2.4, 1.2, 180F, 12F, true, fp, 40, null, null));
        s.add(new Shot("05b_oil_field", 6000, OIL_X + 2.5, G + 3.5, 13.0, 180F, 16F, true, fp, 200, null, null));
        s.add(new Shot("05c_pumpjack_close", 7000, OIL_X + 0.5, G + 2.0, 8.8, 170F, 4F, true, fp, 120, null, null));
        s.add(new Shot("05d_refinery", 6000, OIL_X + 7.8, G + 2.5, 8.5, 150F, -8F, true, fp, 60, null, null));
        s.add(new Shot("07_night_glow", 18000, -1.0, G + 2.8, 6.5, 180F, 16F, true, fp, 60, null, null));
        s.add(new Shot("08_gui_crusher", 6000, -4.5, G + 1.62, 2.5, 180F, 30F, false, fp, 40, p -> open(p, -4), null));
        s.add(new Shot("09_gui_electrolyzer", 6000, 4.5, G + 1.62, 2.5, 180F, 30F, false, fp, 40, p -> open(p, 4), null));
        s.add(new Shot("10_gui_generator", 6000, -7.5, G + 1.62, 2.5, 180F, 30F, false, fp, 40, p -> open(p, -8), null));
        s.add(new Shot("11_gui_refinery", 6000, OIL_X + 5.5, G + 1.62, 7.0, 180F, 20F, false, fp, 40, p -> openAt(p, new BlockPos(OIL_X + 5, F + 1, 4)), null));
        return s;
    }

    private static void open(ServerPlayer player, int x) {
        openAt(player, new BlockPos(x, F + 1, HALL_Z));
    }

    private static void openAt(ServerPlayer player, BlockPos pos) {
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        if (player.level().getBlockEntity(pos) instanceof MachineBlockEntity be) {
            player.openMenu(be, buf -> buf.writeBlockPos(pos));
        }
    }
}
