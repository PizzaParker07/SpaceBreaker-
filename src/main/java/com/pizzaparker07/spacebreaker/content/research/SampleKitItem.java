package com.pizzaparker07.spacebreaker.content.research;

import com.pizzaparker07.spacebreaker.core.body.Bodies;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.Tags;
import com.pizzaparker07.spacebreaker.core.data.SbTagKeys;

import java.util.List;
import java.util.Optional;

/**
 * Geologist's kit: use it on natural ground to bag a field sample of the world you stand on.
 * Each world gives samples of its own kind (the celestial body id), which reveal different technologies.
 */
public class SampleKitItem extends Item {
    public static final int COOLDOWN = 30;

    public SampleKitItem(Properties properties) {
        super(properties);
    }

    /** The sample kind for a level: the id of its celestial body (Earth for unknown dimensions). */
    public static ResourceLocation kindFor(Level level) {
        Optional<ResourceLocation> id = Bodies.registry(level.registryAccess())
                .flatMap(reg -> Bodies.forLevel(level).map(reg::getKey));
        return id.orElse(ResearchTree.EARTH);
    }

    /** Blocks a kit can sample, besides natural soil and stone (other areas add their rocks here). */
    public static final TagKey<Block> SAMPLEABLE = SbTagKeys.modBlock("sampleable");

    /** Natural ground: soil, sand, gravel and stone (not wood, glass, metal, machines or plants). */
    public static boolean isSampleable(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()) {
            return false;
        }
        if (state.is(SAMPLEABLE) || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER) || state.is(Tags.Blocks.GRAVELS) || state.is(Tags.Blocks.ORES)
                || state.is(BlockTags.TERRACOTTA) || state.is(Tags.Blocks.STONES)) {
            return true;
        }
        SoundType sound = state.getSoundType();
        return state.is(BlockTags.MINEABLE_WITH_SHOVEL) && (sound == SoundType.SAND || sound == SoundType.GRAVEL || sound == SoundType.SNOW);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockState state = level.getBlockState(ctx.getClickedPos());
        if (player == null || !isSampleable(state)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            ResourceLocation kind = kindFor(server);
            ItemStack sample = Research.sample(kind);
            if (!player.getInventory().add(sample)) {
                player.drop(sample, false);
            }
            ctx.getItemInHand().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            player.getCooldowns().addCooldown(this, COOLDOWN);
            server.playSound(null, ctx.getClickedPos(), SoundEvents.BRUSH_GRAVEL_COMPLETED, SoundSource.PLAYERS, 1F, 1.1F);
            server.levelEvent(2001, ctx.getClickedPos(), Block.getId(state));
            player.displayClientMessage(Component.translatable("message.spacebreaker.sample_taken",
                    Component.translatable(SampleItem.kindKey(kind))), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.sample_kit").withStyle(ChatFormatting.GRAY));
    }
}
