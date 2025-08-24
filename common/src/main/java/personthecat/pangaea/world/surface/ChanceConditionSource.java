package personthecat.pangaea.world.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules.Condition;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import org.jetbrains.annotations.NotNull;
import personthecat.pangaea.extras.ContextExtras;

public record ChanceConditionSource(float chance) implements ConditionSource {
    public static final MapCodec<ChanceConditionSource> CODEC =
        Codec.floatRange(0, 1).fieldOf("chance").xmap(ChanceConditionSource::new, ChanceConditionSource::chance);

    @Override
    public Condition apply(Context ctx) {
        return new ChanceCondition(ctx, this.chance);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends ConditionSource> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    record ChanceCondition(Context ctx, float chance) implements Condition {
        @Override
        public boolean test() {
            return ContextExtras.getRandomSource(this.ctx).nextFloat() <= this.chance;
        }
    }
}
