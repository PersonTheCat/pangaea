package personthecat.pangaea.world.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules.Condition;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.FloatRange;
import personthecat.pangaea.data.NoiseGraph;
import personthecat.pangaea.extras.ContextExtras;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public record RoadDistanceConditionSource(FloatRange range, float minSignificant) implements ConditionSource {
    public static final MapCodec<RoadDistanceConditionSource> CODEC = codecOf(
        defaulted(Codec.FLOAT, "min", -Float.MAX_VALUE, s -> s.range.min()),
        defaulted(Codec.FLOAT, "max", Float.MAX_VALUE, s -> s.range.max()),
        RoadDistanceConditionSource::fromMinAndMax
    );

    public RoadDistanceConditionSource(FloatRange range) {
        this(range, computeMinSignificant(range));
    }

    private static float computeMinSignificant(FloatRange r) {
        float minSignificant = 0;
        if (r.min() != -Float.MAX_VALUE) minSignificant = Math.max(minSignificant, r.min() + 8);
        if (r.max() != Float.MAX_VALUE) minSignificant = Math.max(minSignificant, r.max() - 8);
        return minSignificant;
    }

    public static RoadDistanceConditionSource fromMinAndMax(float min, float max) {
        return new RoadDistanceConditionSource(FloatRange.of(min, max));
    }

    public static RoadDistanceConditionSource rangeUp(FloatRange range) {
        return new RoadDistanceConditionSource(range.diff() == 0 ? FloatRange.of(range.min(), Float.MAX_VALUE) : range);
    }

    @Override
    public Condition apply(Context ctx) {
        return new RoadDistanceCondition(ctx, ContextExtras.getNoiseGraph(ctx), this.range, this.minSignificant);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends ConditionSource> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    record RoadDistanceCondition(Context ctx, NoiseGraph graph, FloatRange range, float minSignificant) implements Condition {
        @Override
        public boolean test() {
            return this.range.contains(Math.min(1_000_000F, this.graph.getApproximateRoadDistance(this.ctx.blockX, this.ctx.blockZ, this.minSignificant)));
        }
    }
}
