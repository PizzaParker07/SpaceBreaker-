package com.pizzaparker07.spacebreaker.testmod.showcase;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.testmod.showcase.ShowcaseDriver.Shot;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * QA (beta.1 feedback, "the rocket goes inside the character"): the crew must not show through the capsule. The
 * player rides a Moon rocket in third person while it is put through every flight state, the whole stack and the
 * capsule alone; every time the game is about to draw someone sitting in a rocket is counted, with whether that
 * draw was cancelled. Log lines "[showcase] crew: …"; a line "crew: FAIL …" names a state where the rider was drawn.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
final class QaCrewHiddenScenario implements ShowcaseScenario {
    private static final int Y = ShowcaseBuilder.FLOOR;
    private static RocketEntity rocket;
    private static String label = "";
    private static int attempts, drawn;

    @Override
    public void build(ServerLevel level) {
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                level.setBlock(new BlockPos(x, Y, z), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = Y + 1; y <= Y + 40; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    @Override
    public List<Shot> shots() {
        List<Shot> s = new ArrayList<>();
        CameraType third = CameraType.THIRD_PERSON_BACK;
        s.add(new Shot("00_board", 6000, 0.5, Y + 3, 6.5, 180F, 10F, false, third, 40, p -> {
            ServerLevel level = p.serverLevel();
            rocket = RocketEntity.create(level, RocketryModule.MOON_ROCKET, Vec3.atBottomCenterOf(new BlockPos(0, Y + 1, 0)), 0F);
            rocket.fillCompletely();
            level.addFreshEntity(rocket);
            p.startRiding(rocket, true);
        }, null));
        for (FlightState state : FlightState.values()) {
            s.add(state(state, false));
        }
        // the capsule alone, as it comes home
        for (FlightState state : new FlightState[]{FlightState.REENTRY, FlightState.PARACHUTE, FlightState.IDLE}) {
            s.add(state(state, true));
        }
        s.add(Shot.stay("99_end", 6000, false, third, 5, p -> {
            finishLabel();
            ShowcaseDriver.log("crew: done");
        }, null));
        return s;
    }

    private static Shot state(FlightState state, boolean capsuleOnly) {
        String name = (capsuleOnly ? "capsule_" : "stack_") + state.name().toLowerCase(java.util.Locale.ROOT);
        return Shot.stay(name, 6000, false, CameraType.THIRD_PERSON_BACK, 60, p -> {
            finishLabel();
            if (rocket == null || !rocket.isAlive()) {
                ShowcaseDriver.log("crew: no rocket for " + name);
                return;
            }
            int stage = capsuleOnly ? rocket.layout().stages().size() : 0;
            rocket.stageScene(state, 40, stage, state.powered());
            if (p.getVehicle() != rocket) {
                p.teleportTo(rocket.getX(), rocket.getY() + 1, rocket.getZ());
                p.startRiding(rocket, true);
            }
            label = name;
            attempts = 0;
            drawn = 0;
        }, null);
    }

    private static void finishLabel() {
        if (label.isEmpty()) {
            return;
        }
        ShowcaseDriver.log(String.format("crew: %s%s: the game tried to draw the rider %d times, drawn %d",
                drawn > 0 ? "FAIL " : "", label, attempts, drawn));
        label = "";
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    static void onRender(RenderLivingEvent.Pre<?, ?> event) {
        if (label.isEmpty() || !(event.getEntity().getVehicle() instanceof RocketEntity)) {
            return;
        }
        attempts++;
        if (!event.isCanceled()) {
            drawn++;
        }
    }
}
