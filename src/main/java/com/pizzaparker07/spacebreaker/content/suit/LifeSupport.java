package com.pizzaparker07.spacebreaker.content.suit;

import com.pizzaparker07.spacebreaker.core.config.SbConfig;
import com.pizzaparker07.spacebreaker.core.env.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Server-side survival simulation for players: breathing (suit oxygen, held breath, vacuum exposure),
 * body temperature drifting slowly toward the environment and radiation dose. Pure rules live in
 * static methods so GameTests can drive them with any environment.
 */
public final class LifeSupport {
    /** Breath regained per tick in breathable air. */
    static final int BREATH_RECOVERY = 5;
    /** Body temperature time constant: ~63 % of the way to the target in two minutes. */
    static final double TEMP_RATE = 1.0 / (20 * 120);
    /** Inside a working suit the cabin is kept at this temperature. */
    static final double SUIT_CABIN_C = 21.0;
    /** Natural recovery of radiation dose per in-game day, mSv (keeps long Moon stays playable). */
    static final double DOSE_RECOVERY_PER_DAY = 2.0;

    private LifeSupport() {
    }

    /** What the environment is like at a player's head. */
    public record Env(boolean breathable, double ambientC, double doseRateMsvDay, double pressureKpa) {
        public static Env at(Player player, double cachedDoseRate) {
            var cabin = PressurizedCabin.holding(player);
            if (cabin.isPresent()) {
                // a shirt-sleeve cabin: its own air, temperature and shielding
                return new Env(true, cabin.get().cabinTemperatureC(player), cachedDoseRate * cabin.get().cabinRadiationFactor(player), 101.325);
            }
            Level level = player.level();
            BlockPos eyes = BlockPos.containing(player.getEyePosition());
            return new Env(Environment.canBreathe(level, eyes), Environment.temperatureC(level, eyes), cachedDoseRate,
                    Environment.pressureKpa(level, eyes));
        }
    }

    public static SurvivalData data(Player player) {
        return player.getData(SuitModule.SURVIVAL.get());
    }

    public static boolean hazards() {
        return !SbConfig.SERVER_SPEC.isLoaded() || SbConfig.SURVIVAL_HAZARDS.get();
    }

    /** One server tick of the whole simulation. */
    public static void tick(Player player, Env env) {
        SurvivalData data = data(player);
        data.breathable = env.breathable();
        data.ambientC = env.ambientC();
        data.pressureKpa = env.pressureKpa();
        data.doseRateMsvDay = env.doseRateMsvDay() * radiationFactor(player);
        boolean immune = player.isCreative() || player.isSpectator();
        boolean hazards = hazards() && !immune;
        tickBreathing(player, data, env.breathable(), immune, hazards);
        tickTemperature(player, data, env.ambientC(), env.pressureKpa(), hazards);
        tickRadiation(player, data, hazards);
        tickPressureWear(player, env.pressureKpa(), hazards);
    }

    // ------------------------------------------------------------------ crushing pressure

    /** Above this ambient pressure (10 bar) a sealed suit is under strain: Venus' surface is ~92 bar. */
    public static final double STRAIN_KPA = 1000.0;
    /** One point of wear on every suit piece this often while under strain. */
    public static final int STRAIN_WEAR_TICKS = 200;
    /** Pressure alone never wears a piece past this fraction, so a strained suit never leaks: Venus stays playable. */
    public static final double STRAIN_WEAR_LIMIT = 0.75;

    /** Crushing pressure slowly wears the suit (repair it between trips); it never ruins it on its own. */
    public static void tickPressureWear(Player player, double pressureKpa, boolean hazards) {
        if (!hazards || pressureKpa < STRAIN_KPA || player.tickCount % STRAIN_WEAR_TICKS != 0 || !SpaceSuitItem.isSealed(player)) {
            return;
        }
        SuitWear.wearAll(player, 1, STRAIN_WEAR_LIMIT);
    }

    // ------------------------------------------------------------------ oxygen

    static void tickBreathing(Player player, SurvivalData data, boolean breathable, boolean immune, boolean hazards) {
        boolean underwater = player.isEyeInFluid(FluidTags.WATER);
        boolean sealed = SpaceSuitItem.isSealed(player);
        boolean needsSuit = !breathable || underwater;
        boolean supplied = false;
        double rate = OxygenUse.rate(player);
        data.oxygenRate = OxygenUse.follow(data.oxygenRate, rate);
        if (needsSuit && sealed && !immune) {
            ItemStack torso = SuitTanks.wornTorso(player);
            supplied = OxygenUse.breathe(torso, data, rate);
        } else if (needsSuit && sealed) {
            supplied = SuitTanks.oxygen(SuitTanks.wornTorso(player)) > 0 || immune;
        }
        if (!needsSuit || supplied || immune) {
            data.breath = Math.min(SurvivalData.MAX_BREATH, data.breath + BREATH_RECOVERY);
            if (supplied && underwater) {
                player.setAirSupply(player.getMaxAirSupply());
            }
        } else if (!breathable) {
            data.breath = Math.max(0, data.breath - 1);
            if (data.breath == 0 && hazards && player.tickCount % 20 == 0) {
                SuitDamage.hurt(player, SuitDamage.VACUUM, 2.0F);
            }
        }
        if (!breathable && !underwater) {
            // mirror our breath on the vanilla air bubbles so the hotbar shows it
            player.setAirSupply(Math.round((float) data.breath / SurvivalData.MAX_BREATH * player.getMaxAirSupply()));
        }
    }

    // ------------------------------------------------------------------ temperature

    /**
     * How much harder heat flows in a thick atmosphere than in vacuum: Titan (147 kPa) ~2.5x, Venus capped at 4x.
     * Vacuum and thin air (Mars) barely conduct heat, so only radiation matters there.
     */
    public static double convection(double pressureKpa) {
        return 1.0 + Math.min(3.0, Math.max(0.0, pressureKpa) / 100.0);
    }

    /** Ambient temperature the body actually feels, after the suit's thermal control. */
    public static double effectiveAmbient(Player player, double ambientC, double pressureKpa) {
        SuitProtection.Rating rating = SuitProtection.rating(player);
        if (rating == null) {
            return ambientC;
        }
        double k = convection(pressureKpa);
        double[] range = ratedRange(rating, pressureKpa);
        if (ambientC < range[0]) {
            return SUIT_CABIN_C + (ambientC - range[0]) * k;
        }
        if (ambientC > range[1]) {
            return SUIT_CABIN_C + (ambientC - range[1]) * k;
        }
        return SUIT_CABIN_C;
    }

    /**
     * The temperatures a suit really holds at this pressure: the rating is for vacuum, where heat only leaves by
     * radiation; dense air carries heat away (or in) faster, which shrinks the suit's margin around its cabin
     * temperature by the convection factor - except on the side the suit is built for (heated suit: dense cold,
     * thermal suit: dense heat).
     */
    public static double[] ratedRange(SuitProtection.Rating rating, double pressureKpa) {
        double k = convection(pressureKpa);
        double lo = rating.denseCold() ? rating.minTempC() : SUIT_CABIN_C + (rating.minTempC() - SUIT_CABIN_C) / k;
        double hi = rating.denseHot() ? rating.maxTempC() : SUIT_CABIN_C + (rating.maxTempC() - SUIT_CABIN_C) / k;
        return new double[]{lo, hi};
    }

    public static double targetBodyTemp(double feltC) {
        if (feltC < 5.0) {
            return SurvivalData.NORMAL_BODY_C + (feltC - 5.0) * 0.08;
        }
        if (feltC > 35.0) {
            return SurvivalData.NORMAL_BODY_C + (feltC - 35.0) * 0.1;
        }
        return SurvivalData.NORMAL_BODY_C;
    }

    /** Moves the body temperature one tick toward its target; returns the new value. */
    public static double drift(double bodyC, double targetC) {
        return bodyC + (targetC - bodyC) * TEMP_RATE;
    }

    static void tickTemperature(Player player, SurvivalData data, double ambientC, double pressureKpa, boolean hazards) {
        double target = targetBodyTemp(effectiveAmbient(player, ambientC, pressureKpa));
        if (!hazards) {
            target = SurvivalData.NORMAL_BODY_C; // creative or hazards off: recover
        }
        data.bodyTempC = drift(data.bodyTempC, target);
        if (!hazards || player.tickCount % 20 != 0) {
            return;
        }
        double t = data.bodyTempC;
        if (t < 35.0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, t < 32.0 ? 1 : 0, true, false, true));
            player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze() + 20, player.getTicksFrozen() + 6));
            if (t < 32.0) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, true, false, true));
            }
            if (t < 30.0 && player.tickCount % 40 == 0) {
                SuitDamage.hurt(player, SuitDamage.HYPOTHERMIA, 1.0F);
            }
        } else if (t > 38.5) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false, true));
            player.causeFoodExhaustion(0.3F);
            if (t > 41.0 && player.tickCount % 40 == 0) {
                SuitDamage.hurt(player, SuitDamage.HYPERTHERMIA, 1.0F);
            }
        }
    }

    // ------------------------------------------------------------------ radiation

    public static double radiationFactor(Player player) {
        SuitProtection.Rating rating = SuitProtection.rating(player);
        return rating == null ? 1.0 : rating.radiationFactor();
    }

    /** Dose after one tick at the given rate (mSv/day, one day = 24000 ticks), with slow natural recovery. */
    public static double accumulate(double doseMsv, double rateMsvDay) {
        double d = doseMsv + (rateMsvDay - DOSE_RECOVERY_PER_DAY) / 24000.0;
        return Math.max(0.0, d);
    }

    static void tickRadiation(Player player, SurvivalData data, boolean hazards) {
        data.doseMsv = accumulate(data.doseMsv, data.doseRateMsvDay);
        if (!hazards || player.tickCount % 100 != 0) {
            return;
        }
        double d = data.doseMsv;
        if (d >= 1000) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 0, true, false, true));
        }
        if (d >= 500 && player.getRandom().nextInt(6) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false, true));
        }
        if (d >= 3000) {
            SuitDamage.hurt(player, SuitDamage.RADIATION, d >= 6000 ? 2.0F : 1.0F);
        }
    }
}
