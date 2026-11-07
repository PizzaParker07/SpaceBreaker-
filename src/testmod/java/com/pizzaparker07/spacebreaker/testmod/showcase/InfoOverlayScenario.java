package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Block info overlay showcase (-Pscenario=info -PwithJade=true, or -PwithWthit=true): a solar panel with dust on its
 * cells under the crosshair, so Jade or WTHIT shows the dust and output line of compat.info.BlockInfoLines.
 */
final class InfoOverlayScenario implements ShowcaseScenario {
    private static final int F = ShowcaseBuilder.FLOOR;
    private static final BlockPos PANEL = new BlockPos(0, F + 1, 3);

    @Override
    public void build(ServerLevel level) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -1; z <= 6; z++) {
                for (int y = 1; y <= 4; y++) {
                    level.setBlock(new BlockPos(x, F + y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(PANEL, MachinesModule.SOLAR_PANEL.get().defaultBlockState(), 3);
        if (level.getBlockEntity(PANEL) instanceof SolarPanelBlockEntity panel) {
            panel.setDust(0.42F);
        }
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        return List.of(new ShowcaseDriver.Shot("01_solar_panel_dust", 6000, 0.5, F + 2.62, 1.0, 0F, 35F, false,
                CameraType.FIRST_PERSON, 80, null, null));
    }
}
