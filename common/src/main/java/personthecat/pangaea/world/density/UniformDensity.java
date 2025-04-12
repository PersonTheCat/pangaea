package personthecat.pangaea.world.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public record UniformDensity(float minInclusive, float maxExclusive) implements ValueDensity {
    public static final MapCodec<UniformDensity> CODEC = codecOf(
        defaulted(Codec.FLOAT, "minInclusive", -1F, UniformDensity::minInclusive),
        defaulted(Codec.FLOAT, "maxExclusive", 1F, UniformDensity::maxExclusive),
        UniformDensity::new
    ).validate(d -> d.maxExclusive <= d.minInclusive
        ? DataResult.error(() ->
            "Max must be larger than min, min_inclusive: " + d.minInclusive + ", max_exclusive: " + d.maxExclusive)
        : DataResult.success(d)
    );

    @Override
    public double compute(RandomSource rand, FunctionContext ctx) {
        return Mth.randomBetween(rand, this.minInclusive, this.maxExclusive);
    }

    @Override
    public double minValue() {
        return this.minInclusive;
    }

    @Override
    public double maxValue() {
        return this.maxExclusive;
    }

    @Override
    public @NotNull KeyDispatchDataCodec<UniformDensity> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    @Override
    public String toString() {
        return "[" + this.minInclusive + "-" + this.maxExclusive + "]";
    }
}
