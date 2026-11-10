package com.lustig7.overdrive.advancement;

import com.lustig7.overdrive.logic.CoolingSource;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;

/**
 * Fires for players near a running Overdrive (checked once a second).
 * <pre>{ "trigger": "overdrive:overclock", "conditions": { "min_multiplier": 16, "coolant": "cryo" } }</pre>
 * {@code coolant} is optional: none, passive, water or cryo.
 */
public class OverclockTrigger extends SimpleCriterionTrigger<OverclockTrigger.Instance> {
    private static final Codec<CoolingSource> COOLANT_CODEC = Codec.STRING.xmap(
            s -> CoolingSource.valueOf(s.toUpperCase(Locale.ROOT)),
            c -> c.name().toLowerCase(Locale.ROOT));

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, int multiplier, CoolingSource coolant) {
        trigger(player, i -> i.matches(multiplier, coolant));
    }

    public record Instance(Optional<ContextAwarePredicate> player, int minMultiplier, Optional<CoolingSource> coolant)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.intRange(1, 1024).optionalFieldOf("min_multiplier", 2).forGetter(Instance::minMultiplier),
                COOLANT_CODEC.optionalFieldOf("coolant").forGetter(Instance::coolant)
        ).apply(i, Instance::new));

        boolean matches(int multiplier, CoolingSource source) {
            return multiplier >= minMultiplier && coolant.map(c -> c == source).orElse(true);
        }
    }
}
