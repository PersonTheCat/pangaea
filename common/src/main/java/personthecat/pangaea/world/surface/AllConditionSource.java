package personthecat.pangaea.world.surface;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules.Condition;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record AllConditionSource(List<ConditionSource> list) implements ConditionSource {
    public static final MapCodec<AllConditionSource> CODEC =
        ConditionSource.CODEC.listOf(1, 256).fieldOf("list").xmap(AllConditionSource::new, AllConditionSource::list);

    @Override
    public Condition apply(Context ctx) {
        return new AllCondition(this.list.stream().map(s -> s.apply(ctx)).toList());
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
