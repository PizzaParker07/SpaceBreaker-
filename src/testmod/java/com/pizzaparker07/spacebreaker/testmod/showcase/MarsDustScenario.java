package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.mars.MarsModule;
import com.pizzaparker07.spacebreaker.content.mars.entity.DustWhirl;
import com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Mars on a calm afternoon: a dust whirl wandering past a little solar farm, and dusty next to freshly brushed panels. */
final class MarsDustScenario implements ShowcaseScenario {
    /** Corner of the solar farm. */
    private static final int FX = 30;
    private static final int FZ = 60;
    private PlanetTerrain terrain;

    @Override
    public ResourceKey<Level> dimension() {
        return MarsModule.MARS;
    }

    private PlanetTerrain terrain() {
        if (terrain == null) {
            ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getLevel(dimension());
            terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        }
        return terrain;
    }

    private int ground() {
        return (int) Math.floor(terrain().height(FX + 4, FZ + 2)) + 1;
    }

    @Override
    public void build(ServerLevel level) {
        terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        int y = ground();
        // a level pad of regolith and two rows of four panels
        for (int x = -2; x <= 10; x++) {
            for (int z = -2; z <= 6; z++) {
                level.setBlock(new BlockPos(FX + x, y - 1, FZ + z), MarsModule.MARTIAN_REGOLITH.get().defaultBlockState(), 3);
                for (int dy = 0; dy < 6; dy++) {
                    level.setBlock(new BlockPos(FX + x, y + dy, FZ + z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            level.setBlock(new BlockPos(FX + i * 2, y, FZ), MachinesModule.SOLAR_PANEL.get().defaultBlockState(), 3);
            level.setBlock(new BlockPos(FX + i * 2, y, FZ + 3), MachinesModule.SOLAR_PANEL.get().defaultBlockState(), 3);
        }
    }

    private static Consumer<ServerPlayer> afternoon(Consumer<ServerLevel> then) {
        return p -> {
            PlanetTime.setDayFraction(p.serverLevel(), 0.33);
            then.accept(p.serverLevel());
        };
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        int y = ground();
        CameraType fp = CameraType.FIRST_PERSON;
        // a whirl coming in across the plain toward the panels
        s.add(new ShowcaseDriver.Shot("23_dust_whirl", 0, FX + 3.5, y + 3, FZ - 12.5, 0F, 6F, true, fp, 160, afternoon(level -> {
            level.getEntities(MarsEntities.DUST_WHIRL.get(), e -> true).forEach(e -> e.discard());
            DustWhirl whirl = MarsEntities.spawnWhirl(level, new BlockPos(FX - 4, y, FZ + 9));
            if (whirl != null) {
                whirl.setHeading((float) Math.toRadians(-30));
                whirl.setLife(6000);
            }
        }), MoonScenario::farView));
        // the farm after a few dusty sols: the back row still covered, the front row swept clean by a whirl
        s.add(new ShowcaseDriver.Shot("24_whirl_swept_panels", 0, FX - 6.5, y + 4.5, FZ + 1.5, -90F, 30F, true, fp, 140, afternoon(level -> {
            level.getEntities(MarsEntities.DUST_WHIRL.get(), e -> true).forEach(e -> e.discard());
            for (int i = 0; i < 4; i++) {
                setDust(level, new BlockPos(FX + i * 2, y, FZ), 0F);
                setDust(level, new BlockPos(FX + i * 2, y, FZ + 3), 1F);
            }
            DustWhirl whirl = MarsEntities.spawnWhirl(level, new BlockPos(FX + 5, y, FZ - 1));
            if (whirl != null) {
                whirl.setHeading((float) Math.toRadians(-90));
                whirl.setLife(6000);
            }
        }), MoonScenario::farView));
        return s;
    }

    private static void setDust(ServerLevel level, BlockPos pos, float dust) {
        if (level.getBlockEntity(pos) instanceof com.pizzaparker07.spacebreaker.core.env.DustLoad.Dusty d) {
            d.setDust(dust);
        }
    }
}
