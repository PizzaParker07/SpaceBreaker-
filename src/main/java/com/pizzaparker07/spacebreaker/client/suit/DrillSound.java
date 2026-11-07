package com.pizzaparker07.spacebreaker.client.suit;

import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.audio.SbSounds;
import com.pizzaparker07.spacebreaker.content.suit.ElectricDrillItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The drill's motor: a looping sound that follows the local player while they dig with a charged Electric Drill,
 * fading in and out instead of cutting.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class DrillSound extends AbstractTickableSoundInstance {
    private static DrillSound playing;
    private final LocalPlayer player;
    private float target = 1F;

    private DrillSound(LocalPlayer player) {
        super(SbSounds.DRILL_LOOP.get(), SoundSource.PLAYERS, RandomSource.create());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.05F;
        this.pitch = 1.0F;
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
    }

    static boolean drilling(Minecraft mc) {
        return mc.player != null && mc.gameMode != null && mc.gameMode.isDestroying()
                && mc.player.getMainHandItem().getItem() instanceof ElectricDrillItem && ElectricDrillItem.charged(mc.player.getMainHandItem());
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        target = drilling(mc) ? 0.7F : 0F;
        volume += (target - volume) * 0.3F;
        pitch = 0.95F + 0.1F * volume;
        x = player.getX();
        y = player.getY() + 1;
        z = player.getZ();
        if (player.isRemoved() || (target == 0F && volume < 0.02F)) {
            stop();
            playing = null;
        }
    }

    @SubscribeEvent
    static void clientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if ((playing == null || playing.isStopped()) && drilling(mc)) {
            playing = new DrillSound(mc.player);
            mc.getSoundManager().play(playing);
        }
    }
}
