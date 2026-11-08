package com.pizzaparker07.spacebreaker.testmod.showcase;

import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Every inner-planet creature on one neutral stone pad, facing the camera, frozen: a review sheet for the models. One
 * wide shot of the row and a close shot of each.
 */
final class InnerCreatureLineupScenario implements ShowcaseScenario {
    static final int X0 = 0;
    static final int Z0 = 0;
    /** Creature, its spacing (blocks to the next), and a short label for the shot names. */
    private record Entry(Supplier<? extends EntityType<? extends Mob>> type, int width, String name) {
    }

    private static final List<Entry> CREATURES = List.of(
            new Entry(com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.RUST_BEETLE::get, 3, "rust_beetle"),
            new Entry(com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.STONEGRAZER::get, 4, "stonegrazer"),
            new Entry(com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.CANYON_LURKER::get, 4, "canyon_lurker"),
            new Entry(com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.POLAR_WORM::get, 4, "polar_worm"),
            new Entry(com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.DUST_DEVIL::get, 4, "dust_devil"),
            new Entry(com.pizzaparker07.spacebreaker.content.mars.entity.MarsEntities.TETHER_SPINNER::get, 3, "tether_spinner"),
            new Entry(com.pizzaparker07.spacebreaker.content.venus.entity.VenusEntities.ACID_RAY::get, 4, "acid_ray"),
            new Entry(com.pizzaparker07.spacebreaker.content.venus.entity.VenusEntities.CLOUD_JELLY::get, 4, "cloud_jelly"),
            new Entry(com.pizzaparker07.spacebreaker.content.mercury.MercuryEntities.TERMINATOR_CRAWLER::get, 4, "terminator_crawler"),
            new Entry(com.pizzaparker07.spacebreaker.content.mercury.MercuryEntities.HOLLOW_SNAIL::get, 3, "hollow_snail"));
    private int gy;

    private static int offset(int i) {
        int x = 0;
        for (int k = 0; k < i; k++) {
            x += CREATURES.get(k).width();
        }
        return x;
    }

    @Override
    public void build(ServerLevel level) {
        gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X0, Z0);
        int len = offset(CREATURES.size()) + 4;
        for (int x = X0 - 4; x <= X0 + len; x++) {
            for (int z = Z0 - 6; z <= Z0 + 10; z++) {
                level.setBlock(new BlockPos(x, gy - 1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                for (int y = gy; y < gy + 10; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        for (int i = 0; i < CREATURES.size(); i++) {
            Mob m = CREATURES.get(i).type().get().create(level);
            if (m == null) {
                continue;
            }
            m.moveTo(X0 + offset(i) + 0.5, gy + (m.isNoGravity() ? 1.5 : 0), Z0 + 0.5, 0F, 0F);
            m.setYBodyRot(0F);
            m.setYHeadRot(0F);
            m.setNoAi(true);
            m.setNoGravity(true);
            m.setPersistenceRequired();
            level.addFreshEntity(m);
        }
        level.getServer().tickRateManager().setFrozen(true);
    }

    @Override
    public List<ShowcaseDriver.Shot> shots() {
        List<ShowcaseDriver.Shot> s = new ArrayList<>();
        var level = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer().overworld();
        gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X0, Z0);
        int half = offset(CREATURES.size()) / 2;
        s.add(new ShowcaseDriver.Shot("creatures_row_a", 6000, X0 + offset(3) + 0.5, gy + 3, Z0 + 12.5, 180F, 8F, true,
                CameraType.FIRST_PERSON, 60, null, null));
        s.add(new ShowcaseDriver.Shot("creatures_row_b", 6000, X0 + offset(8) + 0.5, gy + 3, Z0 + 12.5, 180F, 8F, true,
                CameraType.FIRST_PERSON, 60, null, null));
        for (int i = 0; i < CREATURES.size(); i++) {
            s.add(new ShowcaseDriver.Shot("creature_" + CREATURES.get(i).name(), 6000, X0 + offset(i) + 2.0, gy + 1.8, Z0 + 3.2, 150F, 14F, true,
                    CameraType.FIRST_PERSON, 40, null, null));
        }
        return s;
    }
}
