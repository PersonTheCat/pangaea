package personthecat.pangaea.world.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules.Condition;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import net.minecraft.world.level.levelgen.SurfaceRules.LazyXZCondition;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.FloatRange;
import personthecat.pangaea.data.NoiseGraph;
import personthecat.pangaea.extras.ContextExtras;
import personthecat.pangaea.world.surface.NeverConditionSource.NeverCondition;

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
        final var graph = ContextExtras.getNoiseGraph(ctx);
        final var pos = ctx.chunk.getPos();

        final float d = Math.min(1_000_000F, graph.getApproximateRoadDistance(pos.getMiddleBlockX(), pos.getMiddleBlockZ(), this.minSignificant));
        if (d >= this.range.min() - 16 && d < this.range.max() + 16) {
            return new RoadDistanceCondition(ctx, this.range, this.minSignificant);
        }
        return NeverCondition.INSTANCE;
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends ConditionSource> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    static class RoadDistanceCondition extends LazyXZCondition {
        private final NoiseGraph graph;
        private final FloatRange range;
        private final float minSignificant;

        RoadDistanceCondition(Context ctx, FloatRange range, float minSignificant) {
            super(ctx);
            this.graph = ContextExtras.getNoiseGraph(ctx);
            this.range = range;
            this.minSignificant = minSignificant;
        }

        @Override
        public boolean compute() {
            return this.range.contains(Math.min(1_000_000F, this.graph.getApproximateRoadDistance(this.context.blockX, this.context.blockZ, this.minSignificant)));
        }
    }
}
