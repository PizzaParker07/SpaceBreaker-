package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.blueprint.BlueprintScreen;
import com.pizzaparker07.spacebreaker.client.guide.GuideScreen;
import com.pizzaparker07.spacebreaker.client.research.ResearchScreen;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.research.Research;
import com.pizzaparker07.spacebreaker.content.research.lab.ResearchLabBlock;
import com.pizzaparker07.spacebreaker.content.research.lab.ResearchLabBlockEntity;
import com.pizzaparker07.spacebreaker.content.research.ResearchModule;
import com.pizzaparker07.spacebreaker.content.research.ResearchTree;
import com.pizzaparker07.spacebreaker.core.tech.Tech;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.assembler.AssemblerSlots;
import com.pizzaparker07.spacebreaker.content.rocketry.assembler.RocketAssemblerBlockEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketDesign;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Locations;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchConsoleBlockEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.pad.TowerAttachmentBlock;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitTanks;
import com.pizzaparker07.spacebreaker.content.vehicles.VehiclesModule;
import com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.ArrayList;
import java.util.List;

/**
 * QA usability pass over the simplified screens (2026-11-08): assembler (complete and incomplete rocket), launch
 * console (crewed without fuel, crewed fuelled, uncrewed probe, cargo route), research lab (without power and
 * running), research tree, field guide, blueprint choice, shipyard, and the suit and rover HUDs. Run with a shot
 * prefix starting with {@code qa_ui} ({@code qa_ui_en}, {@code qa_ui_tr}) so {@link QaUiProbe} records every tooltip
 * and screenshots each one hovered.
 */
final class QaUiScenario implements ShowcaseScenario {
    private static final int F = ShowcaseBuilder.FLOOR;
    private static final double G = F + 1;
    private static final BlockPos PAD = new BlockPos(0, F, 0);
    private static final BlockPos CONSOLE = new BlockPos(5, F + 1, 6);
    private static final BlockPos ASSEMBLER = new BlockPos(7, F + 1, -6);
    private static final BlockPos ASSEMBLER_PART = new BlockPos(9, F + 1, -6);
    private static final BlockPos LAB_DARK = new BlockPos(-8, F + 1, 6);
    private static final BlockPos LAB_LIT = new BlockPos(-10, F + 1, 6);
    private static RocketEntity rocket;

    @Override
    public void build(ServerLevel level) {
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                for (int y = F + 1; y <= F + 30; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                level.setBlock(new BlockPos(x, F, z), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        // the launch site exactly as its blueprint shows it (pad, trench, tower and arms), so the console sees a full pad
        com.pizzaparker07.spacebreaker.core.multiblock.Multiblocks.get(level.registryAccess(),
                com.pizzaparker07.spacebreaker.content.rocketry.pad.LaunchPad.SITE).ifPresent(def -> {
            for (var pl : def.placements(PAD, Direction.NORTH)) {
                level.setBlock(pl.pos(), pl.spec().display(net.minecraft.world.level.block.Rotation.NONE), Block.UPDATE_CLIENTS);
            }
        });
        level.setBlock(CONSOLE, RocketryModule.LAUNCH_CONSOLE.get().defaultBlockState().setValue(TowerAttachmentBlock.FACING, Direction.SOUTH), Block.UPDATE_CLIENTS);
        level.setBlock(ASSEMBLER, RocketryModule.ROCKET_ASSEMBLER.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(ASSEMBLER_PART, RocketryModule.ROCKET_ASSEMBLER.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(ASSEMBLER) instanceof RocketAssemblerBlockEntity be) {
            var items = be.items();
            items.setStackInSlot(AssemblerSlots.stageSlot(0, AssemblerSlots.ENGINE), new ItemStack(RocketryModule.KEROSENE_ENGINE.get()));
            for (int t = AssemblerSlots.TANK_A; t <= AssemblerSlots.TANK_C; t++) {
                items.setStackInSlot(AssemblerSlots.stageSlot(0, t), new ItemStack(RocketryModule.FUEL_TANK_LARGE.get()));
            }
            items.setStackInSlot(AssemblerSlots.stageSlot(0, AssemblerSlots.SEPARATOR), new ItemStack(RocketryModule.INTERSTAGE.get()));
            items.setStackInSlot(AssemblerSlots.stageSlot(1, AssemblerSlots.ENGINE), new ItemStack(RocketryModule.VACUUM_ENGINE.get()));
            items.setStackInSlot(AssemblerSlots.stageSlot(1, AssemblerSlots.TANK_A), new ItemStack(RocketryModule.FUEL_TANK_LARGE.get()));
        }
        if (level.getBlockEntity(ASSEMBLER_PART) instanceof RocketAssemblerBlockEntity be) {
            // tanks but no engine: the assembler should say what is missing
            be.items().setStackInSlot(AssemblerSlots.stageSlot(0, AssemblerSlots.TANK_A), new ItemStack(RocketryModule.FUEL_TANK_LARGE.get()));
        }
        for (BlockPos lab : new BlockPos[]{LAB_DARK, LAB_LIT}) {
            level.setBlock(lab, ResearchModule.RESEARCH_LAB.get().defaultBlockState().setValue(ResearchLabBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        }
        level.setBlock(LAB_LIT.west(), MachinesModule.SOLAR_PANEL.get().defaultBlockState(), Block.UPDATE_ALL);
        ServerPlayer player = level.getServer().getPlayerList().getPlayers().get(0);
        for (String t : new String[]{"basic_rocketry", "pressure_suits", "aluminium_smelting", "solar_power", "metal_pressing",
                "electronics", "fuel_refining", "liquid_fuel_engines", "launch_infrastructure"}) {
            Tech.learn(player, ResearchTree.id(t));
        }
        disk(level, player, LAB_DARK, "field_geology", false);
        disk(level, player, LAB_LIT, "observational_astronomy", true);
    }

    private static void disk(ServerLevel level, ServerPlayer player, BlockPos pos, String tech, boolean power) {
        if (level.getBlockEntity(pos) instanceof ResearchLabBlockEntity lab) {
            lab.setResearcher(player);
            lab.setItem(ResearchLabBlockEntity.SLOT_INPUT, Research.disk(ResearchTree.id(tech)));
            var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            for (int i = 0; power && i < 120 && energy != null; i++) {
                energy.receiveEnergy(ResearchLabBlockEntity.MAX_RECEIVE, false);
            }
        }
    }

    @Override
    public List<Shot> shots() {
        CameraType fp = CameraType.FIRST_PERSON;
        List<Shot> s = new ArrayList<>();
        s.add(new Shot("01_assembler", 6000, 7.5, G + 1.6, -3.5, 180F, 20F, false, fp, 40, p -> open(p, ASSEMBLER), null));
        s.add(new Shot("02_assembler_no_engine", 6000, 9.5, G + 1.6, -3.5, 180F, 20F, false, fp, 40, p -> open(p, ASSEMBLER_PART), null));
        s.add(new Shot("03_console_no_fuel", 6000, 5.5, G + 1.6, 8.5, 180F, 20F, false, fp, 60, p -> {
            onPad(p, RocketEntity.create(p.serverLevel(), RocketryModule.MOON_ROCKET, Vec3.atBottomCenterOf(PAD.above()), -90F), false);
            console(p);
        }, null));
        s.add(new Shot("04_console_crewed", 6000, 5.5, G + 1.6, 8.5, 180F, 20F, false, fp, 60, p -> {
            onPad(p, RocketEntity.create(p.serverLevel(), RocketryModule.MOON_ROCKET, Vec3.atBottomCenterOf(PAD.above()), -90F), true);
            console(p);
        }, null));
        s.add(new Shot("05_console_uncrewed", 6000, 5.5, G + 1.6, 8.5, 180F, 20F, false, fp, 60, p -> {
            RocketEntity probe = RocketEntity.create(p.serverLevel(), design("space_probe"), Vec3.atBottomCenterOf(PAD.above()), -90F);
            onPad(p, probe, true);
            Locations.get(Spacebreaker.id("mars"), false).ifPresent(probe::setMissionTarget);
            console(p);
        }, null));
        s.add(new Shot("06_console_cargo", 6000, 5.5, G + 1.6, 8.5, 180F, 20F, false, fp, 80, p -> {
            RocketEntity cargo = RocketEntity.create(p.serverLevel(), design("cargo_module"), Vec3.atBottomCenterOf(PAD.above()), -90F);
            onPad(p, cargo, true);
            var moonOrbit = Locations.get(Location.MOON, true).orElseThrow();
            cargo.startRoute(Location.MOON_SURFACE, moonOrbit);
            cargo.setMissionTarget(moonOrbit);
            cargo.cargo().setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 64));
            cargo.scheduleRoute(6000, PAD);
            console(p);
        }, null));
        s.add(new Shot("07_lab_no_power", 6000, -7.5, G + 1.9, 9.2, 180F, 22F, false, fp, 40, p -> open(p, LAB_DARK), null));
        s.add(new Shot("08_lab_running", 6000, -9.5, G + 1.9, 9.2, 180F, 22F, false, fp, 40, p -> open(p, LAB_LIT), null));
        s.add(new Shot("09_research_tree", 6000, -9.5, G + 1.9, 9.2, 180F, 22F, false, fp, 40, null, () -> client(() -> {
            ResearchScreen screen = new ResearchScreen(null);
            Minecraft.getInstance().setScreen(screen);
            screen.focus(ResearchTree.id("basic_rocketry"));
        })));
        s.add(new Shot("10_guide_contents", 6000, -9.5, G + 1.9, 9.2, 180F, 22F, false, fp, 40, null, () -> client(() -> {
            GuideScreen screen = new GuideScreen();
            Minecraft.getInstance().setScreen(screen);
            screen.showContents();
        })));
        s.add(new Shot("11_guide_launch_pad", 6000, -9.5, G + 1.9, 9.2, 180F, 22F, false, fp, 40, null, () -> client(() -> {
            GuideScreen screen = new GuideScreen();
            Minecraft.getInstance().setScreen(screen);
            screen.openChapter(Spacebreaker.id("launch_pad"), 0);
        })));
        s.add(new Shot("12_blueprint_choice", 6000, -9.5, G + 1.9, 9.2, 180F, 22F, false, fp, 40, null,
                () -> client(() -> Minecraft.getInstance().setScreen(new BlueprintScreen()))));
        s.add(new Shot("13_shipyard", 6000, -9.5, G + 1.9, 9.2, 180F, 22F, false, fp, 60, null, () -> client(() ->
                RocketryShowcaseClient.shipDesign = com.pizzaparker07.spacebreaker.content.rocketry.ship.ShipDesign.EMPTY
                        .with(com.pizzaparker07.spacebreaker.content.rocketry.ship.ShipPart.COCKPIT)
                        .with(com.pizzaparker07.spacebreaker.content.rocketry.ship.ShipPart.CABIN)
                        .with(com.pizzaparker07.spacebreaker.content.rocketry.ship.ShipPart.TANK_LARGE)
                        .with(com.pizzaparker07.spacebreaker.content.rocketry.ship.ShipPart.ENGINE_NUCLEAR))));
        s.add(new Shot("14_suit_hud", 6000, 0, 0, 0, 0F, 0F, false, fp, 100, QaUiScenario::moonSuit,
                () -> RocketryShowcaseClient.shipDesign = null));
        s.add(new Shot("15_rover_hud", 6000, 0, 0, 0, 0F, 0F, false, fp, 100, p -> {
            moonSuit(p);
            var rover = VehiclesModule.ROVER.get().create(p.serverLevel());
            if (rover != null) {
                rover.moveTo(p.getX(), p.getY(), p.getZ() + 3, 0F, 0F);
                p.serverLevel().addFreshEntity(rover);
                p.startRiding(rover, true);
            }
        }, null));
        return s;
    }

    private static RocketDesign design(String payload) {
        return new RocketDesign(List.of(Spacebreaker.id("kerosene_engine"), Spacebreaker.id("fuel_tank_large"), Spacebreaker.id("interstage"),
                Spacebreaker.id("vacuum_engine"), Spacebreaker.id("fuel_tank_medium"), Spacebreaker.id(payload), Spacebreaker.id("nose_cone")));
    }

    private static void onPad(ServerPlayer p, RocketEntity r, boolean fuel) {
        p.closeContainer();
        if (rocket != null) {
            rocket.discard();
        }
        rocket = r;
        if (fuel) {
            r.fillCompletely();
        }
        r.setOwner(p.getUUID());
        p.serverLevel().addFreshEntity(r);
    }

    private static void console(ServerPlayer p) {
        p.getServer().execute(() -> {
            if (p.serverLevel().getBlockEntity(CONSOLE) instanceof LaunchConsoleBlockEntity be) {
                be.refresh();
                p.openMenu(be, CONSOLE);
            }
        });
    }

    private static void open(ServerPlayer p, BlockPos pos) {
        p.closeContainer();
        p.getAbilities().flying = false;
        var be = p.serverLevel().getBlockEntity(pos);
        if (be instanceof RocketAssemblerBlockEntity a) {
            p.openMenu(a, pos);
        } else if (be instanceof ResearchLabBlockEntity lab) {
            p.openMenu(lab, buf -> buf.writeBlockPos(pos));
        }
    }

    /** A client screen: close the container screen from the client side first, so the server does not close ours. */
    private static void client(Runnable open) {
        Minecraft mc = Minecraft.getInstance();
        mc.getToasts().clear();
        if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) {
            mc.player.closeContainer();
        }
        open.run();
    }

    private static void moonSuit(ServerPlayer p) {
        ServerLevel moon = p.server.getLevel(Location.MOON_SURFACE.dimension());
        if (moon == null) {
            return;
        }
        int x = 212, z = 212;
        moon.getChunk(x >> 4, z >> 4);
        int ground = moon.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        p.teleportTo(moon, x + 0.5, ground + 0.1, z + 0.5, 0F, 10F);
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SuitModule.EVA_HELMET.get()));
        ItemStack torso = new ItemStack(SuitModule.EVA_CHESTPLATE.get());
        SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
        p.setItemSlot(EquipmentSlot.CHEST, torso);
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(SuitModule.EVA_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(SuitModule.EVA_BOOTS.get()));
    }
}
