package personthecat.pangaea.world.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public record NormalDensity(float mean, float deviation) implements ValueDensity {
    public static final MapCodec<NormalDensity> CODEC = codecOf(
        defaulted(Codec.FLOAT, "mean", 0F, NormalDensity::mean),
        defaulted(Codec.FLOAT, "deviation", 1F, NormalDensity::deviation),
        NormalDensity::new
    );

    @Override
    public double compute(RandomSource rand, FunctionContext ctx) {
        return Mth.normal(rand, this.mean, this.deviation);
    }

    @Override
    public double minValue() {
        return this.mean - this.deviation;
    }

    @Override
    public double maxValue() {
        return this.mean + this.deviation;
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    @Override
    public String toString() {
        return "normal(" + this.mean + ", " + this.deviation + ")";
    }
}
