package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.belt.dungeon.LanderPiece;
import com.pizzaparker07.spacebreaker.content.belt.world.BasinFeature;
import com.pizzaparker07.spacebreaker.content.belt.world.GrabenFeature;
import com.pizzaparker07.spacebreaker.content.uranus.UranusData;
import com.pizzaparker07.spacebreaker.content.uranus.UranusModule;
import com.pizzaparker07.spacebreaker.content.uranus.entity.UranusEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Titania ("titania"): Uranus in the sky, a great canyon from its rim and its floor, the crater basin, a scree roller, a lander,
 * the canyon rim outpost from the uplands and from below. */
final class TitaniaScenario extends OuterShowcase {
    @Override
    public ResourceKey<Level> dimension() {
        return UranusModule.TITANIA;
    }

    @Override
    protected void build(ServerLevel level, PlanetTerrain t) {
        parent(level, t, "01_uranus_over_titania", 0, 0, 0.62, 16);
        GrabenFeature.Graben g = UranusData.canyons().grabens().getFirst();
        double cz = -g.latitude() / 90.0 * t.profile().poleDistance();
        double a = Math.toRadians(g.angleDeg());
        double nx = -Math.sin(a);
        double nz = Math.cos(a);
        double ux = Math.cos(a);
        double uz = Math.sin(a);
        look(level, "02_canyon_rim", g.x() + nx * (g.width() / 2 + 18) - ux * 160, cz + nz * (g.width() / 2 + 18) - uz * 160, 6,
                g.x() + ux * 80, cz + uz * 80, at(0.1), 240, MoonScenario::farView);
        look(level, "03_canyon_floor", g.x() - ux * 220, cz - uz * 220, 3, g.x() + ux * 120, cz + uz * 120, at(0.12), 240, MoonScenario::farView);
        var basin = UranusData.greatCrater();
        double bz = BasinFeature.centreZ(t, basin);
        look(level, "04_great_crater", basin.x() + basin.radius() * 0.6, bz + basin.radius() * 0.4, 8, basin.x(), bz, at(0.1), 240,
                MoonScenario::farView);
        int y0 = stage(level, UranusModule.TITANIA_REGOLITH.get().defaultBlockState(), UranusModule.TITANIA_ICE.base().get().defaultBlockState(),
                new Block[]{UranusModule.TITANIA_REGOLITH.get(), UranusModule.TITANIA_ICE.base().get(), UranusModule.CANYON_WALL_ICE.get(),
                        UranusModule.TITANIA_CARBON_ORE.get()},
                new Block[]{UranusModule.TITANIA_ICE.stairs().get(), UranusModule.TITANIA_ICE.slab().get(), UranusModule.TITANIA_ICE.wall().get()});
        mob(level, UranusEntities.SCREE_ROLLER.get(), SX - 2, y0 + 1, SZ + 6, 150F);
        mob(level, UranusEntities.SCREE_ROLLER.get(), SX + 3, y0 + 1, SZ + 7, 210F);
        shot("05_scree_roller", SX + 0.5, y0 + 2.4, SZ + 10.5, 180F, 20F, 60, at(0.12), null);
        lander(level, LanderPiece.Variant.TITANIA, "06_crashed_lander", 0.12);
        // the canyon rim outpost: the first rim spot down the canyon from its middle, on the near side
        int W = com.pizzaparker07.spacebreaker.content.uranus.dungeon.CanyonOutpostPiece.W;
        int D = com.pizzaparker07.spacebreaker.content.uranus.dungeon.CanyonOutpostPiece.D;
        int mid = com.pizzaparker07.spacebreaker.content.uranus.dungeon.CanyonOutpostPiece.MID;
        int f = com.pizzaparker07.spacebreaker.content.uranus.dungeon.CanyonOutpostPiece.FLOOR;
        for (int v = 40; v <= 120; v++) {
            double px = g.x() - ux * 300 + nx * v;
            double pz = cz - uz * 300 + nz * v;
            int x = (int) Math.floor(px - W / 2.0);
            int z = (int) Math.floor(pz - D / 2.0);
            var facing = com.pizzaparker07.spacebreaker.content.uranus.dungeon.UranusStructures.rimFacing(t, x, z);
            if (facing.isEmpty()) {
                continue;
            }
            var probe = new com.pizzaparker07.spacebreaker.content.uranus.dungeon.CanyonOutpostPiece(9L, x, 0, z, facing.get());
            var hut = probe.world(mid, f, 3);
            int y = (int) ground(level, hut.getX() + 0.5, hut.getZ() + 0.5);
            var outpost = new com.pizzaparker07.spacebreaker.content.uranus.dungeon.CanyonOutpostPiece(9L, x, y, z, facing.get());
            MarsScenario.placePiece(level, outpost);
            // from the side along the rim: the hut, the balcony out over the edge, the canyon below
            var side = outpost.world(mid + 22, f + 6, 6);
            var look = outpost.world(mid, f, 12);
            shot("07_canyon_outpost", side.getX() + 0.5, side.getY(), side.getZ() + 0.5,
                    MoonScenario.yaw(look.getX() - side.getX(), look.getZ() - side.getZ()), 16F, 200, at(0.1), MoonScenario::farView);
            var tip = outpost.world(mid, f, 16);
            var below = outpost.world(mid - 8, f - 30, 34);
            double floorY = ground(level, below.getX() + 0.5, below.getZ() + 0.5) + 2.5;
            shot("08_canyon_outpost_from_below", below.getX() + 0.5, floorY, below.getZ() + 0.5,
                    MoonScenario.yaw(tip.getX() - below.getX(), tip.getZ() - below.getZ()), -38F, 200, at(0.1), MoonScenario::farView);
            // inside, from the back door: the bench, the store, the chairs at the long window
            var door = outpost.world(mid, f, 1);
            var win = outpost.world(mid, f, 8);
            shot("09_canyon_outpost_inside", door.getX() + 0.5, door.getY() + 1.6, door.getZ() + 0.5,
                    MoonScenario.yaw(win.getX() - door.getX(), win.getZ() - door.getZ()), 14F, 200, at(0.1), null);
            break;
        }
    }
}
