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
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Realistic sound ({@link SoundMuffling}): each world sound is judged once, when it starts, from where the listener
 * stands; its channel then gets a low-pass filter (OpenAL EFX) that lowers the volume and the high tones. Without
 * EFX it falls back to a lower volume and a slightly lower pitch. When the air around changes (into a pressurised room,
 * out through the airlock) the sounds already playing are re-filtered once; nothing in the world is scanned.
 */
@EventBusSubscriber(modid = Spacebreaker.MOD_ID, value = Dist.CLIENT)
public final class SuitAcoustics {
    private static final Logger LOG = LogUtils.getLogger();
    /** Within this distance of the feet a sound comes from the wearer or what they stand on or hold. */
    private static final double CONTACT_RANGE = 1.6;
    /** World sounds judged on the client thread, waiting for their channel to start on the sound thread. */
    private static final Map<SoundInstance, Pending> PENDING = Collections.synchronizedMap(new WeakHashMap<>());
    /** World sounds playing now, with their OpenAL source, so a change of air can re-filter them once. */
    private static final Map<SoundInstance, Playing> PLAYING = Collections.synchronizedMap(new WeakHashMap<>());
    /** 0 = not tried yet, -1 = no EFX on this device, otherwise the filter object. */
    private static int filter;
    /** The outside muffle where the listener stood last tick; a change re-filters the playing sounds. */
    private static SoundMuffling.Muffle lastAir = SoundMuffling.Muffle.NONE;

    private record Pending(boolean contact, SoundMuffling.Muffle muffle) {
    }

    private record Playing(int source, boolean contact) {
    }

    private SuitAcoustics() {
    }

    @SubscribeEvent
    static void onPlay(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null || !HudConfig.realisticSound()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !worldSound(sound)) {
            return;
        }
        boolean contact = contact(mc, mc.player, sound);
        PENDING.put(sound, new Pending(contact, muffle(mc, mc.player, contact)));
    }

    /** The radio, mission control, alarms, music and menus play inside the helmet or the head: never muffled. */
    static boolean worldSound(SoundInstance sound) {
        return !sound.isRelative() && sound.getAttenuation() != SoundInstance.Attenuation.NONE
                && sound.getSource() != SoundSource.MUSIC && sound.getSource() != SoundSource.MASTER;
    }

    /** From the wearer, what they stand on or hold, or the block they are working on. */
    static boolean contact(Minecraft mc, Player player, SoundInstance sound) {
        Vec3 at = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        return at.distanceTo(player.position()) <= CONTACT_RANGE
                || mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && (player.swinging || mc.gameMode != null && mc.gameMode.isDestroying())
                && at.distanceTo(hit.getBlockPos().getCenter()) <= 1.0;
    }

    /** The muffle for a sound heard where the listener stands now. */
    static SoundMuffling.Muffle muffle(Minecraft mc, Player player, boolean contact) {
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
        return contact ? SoundMuffling.contact(kPa, air) : SoundMuffling.outside(kPa, air);
    }

    /**
     * Once per tick a cheap look at the air around (the last suit status): when it changes class (into a pressurised
     * room, out of the airlock), the sounds already playing get the new filter once. No world scanning.
     */
    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || filter <= 0 || !HudConfig.realisticSound()) {
            return;
        }
        SoundMuffling.Muffle air = rounded(muffle(mc, mc.player, false));
        if (air.equals(lastAir)) {
            return;
        }
        lastAir = air;
        List<Map.Entry<SoundInstance, Playing>> playing;
        synchronized (PLAYING) {
            playing = new ArrayList<>(PLAYING.entrySet());
        }
        for (var e : playing) {
            if (!mc.getSoundManager().isActive(e.getKey())) {
                PLAYING.remove(e.getKey());
            } else {
                setFilter(e.getValue().source(), muffle(mc, mc.player, e.getValue().contact()));
            }
        }
    }

    /** Rounded to 5 %, so small pressure changes (dust, weather) do not count as a change of air. */
    private static SoundMuffling.Muffle rounded(SoundMuffling.Muffle m) {
        return new SoundMuffling.Muffle(Math.round(m.gain() * 20) / 20F, Math.round(m.highGain() * 20) / 20F);
    }

    @SubscribeEvent
    static void onSource(PlaySoundSourceEvent event) {
        apply(event.getSound(), event.getChannel());
    }

    @SubscribeEvent
    static void onStream(PlayStreamingSourceEvent event) {
        apply(event.getSound(), event.getChannel());
    }

    /** Sound thread: puts the muffle on the channel that just started and remembers its source. */
    private static void apply(SoundInstance sound, Channel channel) {
        Pending p = PENDING.remove(sound);
        if (p == null) {
            return;
        }
        int source = source(channel);
        if (source == 0) {
            return;
        }
        PLAYING.put(sound, new Playing(source, p.contact()));
        if (p.muffle().none()) {
            return;
        }
        if (filter() > 0) {
            setFilter(source, p.muffle());
        } else {
            AL10.alSourcef(source, AL10.AL_GAIN, AL10.alGetSourcef(source, AL10.AL_GAIN) * p.muffle().gain());
            AL10.alSourcef(source, AL10.AL_PITCH, AL10.alGetSourcef(source, AL10.AL_PITCH) * (0.9F + 0.1F * p.muffle().highGain()));
        }
    }

    /** Sets (or, for no muffle, removes) the low-pass filter on one source. A source that is gone is just skipped. */
    private static void setFilter(int source, SoundMuffling.Muffle m) {
        int f = filter;
        if (f <= 0) {
            return;
        }
        if (m.none()) {
            AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
        } else {
            EXTEfx.alFilterf(f, EXTEfx.AL_LOWPASS_GAIN, m.gain());
            EXTEfx.alFilterf(f, EXTEfx.AL_LOWPASS_GAINHF, m.highGain());
            // the filter's settings are copied onto the source here, so one filter object serves every sound
            AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, f);
        }
        AL10.alGetError();
    }

    /** The channel's OpenAL source (opened by the access transformer); 0 if it is not reachable, then no filter. */
    private static int source(Channel channel) {
        try {
            return channel.source;
        } catch (LinkageError e) {
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
