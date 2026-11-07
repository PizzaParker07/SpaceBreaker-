package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.machines.SolarPanelBlockEntity;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Three solar trackers side by side on Mars in the afternoon: clean, half and fully covered in dust. */
final class SolarDustScenario implements ShowcaseScenario {
    private static final int X0 = 24;
    private static final int Z0 = 24;
    private static final float[] COVER = {0F, 0.5F, 1F};
    private int gy;

    @Override
    public ResourceKey<Level> dimension() {
        return com.pizzaparker07.spacebreaker.content.mars.MarsModule.MARS;
    }

    private int ground(ServerLevel level) {
        PlanetTerrain t = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        return (int) Math.floor(t.height(X0, Z0)) + 1;
    }

    @Override
    public void build(ServerLevel level) {
        com.pizzaparker07.spacebreaker.content.mars.MarsWeather.force(level, 0F, 0);
        gy = ground(level);
        BlockState soil = level.getBlockState(new BlockPos(X0, gy - 1, Z0));
        if (soil.isAir()) {
            soil = Blocks.STONE.defaultBlockState();
        }
        for (int x = X0 - 6; x <= X0 + 4; x++) {
            for (int z = Z0 - 4; z <= Z0 + 8; z++) {
                for (int y = gy - 2; y <= gy + 6; y++) {
                    level.setBlock(new BlockPos(x, y, z), y < gy ? soil.getBlock().defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        for (int i = 0; i < COVER.length; i++) {
            BlockPos pos = new BlockPos(X0, gy, Z0 + i * 2);
            level.setBlock(pos, MachinesModule.SOLAR_PANEL.get().defaultBlockState(), 3);
        }
        dust(level);
        // two rovers further along: a clean roof panel and one after a few sols without a whirl
        for (int i = 0; i < 2; i++) {
            com.pizzaparker07.spacebreaker.content.vehicles.RoverEntity r = com.pizzaparker07.spacebreaker.content.vehicles.VehiclesModule.ROVER.get().create(level);
            r.moveTo(X0 - 2.5, gy, Z0 + 0.5 + i * 4.5, 90F, 0F);
            r.installSolar();
            r.setDust(i == 0 ? 1F : 0F);
            level.addFreshEntity(r);
        }
    }

    private void dust(ServerLevel level) {
        for (int i = 0; i < COVER.length; i++) {
            if (level.getBlockEntity(new BlockPos(X0, gy, Z0 + i * 2)) instanceof SolarPanelBlockEntity be) {
                be.setDust(COVER[i]);
            }
        }
    }

    private Consumer<ServerPlayer> at(double dayFraction) {
        return p -> {
            PlanetTime.setDayFraction(p.serverLevel(), dayFraction);
            dust(p.serverLevel());
        };
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        ServerLevel level = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer().getLevel(dimension());
        gy = ground(level);
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        // from the east in the morning, cells turned towards the camera; clean on the right, fully covered on the left
        s.add(new ShowcaseDriver.Shot("solar_dust_01_row", 0, X0 + 4.2, gy + 2.6, Z0 + 2.5, 90F, 28F, true,
                CameraType.FIRST_PERSON, 80, at(0.14), null));
        // the rovers from above: dusty roof panel at the front, clean behind
        s.add(new ShowcaseDriver.Shot("solar_dust_02_rovers", 0, X0 - 2.5, gy + 4.6, Z0 - 3.0, 0F, 48F, true,
                CameraType.FIRST_PERSON, 80, at(0.14), null));
        return s;
    }
}
