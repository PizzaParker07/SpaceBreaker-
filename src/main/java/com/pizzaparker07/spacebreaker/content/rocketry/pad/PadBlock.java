package com.pizzaparker07.spacebreaker.content.rocketry.pad;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Launch pad plating and flame trench grate. Using one with an empty hand checks the facility:
 * a checklist in chat and particles on every missing block.
 */
public class PadBlock extends Block {
    public PadBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos center = LaunchPad.centerNear(level, pos);
        if (center == null) {
            player.displayClientMessage(Component.translatable("message.spacebreaker.pad.no_trench").withStyle(ChatFormatting.GOLD), false);
            return InteractionResult.CONSUME;
        }
        report((ServerPlayer) player, LaunchPad.check(level, center));
        return InteractionResult.CONSUME;
    }

    public static void report(ServerPlayer player, LaunchPad.Result r) {
        player.displayClientMessage(Component.translatable("message.spacebreaker.pad.title").withStyle(ChatFormatting.AQUA), false);
        line(player, r.missingTrench().isEmpty(), Component.translatable("message.spacebreaker.pad.trench", 9 - r.missingTrench().size()));
        line(player, r.missingPad().isEmpty(), Component.translatable("message.spacebreaker.pad.plating", 16 - r.missingPad().size()));
        line(player, r.hasTower(), Component.translatable("message.spacebreaker.pad.tower", r.towerHeight(), LaunchPad.MIN_TOWER));
        line(player, !r.arms().isEmpty(), Component.translatable("message.spacebreaker.pad.arm"));
        line(player, !r.umbilicals().isEmpty(), Component.translatable("message.spacebreaker.pad.umbilical"));
        player.displayClientMessage(Component.translatable(r.formed() ? "message.spacebreaker.pad.ready" : "message.spacebreaker.pad.incomplete")
                .withStyle(r.formed() ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        ServerLevel level = player.serverLevel();
        for (BlockPos p : r.missingPad()) {
            mark(level, player, p, ParticleTypes.FLAME);
        }
        for (BlockPos p : r.missingTrench()) {
            mark(level, player, p, ParticleTypes.FLAME);
        }
        if (r.formed()) {
            mark(level, player, r.center(), ParticleTypes.HAPPY_VILLAGER);
        }
    }

    private static void line(ServerPlayer player, boolean ok, Component text) {
        player.displayClientMessage(Component.literal(ok ? " ✔ " : " ✖ ").withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED)
                .append(text.copy().withStyle(ChatFormatting.GRAY)), false);
    }

    private static void mark(ServerLevel level, ServerPlayer player, BlockPos p, ParticleOptions particle) {
        level.sendParticles(player, particle, true, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 8, 0.25, 0.1, 0.25, 0.0);
    }
}
