package personthecat.pangaea.world.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.SurfaceRules.Condition;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.FloatRange;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.world.density.FastNoiseDensity;

import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.pangaea.world.density.FastNoiseDensity.as3dCodec;

public record DensityConditionSource(DensityFunction density, FloatRange threshold) implements ConditionSource {
    private static final Codec<FloatRange> RANGE_UP =
        FloatRange.CODEC.xmap(r -> r.diff() == 0 ? FloatRange.of(r.min(), Float.MAX_VALUE) : r, Function.identity());
    public static final MapCodec<DensityConditionSource> CODEC = codecOf(
        field(as3dCodec(DensityFunction.HOLDER_HELPER_CODEC), "density", DensityConditionSource::density),
        defaulted(RANGE_UP, "threshold", FloatRange.of(0, Float.MAX_VALUE), DensityConditionSource::threshold),
        DensityConditionSource::new
    );

    public static DensityConditionSource fromFastNoise(FastNoiseDensity d) {
        final var b = d.noise().toBuilder();
        return new DensityConditionSource(d, FloatRange.of(b.minThreshold(), b.maxThreshold()));
    }

    @Override
    public Condition apply(Context ctx) {
        return new DensityCondition(ctx, new MutableFunctionContext(), this.density, this.threshold);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends ConditionSource> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    record DensityCondition(
            Context ctx, MutableFunctionContext pos, DensityFunction density, FloatRange threshold) implements Condition {
        @Override
        public boolean test() {
            return this.threshold.contains((float) this.density.compute(this.pos.set(this.ctx.pos)));
        }
    }
}
