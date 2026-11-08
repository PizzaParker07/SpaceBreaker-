package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.content.belt.entity.BeltEntities;
import com.pizzaparker07.spacebreaker.content.jupiter.JupiterModule;
import com.pizzaparker07.spacebreaker.content.jupiter.entity.JupiterEntities;
import com.pizzaparker07.spacebreaker.content.neptune.entity.NeptuneEntities;
import com.pizzaparker07.spacebreaker.content.pluto.entity.PlutoEntities;
import com.pizzaparker07.spacebreaker.content.saturn.entity.SaturnEntities;
import com.pizzaparker07.spacebreaker.content.uranus.entity.UranusEntities;
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
 * Every creature of the outer worlds in the same light on the same neutral pad (dev review, -Pscenario=outer_creatures):
 * a three-quarter view and a profile of each, the camera kept at a distance scaled to the creature's size so that their
 * silhouettes compare. Set CREATURES=name,name to shoot only some.
 */
final class OuterCreaturesScenario extends OuterShowcase {
    private record C(String name, Supplier<? extends EntityType<? extends Mob>> type, double lift) {
    }

    private static final List<C> ALL = List.of(
            new C("rock_mite", BeltEntities.ROCK_MITE, 0),
            new C("glow_serpent", JupiterEntities.GLOW_SERPENT, 0.6),
            new C("sulfur_crab", JupiterEntities.SULFUR_CRAB, 0),
            new C("groove_grazer", JupiterEntities.GROOVE_GRAZER, 0),
            new C("crater_hopper", JupiterEntities.CRATER_HOPPER, 0),
            new C("methane_skater", SaturnEntities.METHANE_SKATER, 0),
            new C("geyser_drifter", SaturnEntities.GEYSER_DRIFTER, 1.2),
            new C("tholin_worm", SaturnEntities.THOLIN_WORM, 0),
            new C("cliff_clinger", UranusEntities.CLIFF_CLINGER, 0),
            new C("frost_moth", UranusEntities.FROST_MOTH, 1.0),
            new C("scree_roller", UranusEntities.SCREE_ROLLER, 0),
            new C("gloam_hopper", UranusEntities.GLOAM_HOPPER, 0),
            new C("rim_burrower", UranusEntities.RIM_BURROWER, 0),
            new C("plume_beetle", NeptuneEntities.PLUME_BEETLE, 0),
            new C("nitrogen_slider", PlutoEntities.NITROGEN_SLIDER, 0),
            new C("frost_tortoise", PlutoEntities.FROST_TORTOISE, 0));

    @Override
    public ResourceKey<Level> dimension() {
        return JupiterModule.CALLISTO;
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
            // a neutral pad high above the ground, air all round so nothing breaks the silhouette
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
            // bats (the frost moth) are born resting; REST=1 keeps that pose, otherwise they are shot in flight
            if (m instanceof net.minecraft.world.entity.ambient.Bat bat && System.getenv("REST") == null) {
                bat.setResting(false);
            }
            double size = m == null ? 1 : Math.max(m.getBbWidth(), m.getBbHeight());
            double d = 0.8 + size * 1.25;
            double eye = y0 + c.lift() + (m == null ? 0.5 : m.getBbHeight() * 0.5);
            // three-quarter front (the mob looks toward +z), then the profile
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
