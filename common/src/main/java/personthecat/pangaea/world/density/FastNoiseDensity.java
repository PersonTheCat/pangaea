package personthecat.pangaea.world.density;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.SimpleFunction;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.serialization.codec.UnionCodec;
import personthecat.fastnoise.FastNoise;
import personthecat.pangaea.serialization.codec.NoiseCodecs;

import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;

public record FastNoiseDensity(
        FastNoise noise, Mode mode, double minValue, double maxValue) implements SimpleFunction {
    private static final MapCodec<Mode> MODE_CODEC =
        ofEnum(Mode.class).optionalFieldOf("mode", Mode.SCALED);
    public static final MapCodec<FastNoiseDensity> CODEC =
        UnionCodec.builder(NoiseCodecs.NOISE_CODEC, MODE_CODEC).create(
            d -> DataResult.success(Pair.of(d.noise, d.mode)),
            p -> DataResult.success(create(p.getFirst(), p.getSecond())));

    public static FastNoiseDensity create(FastNoise noise, Mode mode) {
        final var builder = noise.toBuilder();
        final var min = builder.scaleAmplitude() + builder.scaleOffset();
        final var max = -builder.scaleAmplitude() + builder.scaleOffset();
        return new FastNoiseDensity(noise, mode, min, max);
    }

    @Override
    public double compute(FunctionContext ctx) {
        return switch (this.mode) {
            case RAW -> this.noise.getNoise(ctx.blockX(), ctx.blockY(), ctx.blockZ());
            case RAW_2D -> this.noise.getNoise(ctx.blockX(), ctx.blockZ());
            case SCALED -> this.noise.getNoiseScaled(ctx.blockX(), ctx.blockY(), ctx.blockZ());
            case SCALED_2D -> this.noise.getNoiseScaled(ctx.blockX(), ctx.blockZ());
            case BOOL -> this.noise.getBoolean(ctx.blockX(), ctx.blockY(), ctx.blockZ()) ? 1 : -1;
            case BOOL_2D -> this.noise.getBoolean(ctx.blockX(), ctx.blockZ()) ? 1 : -1;
        };
    }

    @Override
    public @NotNull DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(this);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    public enum Mode {
        RAW,
        RAW_2D,
        SCALED,
        SCALED_2D,
        BOOL,
        BOOL_2D;
    }
}
