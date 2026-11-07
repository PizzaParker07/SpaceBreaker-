package com.pizzaparker07.spacebreaker.client.machines;

import com.pizzaparker07.spacebreaker.content.machines.base.MachineBlock;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Loop for a working machine (electrolyser hum, crusher grind). Started by the machine's client ticker while it is
 * lit; fades out when it stops, is broken or the player walks away. In vacuum it is only a faint vibration felt
 * through the floor.
 */
public class MachineLoopSound extends AbstractTickableSoundInstance {
    private static final Map<BlockPos, MachineLoopSound> PLAYING = new HashMap<>();
    private final Level level;
    private final BlockPos pos;
    private final net.minecraft.world.level.block.Block block;
    private boolean fading;

    private MachineLoopSound(Level level, BlockPos pos, MachineBlock block) {
        super(block.loopSound(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.level = level;
        this.pos = pos.immutable();
        this.block = block;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
        this.pitch = block.loopPitch();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
    }

    /** Hook for {@link MachineBlock#CLIENT_LOOP}. */
    public static void start(Level level, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.blockPosition().distSqr(pos) > 24 * 24) {
            return;
        }
        MachineLoopSound playing = PLAYING.get(pos);
        if (playing != null && !playing.isStopped() && playing.level == level) {
            return;
        }
        if (level.getBlockState(pos).getBlock() instanceof MachineBlock mb && mb.loopSound() != null) {
            MachineLoopSound s = new MachineLoopSound(level, pos, mb);
            PLAYING.put(s.pos, s);
            mc.getSoundManager().play(s);
        }
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        BlockState state = level.getBlockState(pos);
        boolean on = mc.level == level && state.is(block) && state.getValue(MachineBlock.LIT);
        float target = on ? (Environment.isVacuum(level) ? 0.25F : 0.8F) : 0F;
        volume = Mth.lerp(0.15F, volume, target);
        if (!on && volume < 0.02F) {
            PLAYING.remove(pos, this);
            stop();
        }
    }
}
