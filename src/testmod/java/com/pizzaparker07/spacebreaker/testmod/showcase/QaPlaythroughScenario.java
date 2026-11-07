package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.content.research.Research;
import com.pizzaparker07.spacebreaker.content.research.ResearchModule;
import com.pizzaparker07.spacebreaker.content.research.ResearchTree;
import com.pizzaparker07.spacebreaker.content.research.lab.ResearchLabBlockEntity;
import com.pizzaparker07.spacebreaker.content.suit.OxygenTankItem;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.ArrayList;
import java.util.List;

/**
 * QA playthrough (-Pscenario=qa_playthrough): walks the survival order the progression model found — research lab on
 * combustion power, the oxygen / liquid oxygen chain (press, fabricator, electrolyser, cryogenic liquefier), then the
 * rocketry scenes in flight order (assembler, console, countdown, ascent, rocket camera, orbit HUD, star map, Moon
 * landing) and the suit HUD on the Moon. Every shot is looked at for missing textures, untranslated keys, overlapping
 * screens and invisible entities (findings in docs/QA.md).
 */
final class QaPlaythroughScenario implements ShowcaseScenario {
    private static final int F = -61;
    /** The survival bench, north of the launch site. */
    private static final BlockPos BENCH = new BlockPos(0, F + 1, -40);
    private static final BlockPos LAB = BENCH;
    private static final BlockPos GENERATOR = BENCH.offset(-2, 0, 0);
    private static final BlockPos PRESS = BENCH.offset(2, 0, 0);
    private static final BlockPos FABRICATOR = BENCH.offset(4, 0, 0);
    private static final BlockPos ELECTROLYZER = BENCH.offset(6, 0, 0);
    private static final BlockPos LIQUEFIER = BENCH.offset(8, 0, 0);
    /** Rocketry shots reused, in flight order; the Turkish ones (from 23_tr_setup on) come last. */
    private static final List<String> FLIGHT = List.of("01_pad_morning", "05_assembler_gui", "06_console_gui", "07_ignition",
            "08_liftoff", "09_ascent", "10_earth_orbit", "12_orbit_hud", "13_star_map", "15_moon_descent", "16_moon_landed");
    private static final List<String> TURKISH = List.of("23_tr_setup", "24_rocket_camera");

    private final ShowcaseScenario rocketry;

    QaPlaythroughScenario(ShowcaseScenario rocketry) {
        this.rocketry = rocketry;
    }

    @Override
    public void build(ServerLevel level) {
        rocketry.build(level);
        for (int x = -6; x <= 14; x++) {
            for (int z = -46; z <= -32; z++) {
                for (int y = F + 1; y <= F + 6; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                level.setBlock(new BlockPos(x, F, z), Blocks.POLISHED_ANDESITE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        place(level, LAB, ResearchModule.RESEARCH_LAB.get().defaultBlockState());
        place(level, GENERATOR, MachinesModule.COMBUSTION_GENERATOR.block().get().defaultBlockState());
        place(level, PRESS, MachinesModule.PRESS.block().get().defaultBlockState());
        place(level, FABRICATOR, MachinesModule.FABRICATOR.block().get().defaultBlockState());
        place(level, ELECTROLYZER, MachinesModule.ELECTROLYZER.block().get().defaultBlockState());
        place(level, LIQUEFIER, MachinesModule.LIQUEFIER.block().get().defaultBlockState());
        ServerPlayer player = level.getServer().getPlayerList().getPlayers().get(0);
        if (level.getBlockEntity(LAB) instanceof ResearchLabBlockEntity lab) {
            lab.setResearcher(player);
            lab.setItem(ResearchLabBlockEntity.SLOT_INPUT, Research.disk(ResearchTree.id("electrolysis")));
            var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, LAB, null);
            for (int i = 0; i < 100 && energy != null; i++) {
                energy.receiveEnergy(ResearchLabBlockEntity.MAX_RECEIVE, false);
            }
        }
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            state = state.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
        }
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        CameraType fp = CameraType.FIRST_PERSON;
        double g = F + 1;
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        s.add(new ShowcaseDriver.Shot("qa01_bench", 6000, 3.5, g + 2.6, -34.5, 180F, 18F, true, fp, 120, null, null));
        s.add(gui("qa02_research_lab", LAB, true));
        s.add(gui("qa03_combustion_generator", GENERATOR, false));
        s.add(gui("qa04_press", PRESS, false));
        s.add(gui("qa05_fabricator", FABRICATOR, false));
        s.add(gui("qa06_electrolyzer", ELECTROLYZER, false));
        s.add(gui("qa07_cryogenic_liquefier", LIQUEFIER, false));
        java.util.Map<String, ShowcaseDriver.Shot> byName = new java.util.HashMap<>();
        rocketry.shots().forEach(shot -> byName.put(shot.name(), shot));
        FLIGHT.forEach(n -> s.add(byName.get(n)));
        s.add(suitHud("qa20_moon_suit_hud"));
        for (String n : TURKISH) {
            ShowcaseDriver.Shot shot = byName.get(n);
            s.add(new ShowcaseDriver.Shot("tr_" + n, shot.time(), shot.x(), shot.y(), shot.z(), shot.yaw(), shot.pitch(), shot.hideGui(),
                    shot.camera(), shot.minTicks(), shot.serverSetup(), shot.clientSetup()));
        }
        s.add(suitHud("tr_qa21_moon_suit_hud"));
        return s;
    }

    /** Full EVA suit, a filled tank, standing on the Moon near the landing site: the suit HUD. */
    private static ShowcaseDriver.Shot suitHud(String name) {
        return new ShowcaseDriver.Shot(name, 6000, 0, 0, 0, 0F, 0F, false, CameraType.FIRST_PERSON, 80, p -> {
            p.stopRiding();
            p.closeContainer();
            ServerLevel moon = p.server.getLevel(com.pizzaparker07.spacebreaker.content.rocketry.nav.Location.MOON_SURFACE.dimension());
            if (moon == null) {
                return;
            }
            moon.getChunk(212 >> 4, 212 >> 4);
            int ground = moon.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 212, 212);
            p.teleportTo(moon, 212.5, ground + 0.1, 212.5, 135F, 8F);
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SuitModule.EVA_HELMET.get()));
            ItemStack torso = new ItemStack(SuitModule.EVA_CHESTPLATE.get());
            com.pizzaparker07.spacebreaker.content.suit.SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
            com.pizzaparker07.spacebreaker.content.suit.SuitTanks.insert(torso, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
            p.setItemSlot(EquipmentSlot.CHEST, torso);
            p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(SuitModule.EVA_LEGGINGS.get()));
            p.setItemSlot(EquipmentSlot.FEET, new ItemStack(SuitModule.EVA_BOOTS.get()));
            p.setItemSlot(EquipmentSlot.MAINHAND, OxygenTankItem.filled(SuitModule.OXYGEN_TANK.get()));
        }, null);
    }

    private ShowcaseDriver.Shot gui(String name, BlockPos pos, boolean lab) {
        return new ShowcaseDriver.Shot(name, 6000, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 3.5, 180F, 20F, false,
                CameraType.FIRST_PERSON, 40, p -> {
                    p.closeContainer();
                    p.getAbilities().flying = false;
                    if (lab && p.serverLevel().getBlockEntity(pos) instanceof ResearchLabBlockEntity be) {
                        be.setResearcher(p);
                        p.openMenu(be, buf -> buf.writeBlockPos(pos));
                    } else {
                        MachineBlock.openMenu(p.serverLevel(), pos, p);
                    }
                }, null);
    }
}
