package com.pizzaparker07.spacebreaker.content.uranus.entity;

import com.pizzaparker07.spacebreaker.content.uranus.UranusModule;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * Frost Moth: a pale, broad-winged flutterer of Ariel's rift valleys. It rests clinging under the overhangs of the valley
 * walls and flutters out in the dark, scattering a little CO2 frost from its wings. Harmless, never bred: it lives on
 * the frost and the shade of the chasms.
 */
public class FrostMoth extends Bat {
    public FrostMoth(EntityType<? extends Bat> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Bat.createAttributes();
    }

    public static boolean checkSpawn(EntityType<? extends Bat> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return reason != MobSpawnType.NATURAL || level.getBlockState(pos.below()).is(UranusModule.MIRANDAN_GROUND)
                || level.getBlockState(pos.above()).is(UranusModule.MIRANDAN_GROUND);
    }
}
