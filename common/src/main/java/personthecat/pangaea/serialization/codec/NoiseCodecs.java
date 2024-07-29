package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import personthecat.catlib.data.FloatRange;
import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.DistanceType;
import personthecat.fastnoise.data.FractalType;
import personthecat.fastnoise.data.MultiType;
import personthecat.fastnoise.data.NoiseBuilder;
import personthecat.fastnoise.data.NoiseType;
import personthecat.fastnoise.data.ReturnType;
import personthecat.fastnoise.data.WarpType;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

import static personthecat.catlib.serialization.codec.CodecUtils.dynamic;
import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;
import static personthecat.catlib.serialization.codec.CodecUtils.typed;
import static personthecat.catlib.serialization.codec.DynamicField.field;

public class NoiseCodecs {
    public static final Codec<NoiseType> TYPE = ofEnum(NoiseType.class);
    public static final Codec<FractalType> FRACTAL = ofEnum(FractalType.class);
    public static final Codec<WarpType> WARP = ofEnum(WarpType.class);
    public static final Codec<DistanceType> DISTANCE = ofEnum(DistanceType.class);
    public static final Codec<ReturnType> RETURN = ofEnum(ReturnType.class);
    public static final Codec<MultiType> MULTI = ofEnum(MultiType.class);
    private static final Codec<NoiseBuilder> BUILDER_CODEC = createNoiseBuilderCodec();
    public static final Codec<FastNoise> NOISE_CODEC = typed(BUILDER_CODEC.xmap(NoiseBuilder::build, FastNoise::toBuilder));

    private static Codec<NoiseBuilder> createNoiseBuilderCodec() {
        final Codec<NoiseBuilder> recursive = Codec.recursive("NoiseBuilder", n -> BUILDER_CODEC);
        return dynamic(FastNoise::builder).create(
            field(TYPE, "type", NoiseBuilder::type, NoiseBuilder::type),
            field(FRACTAL, "fractal", NoiseBuilder::fractal, NoiseBuilder::fractal),
            field(WARP, "warp", NoiseBuilder::warp, NoiseBuilder::warp),
            field(DISTANCE, "distance", NoiseBuilder::distance, NoiseBuilder::distance),
            field(RETURN, "return", NoiseBuilder::cellularReturn, NoiseBuilder::cellularReturn),
            field(MULTI, "multi", NoiseBuilder::multi, NoiseBuilder::multi),
            field(recursive, "noiseLookup", NoiseBuilder::noiseLookup, NoiseBuilder::noiseLookup),
            field(easyList(recursive), "reference", b -> List.of(b.references()), NoiseBuilder::references),
            field(Codec.INT, "seed", NoiseBuilder::seed, NoiseBuilder::seed),
            field(Codec.FLOAT, "frequency", NoiseBuilder::frequencyX, NoiseBuilder::frequency),
            field(Codec.FLOAT, "frequencyX", NoiseBuilder::frequencyX, NoiseBuilder::frequencyX),
            field(Codec.FLOAT, "frequencyY", NoiseBuilder::frequencyY, NoiseBuilder::frequencyY),
            field(Codec.FLOAT, "frequencyZ", NoiseBuilder::frequencyZ, NoiseBuilder::frequencyZ),
            field(Codec.INT, "octaves", NoiseBuilder::octaves, NoiseBuilder::octaves),
            field(Codec.FLOAT, "lacunarity", NoiseBuilder::lacunarityX, NoiseBuilder::lacunarity),
            field(Codec.FLOAT, "lacunarityX", NoiseBuilder::lacunarityX, NoiseBuilder::lacunarityX),
            field(Codec.FLOAT, "lacunarityY", NoiseBuilder::lacunarityY, NoiseBuilder::lacunarityY),
            field(Codec.FLOAT, "lacunarityZ", NoiseBuilder::lacunarityZ, NoiseBuilder::lacunarityZ),
            field(Codec.FLOAT, "gain", NoiseBuilder::gain, NoiseBuilder::gain),
            field(Codec.FLOAT, "pingPongStrength", NoiseBuilder::pingPongStrength, NoiseBuilder::pingPongStrength),
            field(Codec.FLOAT, "jitter", NoiseBuilder::lacunarityX, NoiseBuilder::jitter),
            field(Codec.FLOAT, "jitterX", NoiseBuilder::lacunarityX, NoiseBuilder::jitterX),
            field(Codec.FLOAT, "jitterY", NoiseBuilder::lacunarityY, NoiseBuilder::jitterY),
            field(Codec.FLOAT, "jitterZ", NoiseBuilder::lacunarityZ, NoiseBuilder::jitterZ),
            field(Codec.FLOAT, "warpAmplitude", NoiseBuilder::warpAmplitudeX, NoiseBuilder::warpAmplitude),
            field(Codec.FLOAT, "warpAmplitudeX", NoiseBuilder::warpAmplitudeX, NoiseBuilder::warpAmplitudeX),
            field(Codec.FLOAT, "warpAmplitudeY", NoiseBuilder::warpAmplitudeY, NoiseBuilder::warpAmplitudeY),
            field(Codec.FLOAT, "warpAmplitudeZ", NoiseBuilder::warpAmplitudeZ, NoiseBuilder::warpAmplitudeZ),
            field(Codec.FLOAT, "warpFrequency", NoiseBuilder::warpFrequencyX, NoiseBuilder::warpFrequency),
            field(Codec.FLOAT, "warpFrequencyX", NoiseBuilder::warpFrequencyX, NoiseBuilder::warpFrequencyX),
            field(Codec.FLOAT, "warpFrequencyY", NoiseBuilder::warpFrequencyY, NoiseBuilder::warpFrequencyY),
            field(Codec.FLOAT, "warpFrequencyZ", NoiseBuilder::warpFrequencyZ, NoiseBuilder::warpFrequencyZ),
            field(Codec.FLOAT, "offset", NoiseBuilder::offsetY, NoiseBuilder::offset),
            field(Codec.FLOAT, "offsetX", NoiseBuilder::offsetX, NoiseBuilder::offsetX),
            field(Codec.FLOAT, "offsetY", NoiseBuilder::offsetY, NoiseBuilder::offsetY),
            field(Codec.FLOAT, "offsetZ", NoiseBuilder::offsetZ, NoiseBuilder::offsetZ),
            field(Codec.BOOL, "invert", NoiseBuilder::invert, NoiseBuilder::invert),
            field(FloatRange.CODEC, "range", NoiseCodecs::getRange, NoiseCodecs::setRange),
            field(FloatRange.CODEC, "threshold", NoiseCodecs::getThreshold, NoiseCodecs::setThreshold)
        ).applyFilters((key) -> switch (key) {
            case "reference" -> NoiseCodecs::isWrapperType;
            case "octaves", "gain" -> NoiseCodecs::isFractal;
            case "frequency" -> NoiseCodecs::isSingleFrequency;
            case "frequencyX", "frequencyY", "frequencyZ" -> not(NoiseCodecs::isSingleFrequency);
            case "lacunarity" -> both(NoiseCodecs::isFractal, NoiseCodecs::isSingleLacunarity);
            case "lacunarityX", "lacunarityY", "lacunarityZ" -> both(NoiseCodecs::isFractal, not(NoiseCodecs::isSingleLacunarity));
            case "pingPongStrength" -> NoiseCodecs::isPingPong;
            case "jitter" -> both(NoiseCodecs::isCellular, NoiseCodecs::isSingleJitter);
            case "jitterX", "jitterY", "jitterZ" -> both(NoiseCodecs::isCellular, not(NoiseCodecs::isSingleJitter));
            case "warpAmplitude" -> both(NoiseCodecs::isWarped, NoiseCodecs::isSingleWarpAmplitude);
            case "warpAmplitudeX", "warpAmplitudeY", "warpAmplitudeZ" -> both(NoiseCodecs::isWarped, not(NoiseCodecs::isSingleWarpAmplitude));
            case "warpFrequency" -> both(NoiseCodecs::isWarped, NoiseCodecs::isSingleWarpFrequency);
            case "warpFrequencyX", "warpFrequencyY", "warpFrequencyZ" -> both(NoiseCodecs::isWarped, not(NoiseCodecs::isSingleWarpFrequency));
            case "offset" -> NoiseCodecs::isSingleOffset;
            case "offsetX", "offsetY", "offsetZ" -> not(NoiseCodecs::isSingleOffset);
            default -> null;
        }).validate(b -> {
            if (isWrapperType(b, null) && b.references().length == 0) {
                return DataResult.error(() -> "Must provide a reference for wrapper type: " + b.type(), b);
            } else if (isWrapperType(b, null) && containsUpdatedFrequency(b.references())) {
                return DataResult.error(() -> "Non-default frequency in reference will get ignored, move it up");
            } else if (b.type() == NoiseType.MULTI && b.references().length == 0) {
                return DataResult.error(() -> "Must provided references for multi type", b);
            } else if (b.cellularReturn() == ReturnType.NOISE_LOOKUP && b.noiseLookup() == null) {
                return DataResult.error(() -> "Must provide a noiseLookup for return type: NOISE_LOOKUP", b);
            } else if (b.warp() == WarpType.NOISE_LOOKUP && b.noiseLookup() == null) {
                return DataResult.error(() -> "Must provide a noiseLookup for warp type: NOISE_LOOKUP", b);
            } else if (b.cellularReturn() == ReturnType.NOISE_LOOKUP && b.warp() == WarpType.NOISE_LOOKUP) {
                return DataResult.error(() -> "Conflicting noiseLookup (cellularReturn, warp). Use type == WARPED", b);
            }
            return DataResult.success(b);
        });
    }

    private static <T> boolean isWrapperType(final NoiseBuilder b, final T any) {
        return b.type() == NoiseType.FRACTAL || b.type() == NoiseType.WARPED;
    }

    private static <T> boolean isFractal(final NoiseBuilder b, final T any) {
        return b.fractal() != FractalType.NONE;
    }

    private static <T> boolean isSingleFrequency(final NoiseBuilder b, final T any) {
        return isSingleValue(b.frequencyX(), b.frequencyY(), b.frequencyZ());
    }

    private static <T> boolean isSingleLacunarity(final NoiseBuilder b, final T any) {
        return isSingleValue(b.lacunarityX(), b.lacunarityY(), b.lacunarityZ());
    }

    private static <T> boolean isPingPong(final NoiseBuilder b, final T any) {
        return b.fractal() == FractalType.PING_PONG;
    }
    
    private static <T> boolean isCellular(final NoiseBuilder b, final T any) {
        return b.type() == NoiseType.CELLULAR;
    }

    private static <T> boolean isSingleJitter(final NoiseBuilder b, final T any) {
        return isSingleValue(b.jitterX(), b.jitterY(), b.jitterZ());
    }

    private static <T> boolean isWarped(final NoiseBuilder b, final T any) {
        return b.warp() != WarpType.NONE;
    }

    private static <T> boolean isSingleWarpAmplitude(final NoiseBuilder b, final T any) {
        return isSingleValue(b.warpAmplitudeX(), b.warpAmplitudeY(), b.warpAmplitudeZ());
    }

    private static <T> boolean isSingleWarpFrequency(final NoiseBuilder b, final T any) {
        return isSingleValue(b.warpFrequencyX(), b.warpFrequencyX(), b.warpFrequencyZ());
    }

    private static <T> boolean isSingleOffset(final NoiseBuilder b, final T any) {
        return b.offsetX() != 0 || b.offsetZ() != 0;
    }

    private static <T> boolean isSingleValue(final T t1, final T t2, final T t3) {
        return Objects.equals(t1, t2) && Objects.equals(t2, t3);
    }

    private static FloatRange getRange(final NoiseBuilder b) {
        return new FloatRange(b.scaleAmplitude() + b.scaleOffset(), -b.scaleAmplitude() + b.scaleOffset());
    }

    private static void setRange(final NoiseBuilder b, final FloatRange r) {
        b.range(r.min, r.max);
    }

    private static FloatRange getThreshold(final NoiseBuilder b) {
        return new FloatRange(b.minThreshold(), b.maxThreshold());
    }

    private static void setThreshold(final NoiseBuilder b, final FloatRange r) {
        b.threshold(r.min, r.max);
    }

    private static boolean containsUpdatedFrequency(final NoiseBuilder[] references) {
        for (final NoiseBuilder ref : references) {
            if (ref.frequencyX() != 0.01 || ref.frequencyY() != 0.01 || ref.frequencyZ() != 0.02) {
                return true;
            }
        }
        return false;
    }

    private static <A, B> BiPredicate<A, B> not(final BiPredicate<A, B> predicate) {
        return (a, b) -> !predicate.test(a, b);
    }

    private static <A, B> BiPredicate<A, B> both(final BiPredicate<A, B> c1, final BiPredicate<A, B> c2) {
        return (a, b) -> c1.test(a, b) && c2.test(a, b);
    }
}
