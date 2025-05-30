package personthecat.pangaea.world.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.SimpleFunction;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.serialization.codec.capture.CapturingCodec;
import personthecat.catlib.serialization.codec.capture.Receiver;
import personthecat.fastnoise.FastNoise;
import personthecat.pangaea.serialization.codec.NoiseCodecs;

import static personthecat.catlib.serialization.codec.capture.CapturingCodec.receive;
import static personthecat.catlib.serialization.codec.capture.CapturingCodec.supply;
import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaultTry;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public record FastNoiseDensity(
        FastNoise noise, Mode mode, double minValue, double maxValue) implements SimpleFunction {
    private static final Receiver<Mode> DEFAULT_MODE = receive("mode", Mode.SCALED_2D);
    public static final MapCodec<FastNoiseDensity> CODEC = codecOf(
        union(NoiseCodecs.NOISE_CODEC, FastNoiseDensity::noise),
        defaultTry(ofEnum(Mode.class), "mode", DEFAULT_MODE, FastNoiseDensity::mode),
        FastNoiseDensity::create
    );

    public static FastNoiseDensity create(FastNoise noise, Mode mode) {
        final var builder = noise.toBuilder();
        final var min = -builder.scaleAmplitude() + builder.scaleOffset();
        final var max = builder.scaleAmplitude() + builder.scaleOffset();
        return new FastNoiseDensity(noise, mode, min, max);
    }

    public static <A> Codec<A> as3dCodec(Codec<A> codec) {
        return as3dBuilder().build(codec);
    }

    public static <A> MapCodec<A> as3dCodec(MapCodec<A> codec) {
        return as3dBuilder().build(codec);
    }

    private static CapturingCodec.Builder as3dBuilder() {
        return CapturingCodec.builder().capturing(supply("mode", Mode.SCALED));
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
        BOOL_2D
    }
}
