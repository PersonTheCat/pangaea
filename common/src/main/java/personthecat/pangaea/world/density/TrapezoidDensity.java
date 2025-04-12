package personthecat.pangaea.world.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public record TrapezoidDensity(float min, float max, float plateau) implements ValueDensity {
    public static final MapCodec<TrapezoidDensity> CODEC = codecOf(
        defaulted(Codec.FLOAT, "min", -1F, TrapezoidDensity::min),
        defaulted(Codec.FLOAT, "max", 1F, TrapezoidDensity::max),
        defaulted(Codec.FLOAT, "plateau", 0F, TrapezoidDensity::plateau),
        TrapezoidDensity::new
    ).validate((d) -> {
        if (d.max < d.min) {
            return DataResult.error(() ->
                "Max must be larger than min: [" + d.min + ", " + d.max + "]");
        } else if (d.plateau > d.max - d.min) {
            return DataResult.error(() ->
                "Plateau can at most be the full span: [" + d.min + ", " + d.max + "]");
        }
        return DataResult.success(d);
    });

    @Override
    public double compute(RandomSource rand, FunctionContext ctx) {
        final float s = this.max - this.min;
        final float u = (s - this.plateau) / 2F;
        final float l = s - u;
        return this.min + rand.nextFloat() * l + rand.nextFloat() * u;
    }

    @Override
    public double minValue() {
        return this.min;
    }

    @Override
    public double maxValue() {
        return this.max;
    }

    @Override
    public @NotNull KeyDispatchDataCodec<TrapezoidDensity> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    @Override
    public String toString() {
        return "trapezoid(" + this.plateau + ") in [" + this.min + "-" + this.max + "]";
    }
}
