package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.machines.MachineEntry;
import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Every industry machine in rows on a stone pad, fronts to the camera and lit (running): a review sheet for the
 * models. Single-block machines in rows of six; the multiblocks (pumpjack, refinery) on their own row behind.
 */
final class MachineLineupScenario implements ShowcaseScenario {
    static final int X0 = 0;
    static final int Z0 = 0;
    private int gy;
    @SuppressWarnings("unchecked")
    private static final java.util.function.Supplier<? extends Block>[] EXTRA = new java.util.function.Supplier[]{
            MachinesModule.FLUID_TANK, MachinesModule.PRESSURIZED_TANK, MachinesModule.CRYOGENIC_TANK,
            MachinesModule.SOLAR_PANEL, MachinesModule.SOLAR_ARRAY, MachinesModule.POWER_MONITOR};

    static List<MachineEntry<?, ?>> singles() {
        List<MachineEntry<?, ?>> out = new ArrayList<>();
        for (MachineEntry<?, ?> e : MachinesModule.MACHINES) {
            if (e != MachinesModule.PUMPJACK && e != MachinesModule.REFINERY) {
                out.add(e);
            }
        }
        return out;
    }

    @Override
    public void build(ServerLevel level) {
        gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X0, Z0);
        for (int x = X0 - 4; x <= X0 + 16; x++) {
            for (int z = Z0 - 22; z <= Z0 + 6; z++) {
                level.setBlock(new BlockPos(x, gy - 1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                for (int y = gy; y < gy + 8; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        List<MachineEntry<?, ?>> list = singles();
        for (int i = 0; i < list.size(); i++) {
            int row = i / 6, col = i % 6;
            BlockPos p = new BlockPos(X0 + col * 2, gy, Z0 - row * 3);
            BlockState s = list.get(i).block().get().defaultBlockState();
            if (s.hasProperty(MachineBlock.FACING)) {
                s = s.setValue(MachineBlock.FACING, Direction.SOUTH);
            }
            if (s.hasProperty(MachineBlock.LIT)) {
                s = s.setValue(MachineBlock.LIT, true);
            }
            if (s.hasProperty(com.pizzaparker07.spacebreaker.content.machines.machine.BatteryBankBlock.CHARGE)) {
                s = s.setValue(com.pizzaparker07.spacebreaker.content.machines.machine.BatteryBankBlock.CHARGE, 3);
            }
            level.setBlock(p, s, Block.UPDATE_ALL);
        }
        // other industry blocks: tanks, solar trackers, the power monitor
        for (int i = 0; i < EXTRA.length; i++) {
            BlockState es = EXTRA[i].get().defaultBlockState();
            if (es.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
                es = es.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
            }
            level.setBlock(new BlockPos(X0 + i * 2, gy, Z0 - 12), es, Block.UPDATE_ALL);
        }
        // the two multiblocks behind the rest, running
        BlockPos pj = new BlockPos(X0 + 2, gy, Z0 - 18);
        BlockState pjs = MachinesModule.PUMPJACK.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH)
                .setValue(MachineBlock.LIT, true);
        level.setBlock(pj, pjs, Block.UPDATE_ALL);
        MachinesModule.PUMPJACK.block().get().build(level, pj, pjs);
        BlockPos rf = new BlockPos(X0 + 9, gy, Z0 - 18);
        BlockState rfs = MachinesModule.REFINERY.block().get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH)
                .setValue(MachineBlock.LIT, true);
        level.setBlock(rf, rfs, Block.UPDATE_ALL);
        MachinesModule.REFINERY.block().get().build(level, rf, rfs);
        // hold the LIT (running) look for the photos: idle machines would switch it off on their first tick
        level.getServer().tickRateManager().setFrozen(true);
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        var level = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer().overworld();
        gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X0, Z0);
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        int rows = (singles().size() + 5) / 6;
        // one close shot per row of machines, looking north at their fronts
        for (int r = 0; r < rows; r++) {
            s.add(new ShowcaseDriver.Shot("lineup_row" + r, 6000, X0 + 5.5, gy + 1.9, Z0 - r * 3 + 3.6, 180F, 14F, true,
                    CameraType.FIRST_PERSON, 40, null, null));
            // closer halves: three machines each, for detail review
            s.add(new ShowcaseDriver.Shot("lineup_row" + r + "a", 6000, X0 + 2.5, gy + 1.6, Z0 - r * 3 + 2.6, 180F, 16F, true,
                    CameraType.FIRST_PERSON, 40, null, null));
            s.add(new ShowcaseDriver.Shot("lineup_row" + r + "b", 6000, X0 + 8.5, gy + 1.6, Z0 - r * 3 + 2.6, 180F, 16F, true,
                    CameraType.FIRST_PERSON, 40, null, null));
        }
        // three-quarter close-ups (front and right-hand side) of every machine, all from the same angle
        List<MachineEntry<?, ?>> list = singles();
        for (MachineEntry<?, ?> e : list) {
            int i = list.indexOf(e);
            double bx = X0 + (i % 6) * 2 + 0.5, bz = Z0 - (i / 6) * 3 + 0.5;
            s.add(new ShowcaseDriver.Shot("polish_" + e.block().getId().getPath(), 6000, bx + 1.25, gy + 1.35, bz + 1.65, 143F, 24F, true,
                    CameraType.FIRST_PERSON, 40, null, null));
        }
        for (int i = 0; i < EXTRA.length; i++) {
            double bx = X0 + i * 2 + 0.5, bz = Z0 - 12 + 0.5;
            String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(EXTRA[i].get()).getPath();
            s.add(new ShowcaseDriver.Shot("polish_" + id, 6000, bx + 1.25, gy + 1.35, bz + 1.65, 143F, 24F, true,
                    CameraType.FIRST_PERSON, 40, null, null));
        }
        s.add(new ShowcaseDriver.Shot("polish_pumpjack", 6000, X0 + 2.5 + 4.0, gy + 4.0, Z0 - 17.5 + 4.5, 138F, 22F, true,
                CameraType.FIRST_PERSON, 40, null, null));
        s.add(new ShowcaseDriver.Shot("polish_refinery", 6000, X0 + 9.5 + 3.5, gy + 3.5, Z0 - 17.5 + 4.5, 142F, 15F, true,
                CameraType.FIRST_PERSON, 40, null, null));
        s.add(new ShowcaseDriver.Shot("lineup_blocks", 6000, X0 + 5.5, gy + 2.3, Z0 - 12 + 4.0, 180F, 18F, true,
                CameraType.FIRST_PERSON, 40, null, null));
        return s;
    }
}
