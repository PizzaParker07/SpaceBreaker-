package com.pizzaparker07.spacebreaker.client.suit;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.logging.LogUtils;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.client.hud.ClientSuitState;
import com.pizzaparker07.spacebreaker.client.hud.HudConfig;
import com.pizzaparker07.spacebreaker.content.suit.SoundMuffling;
import com.pizzaparker07.spacebreaker.content.suit.net.SuitStatusPayload;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Realistic sound ({@link SoundMuffling}): each world sound is judged once, when it starts, from where the listener
 * stands; its channel then gets a low-pass filter (OpenAL EFX) that lowers the volume and the high tones. Without
 * EFX it falls back to a lower volume and a slightly lower pitch. Sounds already playing keep the filter they started
 * with; nothing is scanned per tick.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class SuitAcoustics {
    private static final Logger LOG = LogUtils.getLogger();
    /** Within this distance of the feet a sound comes from the wearer or what they stand on or hold. */
    private static final double CONTACT_RANGE = 1.6;
    /** Muffles decided on the client thread, applied on the sound thread when the channel starts. */
    private static final Map<SoundInstance, SoundMuffling.Muffle> PENDING = Collections.synchronizedMap(new WeakHashMap<>());
    private static Field sourceField;
    /** 0 = not tried yet, -1 = no EFX on this device, otherwise the filter object. */
    private static int filter;

    private SuitAcoustics() {
    }

    @SubscribeEvent
    static void onPlay(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null || !HudConfig.realisticSound()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        SoundMuffling.Muffle m = classify(mc, mc.player, sound);
        if (!m.none()) {
            PENDING.put(sound, m);
        }
    }

    /** Inside the helmet, in the air or on what the suit touches: the muffle for one sound, decided once. */
    static SoundMuffling.Muffle classify(Minecraft mc, Player player, SoundInstance sound) {
        // the radio, mission control, alarms, music and menus play inside the helmet or the head
        if (sound.isRelative() || sound.getAttenuation() == SoundInstance.Attenuation.NONE
                || sound.getSource() == SoundSource.MUSIC || sound.getSource() == SoundSource.MASTER) {
            return SoundMuffling.Muffle.NONE;
        }
        double kPa;
        boolean air;
        SuitStatusPayload s = ClientSuitState.get(mc.level.getGameTime());
        if (s != null) {
            kPa = s.pressureKpa();
            air = s.has(SuitStatusPayload.BREATHABLE) || s.has(SuitStatusPayload.PRESSURISED);
        } else {
            BlockPos eye = BlockPos.containing(player.getEyePosition());
            kPa = Environment.pressureKpa(mc.level, eye);
            air = Environment.canBreathe(mc.level, eye);
        }
        if (air || kPa >= SoundMuffling.FULL_AIR_KPA) {
            return SoundMuffling.Muffle.NONE;
        }
        Vec3 at = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        boolean contact = at.distanceTo(player.position()) <= CONTACT_RANGE
                || mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && (player.swinging || mc.gameMode != null && mc.gameMode.isDestroying())
                && at.distanceTo(hit.getBlockPos().getCenter()) <= 1.0;
        return contact ? SoundMuffling.contact(kPa, false) : SoundMuffling.outside(kPa, false);
    }

    @SubscribeEvent
    static void onSource(PlaySoundSourceEvent event) {
        apply(event.getSound(), event.getChannel());
    }

    @SubscribeEvent
    static void onStream(PlayStreamingSourceEvent event) {
        apply(event.getSound(), event.getChannel());
    }

    /** Sound thread: puts the muffle on the channel that just started. */
    private static void apply(SoundInstance sound, Channel channel) {
        SoundMuffling.Muffle m = PENDING.remove(sound);
        if (m == null) {
            return;
        }
        int source = source(channel);
        if (source == 0) {
            return;
        }
        int f = filter();
        if (f > 0) {
            EXTEfx.alFilterf(f, EXTEfx.AL_LOWPASS_GAIN, m.gain());
            EXTEfx.alFilterf(f, EXTEfx.AL_LOWPASS_GAINHF, m.highGain());
            // the filter's settings are copied onto the source here, so one filter object serves every sound
            AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, f);
        } else {
            AL10.alSourcef(source, AL10.AL_GAIN, AL10.alGetSourcef(source, AL10.AL_GAIN) * m.gain());
            AL10.alSourcef(source, AL10.AL_PITCH, AL10.alGetSourcef(source, AL10.AL_PITCH) * (0.9F + 0.1F * m.highGain()));
        }
    }

    private static int source(Channel channel) {
        try {
            if (sourceField == null) {
                sourceField = Channel.class.getDeclaredField("source");
                sourceField.setAccessible(true);
            }
            return sourceField.getInt(channel);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }

    /** The shared low-pass filter, made on the sound thread the first time; -1 when the device has no EFX. */
    private static int filter() {
        if (filter == 0) {
            filter = -1;
            try {
                long device = ALC10.alcGetContextsDevice(ALC10.alcGetCurrentContext());
                if (ALC10.alcIsExtensionPresent(device, "ALC_EXT_EFX")) {
                    int f = EXTEfx.alGenFilters();
                    EXTEfx.alFilteri(f, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
                    if (AL10.alGetError() == AL10.AL_NO_ERROR && f > 0) {
                        filter = f;
                        LOG.info("Realistic sound: OpenAL EFX low-pass filter ready");
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                LOG.info("Realistic sound: no OpenAL EFX filter, using volume only ({})", e.toString());
            }
        }
        return filter;
    }
}
