package com.pizzaparker07.spacebreaker.content.rocketry.pad;

import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketMath;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.MissionPlanner;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Transfers;
import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class LaunchConsoleBlockEntity extends BlockEntity implements MenuProvider {
    public static final int RADIUS = 16;
    // flag bits
    public static final int PAD = 1, TOWER = 2, ARM = 4, UMBILICAL = 8, ROCKET = 16, CREW = 32, FUELLED = 64,
            TWR_OK = 128, DV_OK = 256, RETURN_OK = 512;

    private LaunchPad.Result pad;
    private long lastRefresh = -1000;
    private final int[] values = new int[LaunchConsoleMenu.DATA_COUNT];

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            update();
            return values[index];
        }

        @Override
        public void set(int index, int value) {
            values[index] = value;
        }

        @Override
        public int getCount() {
            return LaunchConsoleMenu.DATA_COUNT;
        }
    };

    public LaunchConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(RocketryModule.LAUNCH_CONSOLE_BE.get(), pos, state);
    }

    public void refresh() {
        if (level != null) {
            pad = LaunchPad.findNear(level, worldPosition, RADIUS);
            if (pad == null) {
                // report an incomplete pad too, so the checklist can say what is missing
                BlockPos c = nearestTrench();
                pad = c == null ? null : LaunchPad.check(level, c);
            }
            lastRefresh = level.getGameTime();
        }
    }

    @Nullable
    private BlockPos nearestTrench() {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -4; dy <= 2; dy++) {
                    p.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    if (LaunchPad.isTrench(level.getBlockState(p))) {
                        return LaunchPad.centerNear(level, p.immutable());
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public RocketEntity rocket() {
        return pad == null || !pad.formed() ? null : LaunchPad.rocketOn(level, pad.center());
    }

    private void update() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (level.getGameTime() - lastRefresh >= 20) {
            refresh();
        }
        int flags = 0;
        java.util.Arrays.fill(values, 0);
        values[LaunchConsoleMenu.STATE] = -1;
        if (pad != null) {
            if (pad.formed()) flags |= PAD;
            if (pad.hasTower()) flags |= TOWER;
            if (!pad.arms().isEmpty()) flags |= ARM;
            if (!pad.umbilicals().isEmpty()) flags |= UMBILICAL;
            values[LaunchConsoleMenu.TOWER_HEIGHT] = pad.towerHeight();
            values[LaunchConsoleMenu.MISSING] = pad.missingPad().size() + pad.missingTrench().size();
        } else {
            values[LaunchConsoleMenu.MISSING] = 25;
        }
        RocketEntity r = rocket();
        if (r != null) {
            flags |= ROCKET;
            long crew = r.getPassengers().stream().filter(p -> p instanceof Player).count();
            if (crew > 0) flags |= CREW;
            if (r.fullyFuelled()) flags |= FUELLED;
            double g = Environment.gravity(level);
            double twr = RocketMath.twr(r.layout(), r.activeStage(), r.fuel(), g);
            if (twr >= 1.05) flags |= TWR_OK;
            double dv = r.deltaV();
            double cost = Transfers.cost(Location.EARTH_SURFACE, Location.EARTH_ORBIT, r.layout(), r.activeStage(), SbConfig.realism()).orElse(0);
            if (dv >= cost) flags |= DV_OK;
            if (MissionPlanner.canReturnAfter(r.layout(), r.activeStage(), r.fuel(), Location.EARTH_SURFACE, Location.EARTH_ORBIT, SbConfig.realism())) flags |= RETURN_OK;
            values[LaunchConsoleMenu.FUEL] = (int) Math.round(r.fuelFraction() * 1000);
            values[LaunchConsoleMenu.DV] = (int) Math.round(dv);
            values[LaunchConsoleMenu.COST] = (int) Math.round(cost);
            values[LaunchConsoleMenu.TWR] = (int) Math.round(twr * 100);
            values[LaunchConsoleMenu.STATE] = r.state().ordinal();
            values[LaunchConsoleMenu.TIMER] = r.timer();
            values[LaunchConsoleMenu.CREW] = (int) crew;
            values[LaunchConsoleMenu.ROCKET_ID] = r.getId();
        }
        values[LaunchConsoleMenu.FLAGS] = flags;
    }

    /** Button presses from the console screen (server side). */
    public void press(ServerPlayer player, int button) {
        refresh();
        RocketEntity r = rocket();
        if (r == null) {
            player.displayClientMessage(Component.translatable("message.spacebreaker.console.no_rocket"), true);
            return;
        }
        if (button == LaunchConsoleMenu.BUTTON_LAUNCH) {
            r.tryStartCountdown(player);
        } else if (button == LaunchConsoleMenu.BUTTON_ABORT) {
            r.abortCountdown(player);
        } else if (button == LaunchConsoleMenu.BUTTON_REPORT && pad != null) {
            PadBlock.report(player, pad);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.spacebreaker.launch_console");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new LaunchConsoleMenu(id, inventory, this, data);
    }

    public boolean isRocketIdle() {
        RocketEntity r = rocket();
        return r != null && r.state() == FlightState.IDLE;
    }
}
