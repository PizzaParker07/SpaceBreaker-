package com.pizzaparker07.spacebreaker.content.venus;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Aerostat Platform Kit: a packed lift envelope, a folding composite deck and its rigging. Used inside the habitable
 * band of the Venusian clouds (about 1 bar and room temperature) it inflates into a small floating base: a 9×9 deck with
 * railings and a lantern, hanging from a lift-gas envelope. It only fills empty air and needs open sky to inflate.
 * Elsewhere it refuses: below the clouds the envelope would cook, above them it would not hold you up.
 */
public class AerostatKitItem extends Item {
    public static final int HALF = 4;

    public AerostatKitItem(Properties props) {
        super(props);
    }

    /** Whether a platform can be deployed with its deck at height y in this level. */
    public static boolean inLiftBand(Level level, double y) {
        return level.dimension() == VenusModule.VENUS && y >= VenusClimate.CLOUD_BASE && y <= VenusClimate.CLOUD_TOP + 30;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.spacebreaker.aerostat_kit.desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        BlockPos deck = player.blockPosition().below();
        if (!inLiftBand(level, deck.getY())) {
            player.displayClientMessage(Component.translatable("message.spacebreaker.aerostat_kit.band"), true);
            return InteractionResultHolder.fail(stack);
        }
        int placed = deploy(level, deck);
        if (placed == 0) {
            player.displayClientMessage(Component.translatable("message.spacebreaker.aerostat_kit.blocked"), true);
            return InteractionResultHolder.fail(stack);
        }
        level.playSound(null, deck, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1F, 0.7F);
        player.resetFallDistance();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    private static int put(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, state, Block.UPDATE_ALL);
            return 1;
        }
        return 0;
    }

    /**
     * Builds the platform with its deck centred at {@code deck}; returns the number of blocks placed. A 9x9 composite deck
     * with a safety railing (a gate on two sides) and mooring cleats at the corners, rigging up to a load ring, and
     * above it a round envelope in the same two-tone gores as the research aerostats, with a grey equator ring and cap.
     */
    public static int deploy(Level level, BlockPos deck) {
        int n = 0;
        BlockState panel = VenusModule.COMPOSITE_PANEL.get().defaultBlockState();
        BlockState fabric = VenusModule.ENVELOPE_FABRIC.get().defaultBlockState();
        BlockState gold = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        BlockState ring = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState rail = com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.RAILING.get().defaultBlockState();
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                n += put(level, deck.offset(dx, 0, dz), panel);
            }
        }
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                boolean edge = Math.abs(dx) == HALF || Math.abs(dz) == HALF;
                boolean corner = Math.abs(dx) == HALF && Math.abs(dz) == HALF;
                boolean gate = dx == 0 && Math.abs(dz) == HALF;
                if (corner) {
                    n += put(level, deck.offset(dx, 1, dz), ring); // mooring cleat
                } else if (edge && !gate) {
                    n += put(level, deck.offset(dx, 1, dz), rail);
                }
            }
        }
        // rigging from the deck corners in to a load ring under the envelope
        for (int[] c : new int[][]{{-3, -3}, {-3, 3}, {3, -3}, {3, 3}}) {
            for (int dy = 1; dy <= 6; dy++) {
                n += put(level, deck.offset(c[0], dy, c[1]), Blocks.CHAIN.defaultBlockState());
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
                    n += put(level, deck.offset(dx, 7, dz), Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState());
                }
            }
        }
        // the envelope: a round shell in gores, with a grey ring at its equator and a cap on top
        int cy = 12;
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                for (int dy = 8; dy <= 16; dy++) {
                    double e = (dx * dx + dz * dz) / 40.0 + (dy - cy) * (dy - cy) / 18.0;
                    double inner = (dx * dx + dz * dz) / 26.0 + (dy - cy) * (dy - cy) / 9.5;
                    if (e > 1 || inner <= 1) {
                        continue;
                    }
                    int gore = (int) Math.floor((Math.atan2(dz, dx) + Math.PI) / (Math.PI / 4));
                    BlockState st = dy == cy ? ring : dy >= 16 ? ring : gore % 2 == 0 ? fabric : gold;
                    n += put(level, deck.offset(dx, dy, dz), st);
                }
            }
        }
        n += put(level, deck.offset(0, 7, 0), com.pizzaparker07.spacebreaker.content.rocketry.base.BaseBlocks.LED_PANEL.get().defaultBlockState());
        return n;
    }
}
