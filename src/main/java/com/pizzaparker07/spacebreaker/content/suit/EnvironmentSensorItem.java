package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.content.suit.net.SensorReadingPayload;
import com.pizzaparker07.spacebreaker.core.body.Bodies;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Environment Sensor: use it to take a reading of where you stand — pressure and what the air is made of,
 * temperature (and whether you are in sunlight), radiation dose rate, gravity and the ground underfoot. The
 * reading shows as a card on screen for ten seconds (client.hud.SensorCard). Planet data comes from the
 * celestial body files, local values from {@link Environment}.
 */
public class EnvironmentSensorItem extends Item {
    /** Optional dust deposition rate for solar panels, filled in by the module that models dust (Mars). */
    public static java.util.function.BiFunction<Level, BlockPos, Double> dustRate = (level, pos) -> -1.0;

    public EnvironmentSensorItem(Properties properties) {
        super(properties);
    }

    /** Takes a reading at {@code pos}. */
    public static SensorReadingPayload read(Level level, BlockPos pos) {
        var body = Bodies.forLevel(level);
        String bodyKey = body.isPresent() ? level.registryAccess().registry(Bodies.REGISTRY)
                .map(r -> r.getKey(body.get())).map(id -> "body." + id.getNamespace() + "." + id.getPath()).orElse("") : "";
        double pressure = Environment.pressureKpa(level, pos);
        Map<String, Double> comp = new LinkedHashMap<>();
        if (pressure > 0.01) {
            body.ifPresent(b -> b.atmosphere().composition().entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())).limit(5)
                    .forEach(e -> comp.put(e.getKey(), e.getValue())));
        }
        boolean sunlit = Environment.sunElevation(level) > 0 && level.canSeeSky(pos.above());
        BlockState ground = level.getBlockState(pos.below());
        return new SensorReadingPayload(bodyKey, (float) pressure, (float) Environment.temperatureC(level, pos), sunlit,
                (float) Environment.radiation(level, pos), (float) Environment.gravityFactor(level), comp,
                ground.getBlock().getDescriptionId(), dustRate.apply(level, pos).floatValue());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            // sneak + use: choose what the helmet's way-home arrow points at
            if (!level.isClientSide) {
                SurvivalData data = LifeSupport.data(player);
                data.navKind = data.navKind.next();
                player.displayClientMessage(Component.translatable("message.spacebreaker.nav.target",
                        Component.translatable(data.navKind.key())).withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            com.pizzaparker07.spacebreaker.content.suit.net.SuitNetwork.send(sp, read(level, player.blockPosition()));
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.4F, 1.8F);
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spacebreaker.environment_sensor").withStyle(ChatFormatting.GRAY));
    }
}
