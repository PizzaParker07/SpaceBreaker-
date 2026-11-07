package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece;
import com.pizzaparker07.spacebreaker.content.belt.world.CoronaFeature;
import com.pizzaparker07.spacebreaker.content.uranus.UranusData;
import com.pizzaparker07.spacebreaker.content.uranus.UranusModule;
import com.pizzaparker07.spacebreaker.content.uranus.entity.UranusEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Miranda ("miranda"): Uranus with its rings, the great cliff from its foot and from its edge, the ridges of a corona,
 * the blocks with cliff clingers (one on a wall) and a crashed lander.
 */
final class MirandaScenario extends OuterShowcase {
    @Override
    public ResourceKey<Level> dimension() {
        return UranusModule.MIRANDA;
    }

    @Override
    protected void build(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_uranus_over_miranda", 0, 0, 0.62, 18);
        var cliff = UranusData.cliff();
        double cz = -cliff.latitude() / 90.0 * t.profile().poleDistance();
        double a = Math.toRadians(cliff.angleDeg());
        double nx = -Math.sin(a);
        double nz = Math.cos(a);
        double ux = Math.cos(a);
        double uz = Math.sin(a);
        // from the foot, looking back up at the face
        look(level, "02_cliff_from_below", cliff.x() + nx * 110 + ux * 60, cz + nz * 110 + uz * 60, 3, cliff.x() - nx * 5, cz - nz * 5,
                at(0.1), 240, MoonScenario::farView);
        // from the edge, looking along it and down
        double ex = cliff.x() - nx * 6 - ux * 150;
        double ez = cz - nz * 6 - uz * 150;
        look(level, "03_cliff_edge", ex, ez, 4, cliff.x() + nx * 60 + ux * 60, cz + nz * 60 + uz * 60, at(0.1), 240, MoonScenario::farView);
        CoronaFeature.Corona c = UranusData.coronae().coronae().get(1);
        double ccz = -c.latitude() / 90.0 * t.profile().poleDistance();
        look(level, "04_corona_ridges", c.x() + 60, ccz + 50, 18, c.x(), ccz, at(0.12), 220, MoonScenario::farView);
        int y0 = stage(level, UranusModule.MIRANDAN_REGOLITH.get().defaultBlockState(), UranusModule.MIRANDAN_ICE.base().get().defaultBlockState(),
                new Block[]{UranusModule.MIRANDAN_REGOLITH.get(), UranusModule.MIRANDAN_ICE.base().get(), UranusModule.BRIGHT_RIDGE_ICE.get(),
                        UranusModule.DARK_BAND_ICE.get(), UranusModule.CARBON_ORE.get()},
                new Block[]{UranusModule.MIRANDAN_ICE.stairs().get(), UranusModule.MIRANDAN_ICE.slab().get(), UranusModule.MIRANDAN_ICE.wall().get()});
        // a pillar of dark band ice with a clinger on its side
        for (int y = 1; y <= 4; y++) {
            level.setBlock(new BlockPos(SX + 4, y0 + y, SZ + 5), UranusModule.DARK_BAND_ICE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        mob(level, UranusEntities.CLIFF_CLINGER.get(), SX - 2, y0 + 1, SZ + 6, 150F);
        mob(level, UranusEntities.CLIFF_CLINGER.get(), SX + 4, y0 + 2, SZ + 6.2, 0F);
        shot("05_miranda_blocks", SX + 0.5, y0 + 3.4, SZ + 12.5, 180F, 18F, 120, at(0.12), null);
        shot("06_cliff_clinger", SX + 1.5, y0 + 2.6, SZ + 10.5, 180F, 18F, 60, at(0.12), null);
        lander(level, LanderPiece.Variant.MIRANDA, "07_crashed_lander", 0.12);
    }
}
