package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.moon.MoonModule;
import com.pizzaparker07.spacebreaker.content.moon.entity.MoonEntities;
import com.pizzaparker07.spacebreaker.content.worldgen.PlanetTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.function.Supplier;

/**
 * Every Moon creature in the same light on the same neutral pad (dev review, -Pscenario=moon_creatures), framed like
 * the outer creature lineup: a three-quarter view and a profile of each at a distance scaled to its size. Set
 * CREATURES=name,name to shoot only some.
 */
final class MoonCreaturesScenario extends OuterShowcase {
    private record C(String name, Supplier<? extends EntityType<? extends Mob>> type, double lift) {
    }

    private static final List<C> ALL = List.of(
            new C("regolith_tick", MoonEntities.REGOLITH_TICK, 0),
            new C("elder_regolith_tick", MoonEntities.ELDER_REGOLITH_TICK, 0),
            new C("dust_swimmer", MoonEntities.DUST_SWIMMER, 0),
            new C("glimmer_beetle", MoonEntities.GLIMMER_BEETLE, 0),
            new C("heat_siphon", MoonEntities.HEAT_SIPHON, 0),
            new C("survey_drone", MoonEntities.SURVEY_DRONE, 1.0));

    @Override
    public ResourceKey<Level> dimension() {
        return MoonModule.MOON;
    }

    @Override
    protected void build(ServerLevel level, PlanetTerrain t) {
        String only = System.getenv("CREATURES");
        int y0 = (int) ground(level, SX, SZ) + 6;
        int i = 0;
        for (C c : ALL) {
            if (only != null && !only.isBlank() && !List.of(only.split(",")).contains(c.name())) {
                continue;
            }
            int x = SX + i * 24;
            i++;
            for (int dx = -9; dx <= 9; dx++) {
                for (int dz = -9; dz <= 9; dz++) {
                    for (int y = y0; y <= y0 + 10; y++) {
                        level.setBlock(new BlockPos(x + dx, y, SZ + dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    Block pad = (dx + dz & 1) == 0 ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.GRAY_CONCRETE;
                    level.setBlock(new BlockPos(x + dx, y0 - 1, SZ + dz), pad.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            Mob m = mob(level, c.type().get(), x, y0 + c.lift(), SZ, 0F);
            double size = m == null ? 1 : Math.max(m.getBbWidth(), m.getBbHeight());
            double d = 0.8 + size * 1.25;
            double eye = y0 + c.lift() + (m == null ? 0.5 : m.getBbHeight() * 0.5);
            double ax = x + 0.5 + d * 0.7;
            double az = SZ + 0.5 + d * 0.7;
            double ay = eye + d * 0.35;
            shot(c.name() + "_a", ax, ay, az, MoonScenario.yaw(x + 0.5 - ax, SZ + 0.5 - az),
                    (float) Math.toDegrees(Math.atan2(ay - eye, d)), 60, at(0.25), null);
            double px = x + 0.5 + d;
            shot(c.name() + "_b", px, eye, SZ + 0.5, 90F, 0F, 40, at(0.25), null);
        }
    }
}
