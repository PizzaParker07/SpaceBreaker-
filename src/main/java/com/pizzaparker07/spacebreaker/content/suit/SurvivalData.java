package com.pizzaparker07.spacebreaker.content.suit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-player survival state, stored as a data attachment (saved with the player, reset on death).
 * breath: ticks of held breath left without air (vanilla-like 15 s), bodyTempC: core temperature that
 * drifts slowly toward the environment, doseMsv: accumulated radiation dose.
 */
public final class SurvivalData {
    public static final int MAX_BREATH = 300;
    public static final double NORMAL_BODY_C = 37.0;

    public static final Codec<SurvivalData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("breath", MAX_BREATH).forGetter(d -> d.breath),
            Codec.DOUBLE.optionalFieldOf("body_temp_c", NORMAL_BODY_C).forGetter(d -> d.bodyTempC),
            Codec.DOUBLE.optionalFieldOf("dose_msv", 0.0).forGetter(d -> d.doseMsv)
    ).apply(i, SurvivalData::new));

    public int breath;
    public double bodyTempC;
    public double doseMsv;

    // transient, recomputed on the server
    public double doseRateMsvDay;
    public double ambientC = 15.0;
    public double pressureKpa = 101.325;
    public boolean breathable = true;
    public boolean thrusting;

    public SurvivalData() {
        this(MAX_BREATH, NORMAL_BODY_C, 0.0);
    }

    public SurvivalData(int breath, double bodyTempC, double doseMsv) {
        this.breath = breath;
        this.bodyTempC = bodyTempC;
        this.doseMsv = doseMsv;
    }
}
