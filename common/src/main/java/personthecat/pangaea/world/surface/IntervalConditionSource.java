package personthecat.pangaea.world.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules.Condition;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import org.jetbrains.annotations.NotNull;

public record IntervalConditionSource(int interval) implements ConditionSource {
    public static final MapCodec<IntervalConditionSource> CODEC =
        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("interval").xmap(IntervalConditionSource::new, IntervalConditionSource::interval);

    @Override
    public Condition apply(Context ctx) {
        return new IntervalCondition(ctx, this.interval);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends ConditionSource> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    record IntervalCondition(Context ctx, int interval) implements Condition {
        @Override
        public boolean test() {
            return this.ctx.blockX % this.interval == 0
                && this.ctx.blockZ % this.interval == 0
                && this.ctx.blockY % this.interval == 0;
        }
    }
}
