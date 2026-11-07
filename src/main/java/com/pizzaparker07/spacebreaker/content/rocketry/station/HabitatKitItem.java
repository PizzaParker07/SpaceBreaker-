package com.pizzaparker07.spacebreaker.content.rocketry.station;

import com.pizzaparker07.spacebreaker.content.machines.MachinesModule;
import com.pizzaparker07.spacebreaker.content.rocketry.RocketryModule;
import com.pizzaparker07.spacebreaker.content.suit.SuitModule;
import com.pizzaparker07.spacebreaker.content.suit.block.AirlockDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.List;

/**
 * Habitat module kit: unfolds a sealed 7 × 5 × 7 room of station hull in front of the player, with a band of
 * windows, an airlock door facing the player, a docking port on the far wall, an oxygen distributor and a
 * bunk inside, and a solar array over a battery bank in the roof. Slot an oxygen tank into the distributor (or pipe
 * oxygen in) and the room fills with air.
 */
public class HabitatKitItem extends Item {
    /** Outer size: 7 wide, 5 high, 7 deep. */
    public static final int HALF = 3, HEIGHT = 5;

    public HabitatKitItem(Properties properties) {
        super(properties);
    }

    /** World position of a block given as (right, up, forward) from the floor centre. */
    private static BlockPos at(BlockPos centre, Direction forward, int right, int up, int fwd) {
        return centre.relative(forward.getClockWise(), right).above(up).relative(forward, fwd);
    }

    /** Where the oxygen distributor ends up (floor centre, one up). */
    public static BlockPos distributor(BlockPos centre) {
        return centre.above();
    }

    /** Where the bunk ends up. */
    public static BlockPos bunk(BlockPos centre, Direction forward) {
        return at(centre, forward, -2, 1, 2);
    }

    /** Where the battery bank ends up: in the middle of the roof, under the solar array. */
    public static BlockPos battery(BlockPos centre) {
        return centre.above(HEIGHT - 1);
    }

    /** Where the docking port ends up (middle of the far wall). */
    public static BlockPos port(BlockPos centre, Direction forward) {
        return at(centre, forward, 0, 2, HALF);
    }

    /**
     * Builds the module with its floor centred on {@code centre}, the door wall nearest the player
     * ({@code forward} points from the player into the module). False, and nothing placed, if anything is in the way.
     */
    public static boolean unfold(Level level, BlockPos centre, Direction forward) {
        for (int r = -HALF; r <= HALF; r++) {
            for (int u = 0; u <= HEIGHT; u++) {
                for (int f = -HALF; f <= HALF; f++) {
                    if (!level.getBlockState(at(centre, forward, r, u, f)).canBeReplaced()) {
                        return false;
                    }
                }
            }
        }
        BlockState hull = RocketryModule.STATION_HULL.get().defaultBlockState();
        BlockState window = RocketryModule.STATION_WINDOW.get().defaultBlockState();
        for (int r = -HALF; r <= HALF; r++) {
            for (int u = 0; u < HEIGHT; u++) {
                for (int f = -HALF; f <= HALF; f++) {
                    boolean shellR = Math.abs(r) == HALF, shellF = Math.abs(f) == HALF, shellU = u == 0 || u == HEIGHT - 1;
                    if (!(shellR || shellF || shellU)) {
                        continue;
                    }
                    boolean corner = shellR && shellF;
                    boolean windowBand = u == 2 && !shellU && !corner && Math.abs(shellR ? f : r) <= 1;
                    level.setBlock(at(centre, forward, r, u, f), windowBand ? window : hull, 3);
                }
            }
        }
        // airlock in the near wall, facing out towards the player
        BlockState door = SuitModule.AIRLOCK_DOOR.get().defaultBlockState().setValue(AirlockDoorBlock.FACING, forward.getOpposite());
        level.setBlock(at(centre, forward, 0, 1, -HALF), door.setValue(AirlockDoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
        level.setBlock(at(centre, forward, 0, 2, -HALF), door.setValue(AirlockDoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
        level.setBlock(port(centre, forward), RocketryModule.DOCKING_PORT.get().defaultBlockState()
                .setValue(DockingPortBlock.FACING, forward), 3);
        level.setBlock(distributor(centre), SuitModule.OXYGEN_DISTRIBUTOR.get().defaultBlockState(), 3);
        level.setBlock(bunk(centre, forward), RocketryModule.STATION_BUNK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, forward.getClockWise()), 3);
        // one solar array on the roof (its wings fold out north and south, over open roof), charging a battery
        // bank set into the roof below it, so the station keeps its power through the night side of the orbit
        level.setBlock(battery(centre), MachinesModule.BATTERY_BANK.block().get().defaultBlockState()
                .setValue(com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock.FACING, forward), 3);
        level.setBlock(centre.above(HEIGHT), MachinesModule.SOLAR_ARRAY.get().defaultBlockState(), 3);
        // a cable end hanging from the ceiling under the battery: machines in the room plug in there
        BlockPos cable = battery(centre).below();
        level.setBlock(cable, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(
                MachinesModule.COPPER_CABLE.get().defaultBlockState(), level, cable), 3);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Direction facing = player.getDirection();
        BlockPos centre = BlockPos.containing(player.getEyePosition()).relative(facing, HALF + 3).below(2);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!unfold(level, centre, facing)) {
            player.displayClientMessage(Component.translatable("message.spacebreaker.station_kit.blocked"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            DockingPorts.get(server).setOwner(port(centre, facing), player.getGameProfile().getName());
        }
        level.playSound(null, centre, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1F, 0.7F);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.habitat_kit").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.spacebreaker.habitat_kit.air").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
