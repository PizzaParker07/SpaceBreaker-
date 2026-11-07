package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.vehicles.RoverEntity;
import com.pizzaparker07.spacebreaker.content.vehicles.VehiclesModule;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** The amphibious rover afloat on a lake (water on Earth, liquid methane on Titan), with one parked on the shore. */
final class AmphibiousScenario implements ShowcaseScenario {
    private static final int X0 = 24;
    private static final int Z0 = 24;
    private final ResourceKey<Level> dimension;
    private final boolean titan;
    private int gy;

    AmphibiousScenario(ResourceKey<Level> dimension, boolean titan) {
        this.dimension = dimension;
        this.titan = titan;
    }

    @Override
    public ResourceKey<Level> dimension() {
        return dimension;
    }

    private int ground(ServerLevel level) {
        if (level.getChunkSource().getGenerator() instanceof PlanetChunkGenerator g) {
            return (int) Math.floor(g.terrain().height(X0, Z0)) + 1;
        }
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X0, Z0);
    }

    @Override
    public void build(ServerLevel level) {
        gy = ground(level);
        BlockState liquid = titan ? com.pizzaparker07.spacebreaker.content.saturn.SaturnModule.TITAN_METHANE.get().defaultBlockState()
                : Blocks.WATER.defaultBlockState();
        BlockState shore = level.getBlockState(new BlockPos(X0, gy - 1, Z0));
        if (shore.isAir() || !shore.getFluidState().isEmpty()) {
            shore = Blocks.STONE.defaultBlockState();
        }
        for (int x = X0 - 10; x <= X0 + 10; x++) {
            for (int z = Z0 - 6; z <= Z0 + 14; z++) {
                boolean lake = z >= Z0 && Math.abs(x - X0) <= 8;
                for (int y = gy - 4; y <= gy + 6; y++) {
                    BlockState s;
                    if (y < gy - 3) {
                        s = shore;
                    } else if (y < gy) {
                        s = lake ? liquid : shore;
                    } else {
                        s = Blocks.AIR.defaultBlockState();
                    }
                    level.setBlock(new BlockPos(x, y, z), s, 2);
                }
            }
        }
        RoverEntity afloat = VehiclesModule.AMPHIBIOUS_ROVER.get().create(level);
        afloat.moveTo(X0 + 0.5, gy - 0.6, Z0 + 6.5, 200F, 0F);
        afloat.setEnergy(900_000);
        afloat.setCabinOxygen(100_000);
        level.addFreshEntity(afloat);
        RoverEntity parked = VehiclesModule.AMPHIBIOUS_ROVER.get().create(level);
        parked.moveTo(X0 - 5.5, gy, Z0 - 3.5, 150F, 0F);
        parked.setEnergy(900_000);
        parked.installSolar();
        level.addFreshEntity(parked);
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        ServerLevel level = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer().getLevel(dimension());
        gy = ground(level);
        String w = titan ? "titan" : "earth";
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        s.add(new ShowcaseDriver.Shot(w + "_amphibious", titan ? 0 : 6000, X0 + 6.5, gy + 3.2, Z0 - 4.5, MoonScenario.yaw(-6, 10), 18F, true,
                CameraType.FIRST_PERSON, 100, p -> {
                    if (titan) {
                        PlanetTime.setDayFraction(p.serverLevel(), 0.3);
                    }
                }, null));
        return s;
    }
}
