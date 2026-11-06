package com.pizzaparker07.spacebreaker.content.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Progression milestones for the main advancement tree. Other areas call {@code Milestones.award(player, id)} when
 * something happens that vanilla criteria cannot see (the advancement JSON uses trigger {@code spacebreaker:milestone}
 * with {@code "milestone": "<id>"}). Unknown ids are harmless.
 * <ul>
 *   <li>{@link #FIRST_ROCKET} — rocketry: a rocket was assembled</li>
 *   <li>{@link #LAUNCH} — rocketry: lift-off</li>
 *   <li>{@link #ORBIT} — rocketry: stable orbit reached (also covered by entering an orbit dimension)</li>
 *   <li>{@link #MOON_LANDING} — rocketry/moon: touchdown on the Moon (also covered by entering the Moon)</li>
 *   <li>{@link #SAFE_RETURN} — rocketry: landed back on Earth</li>
 * </ul>
 */
public final class Milestones {
    public static final String FIRST_ROCKET = "first_rocket";
    public static final String LAUNCH = "launch";
    public static final String ORBIT = "orbit";
    public static final String MOON_LANDING = "moon_landing";
    public static final String SAFE_RETURN = "safe_return";
    public static final String OPEN_GUIDE = "open_guide";

    public static void award(ServerPlayer player, String milestone) {
        ResearchModule.MILESTONE.get().trigger(player, milestone);
    }

    public static class Trigger extends SimpleCriterionTrigger<Trigger.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player, String milestone) {
            trigger(player, i -> i.milestone().equals(milestone));
        }

        public record Instance(Optional<ContextAwarePredicate> player, String milestone) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                    Codec.STRING.fieldOf("milestone").forGetter(Instance::milestone)
            ).apply(i, Instance::new));
        }
    }

    private Milestones() {
    }
}
