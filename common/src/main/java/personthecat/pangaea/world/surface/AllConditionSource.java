package personthecat.pangaea.world.surface;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules.Condition;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import org.jetbrains.annotations.NotNull;
import personthecat.pangaea.world.surface.NeverConditionSource.NeverCondition;

import java.util.List;

public record AllConditionSource(List<ConditionSource> list) implements ConditionSource {
    public static final MapCodec<AllConditionSource> CODEC =
        ConditionSource.CODEC.listOf(1, 256).fieldOf("list").xmap(AllConditionSource::new, AllConditionSource::list);

    @Override
    public Condition apply(Context ctx) {
        final var conditions = ImmutableList.<Condition>builder();
        for (final var source : this.list) {
            final var condition = source.apply(ctx);
            if (condition == NeverCondition.INSTANCE) {
                return NeverCondition.INSTANCE; // result will always be false for this chunk
            }
            conditions.add(condition);
        }
        return new AllCondition(conditions.build());
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends ConditionSource> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    record AllCondition(List<Condition> list) implements Condition {
        @Override
        public boolean test() {
            return list.stream().allMatch(Condition::test);
        }
    }
}
