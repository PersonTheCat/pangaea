package personthecat.pangaea.world.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.SimpleFunction;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.FloatRange;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record DensityCutoff(double min, double max, double harshness) {
    public static final MapCodec<DensityCutoff> CODEC = codecOf(
        field(FloatRange.CODEC, "range", c -> new FloatRange((float) c.min, (float) c.max)),
        defaulted(Codec.DOUBLE, "harshness", 0.1, c -> c.harshness),
        DensityCutoff::new
    );

    public DensityCutoff(final FloatRange range, final double harshness) {
        this(range.min(), range.max(), harshness);
    }

    public double transformUpper(double d, double y) {
        d += this.harshness;
        d *= Mth.clampedMap(y, this.min, this.max, 1, 0);
        d -= this.harshness;
        return d;
    }

    public double transformLower(double d, double y) {
        d -= this.harshness;
        d *= Mth.clampedMap(y, this.min, this.max, 0, 1);
        d += this.harshness;
        return d;
    }

    public DensityFunction upper(final DensityFunction input) {
        return new UpperTransformer(this, input);
    }

    public DensityFunction lower(final DensityFunction input) {
        return new LowerTransformer(this, input);
    }

    public record UpperTransformer(DensityCutoff cutoff, DensityFunction input) implements SimpleFunction {
        public static final MapCodec<UpperTransformer> CODEC = codecOf(
            field(FloatRange.CODEC, "range", c -> new FloatRange((float) c.cutoff.min, (float) c.cutoff.max)),
            defaulted(Codec.DOUBLE, "harshness", 0.1, c -> c.cutoff.harshness),
            field(DensityFunction.HOLDER_HELPER_CODEC, "input", c -> c.input),
            UpperTransformer::new
        );

        public UpperTransformer(final FloatRange range, final double harshness, final DensityFunction input) {
            this(new DensityCutoff(range, harshness), input);
        }

        @Override
        public double compute(final FunctionContext ctx) {
            return this.cutoff.transformUpper(this.input.compute(ctx), ctx.blockY());
        }

        @Override
        public @NotNull DensityFunction mapAll(final Visitor visitor) {
            return new UpperTransformer(this.cutoff, this.input.mapAll(visitor));
        }

        @Override
        public double minValue() {
            return Math.min(0, this.input.minValue());
        }

        @Override
        public double maxValue() {
            return this.input.maxValue();
        }

        @Override
        public @NotNull KeyDispatchDataCodec<UpperTransformer> codec() {
            return KeyDispatchDataCodec.of(CODEC);
        }
    }

    public record LowerTransformer(DensityCutoff cutoff, DensityFunction input) implements SimpleFunction {
        public static final MapCodec<LowerTransformer> CODEC = codecOf(
            field(FloatRange.CODEC, "range", c -> new FloatRange((float) c.cutoff.min, (float) c.cutoff.max)),
            defaulted(Codec.DOUBLE, "harshness", 0.1, c -> c.cutoff.harshness),
            field(DensityFunction.HOLDER_HELPER_CODEC, "input", c -> c.input),
            LowerTransformer::new
        );

        public LowerTransformer(final FloatRange range, final double harshness, final DensityFunction input) {
            this(new DensityCutoff(range, harshness), input);
        }

        @Override
        public double compute(final FunctionContext ctx) {
            return this.cutoff.transformLower(this.input.compute(ctx), ctx.blockY());
        }

        @Override
        public @NotNull DensityFunction mapAll(final Visitor visitor) {
            return new LowerTransformer(this.cutoff, this.input.mapAll(visitor));
        }

        @Override
        public double minValue() {
            return Math.min(0, this.input.minValue());
        }

        @Override
        public double maxValue() {
            return this.input.maxValue();
        }

        @Override
        public @NotNull KeyDispatchDataCodec<LowerTransformer> codec() {
            return KeyDispatchDataCodec.of(CODEC);
        }
    }
}
