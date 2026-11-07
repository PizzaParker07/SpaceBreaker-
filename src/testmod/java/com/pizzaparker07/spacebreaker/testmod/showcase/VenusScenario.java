package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.venus.VenusClimate;
import com.pizzaparker07.spacebreaker.content.venus.VenusModule;
import com.pizzaparker07.spacebreaker.content.venus.dungeon.AerostatPiece;
import com.pizzaparker07.spacebreaker.content.venus.entity.VenusEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetChunkGenerator;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTime;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Venus: the dim orange surface (plains, tesserae, a shield volcano, metal-frosted peaks), the whiteout inside the
 * cloud deck, the bright world above it with the cloud sea, the derelict aerostat, a composite platform with the
 * blocks on display, and the cloud creatures.
 */
final class VenusScenario implements ShowcaseScenario {
    private PlanetTerrain terrain;
    static final int AX = 60;
    static final int AZ = 60;
    static final int AY = VenusClimate.CLOUD_TOP - 8;
    static final int PX = -20;
    static final int PZ = 20;
    static final int PY = VenusClimate.CLOUD_TOP + 6;
    /** A deployed aerostat platform kit. */
    static final int KX = -60;
    static final int KY = VenusClimate.CLOUD_TOP + 8;
    static final int KZ = -40;

    @Override
    public ResourceKey<Level> dimension() {
        return VenusModule.VENUS;
    }

    private PlanetTerrain terrain() {
        if (terrain == null) {
            ServerLevel level = Minecraft.getInstance().getSingleplayerServer().getLevel(VenusModule.VENUS);
            terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        }
        return terrain;
    }

    @Override
    public void build(ServerLevel level) {
        terrain = ((PlanetChunkGenerator) level.getChunkSource().getGenerator()).terrain();
        MarsScenario.placePiece(level, new AerostatPiece(17L, AX, AY, AZ, Direction.NORTH));
        // a composite platform above the clouds with every Venus block on display
        for (int x = PX - 6; x <= PX + 6; x++) {
            for (int z = PZ - 4; z <= PZ + 4; z++) {
                level.setBlock(new BlockPos(x, PY, z), VenusModule.COMPOSITE_PANEL.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                boolean edge = x == PX - 6 || x == PX + 6 || z == PZ - 4 || z == PZ + 4;
                if (edge) {
                    level.setBlock(new BlockPos(x, PY + 1, z), Blocks.IRON_BARS.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        Block[] show = {VenusModule.VENUS_BASALT.get(), VenusModule.VENUS_SOIL.get(), VenusModule.TESSERA_ROCK.get(), VenusModule.SULFUR_CRUST.get(),
                VenusModule.METAL_FROST.get(), VenusModule.COMPOSITE_PANEL.get(), VenusModule.AEROGEL.get(), VenusModule.ENVELOPE_FABRIC.get()};
        for (int i = 0; i < show.length; i++) {
            level.setBlock(new BlockPos(PX - 5 + i + i / 4, PY + 1, PZ - 2), show[i].defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        com.pizzaparker07.spacebreaker.content.venus.AerostatKitItem.deploy(level, new BlockPos(KX, KY, KZ));
        spawn(level, VenusEntities.CLOUD_JELLY.get(), PX + 3, PY + 4, PZ + 9, 200F);
        spawn(level, VenusEntities.ACID_RAY.get(), PX - 4, PY + 3, PZ + 8, 160F);
    }

    private static void spawn(ServerLevel level, EntityType<? extends Mob> type, double x, double y, double z, float yaw) {
        Mob mob = type.create(level);
        if (mob != null) {
            mob.moveTo(x + 0.5, y, z + 0.5, yaw, 0F);
            mob.setYBodyRot(yaw);
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            mob.setSilent(true);
            level.addFreshEntity(mob);
        }
    }

    private static Consumer<ServerPlayer> at(double f) {
        return p -> PlanetTime.setDayFraction(p.serverLevel(), f);
    }

    private double ground(double x, double z) {
        return Math.floor(terrain().height(x, z)) + 1;
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        PlanetTerrain t = terrain();
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        CameraType fp = CameraType.FIRST_PERSON;
        s.add(new ShowcaseDriver.Shot("01_surface_plains", 0, 0.5, ground(0.5, 0.5) + 3, 0.5, 30F, 4F, true, fp, 220, at(0.25), MoonScenario::farView));
        double[] tes = MoonScenario.find(t, (x, z) -> t.column(x, z).tessera > 0.8, 4000);
        if (tes != null) {
            s.add(new ShowcaseDriver.Shot("02_tesserae", 0, tes[0], ground(tes[0], tes[1]) + 8, tes[1], 60F, 15F, true, fp, 220, at(0.25), null));
        }
        double vx = 900 + 520 * 0.12;
        s.add(new ShowcaseDriver.Shot("03_volcano_summit", 0, vx, ground(vx, -500) + 6, -500, 90F, 12F, true, fp, 220, at(0.25), null));
        s.add(new ShowcaseDriver.Shot("04_inside_clouds", 0, AX - 20.5, VenusClimate.CLOUD_BASE + 20, AZ + 7.5, -90F, 0F, true, fp, 120, at(0.25), null));
        s.add(new ShowcaseDriver.Shot("05_cloud_sea", 0, PX + 0.5, PY + 6, PZ - 12.5, 160F, 8F, true, fp, 160, at(0.18), null));
        s.add(new ShowcaseDriver.Shot("06_platform_blocks", 0, PX + 0.5, PY + 3.2, PZ + 4.5, 180F, 30F, true, fp, 80, at(0.2), null));
        s.add(new ShowcaseDriver.Shot("07_cloud_creatures", 0, PX + 0.5, PY + 3.6, PZ + 1.5, 0F, -6F, true, fp, 80, at(0.2), null));
        s.add(new ShowcaseDriver.Shot("08_aerostat", 0, AX + 42.5, AY + 14, AZ + 38.5, 140F, 6F, true, fp, 160, at(0.22), null));
        s.add(new ShowcaseDriver.Shot("08b_aerostat_gondola_outside", 0, AX + 23.5, AY + 5, AZ - 11.5, 20F, 7F, true, fp, 100, at(0.22), null));
        s.add(new ShowcaseDriver.Shot("09_aerostat_gondola", 0, AX + 12.5, AY + 3.6, AZ + 7.5, -40F, 4F, true, fp, 80, at(0.22), null));
        s.add(new ShowcaseDriver.Shot("09b_aerostat_bridge", 0, AX + 17.5, AY + 3.6, AZ + 8.5, -90F, 4F, true, fp, 80, at(0.22), null));
        s.add(new ShowcaseDriver.Shot("11_aerostat_kit_base", 0, KX + 18.5, KY + 9, KZ + 18.5, 135F, 4F, true, fp, 140, at(0.22), null));
        s.add(new ShowcaseDriver.Shot("10_sunset_above_clouds", 0, PX + 0.5, PY + 6, PZ + 0.5, -90F, 2F, true, fp, 120, at(0.49), null));
        return s;
    }
}
