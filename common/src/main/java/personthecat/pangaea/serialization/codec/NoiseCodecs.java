package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import personthecat.catlib.data.FloatRange;
import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.DistanceType;
import personthecat.fastnoise.data.Float3;
import personthecat.fastnoise.data.FractalType;
import personthecat.fastnoise.data.MultiType;
import personthecat.fastnoise.data.NoiseBuilder;
import personthecat.fastnoise.data.NoiseType;
import personthecat.fastnoise.data.ReturnType;
import personthecat.fastnoise.data.WarpType;

import java.util.List;

import static personthecat.catlib.serialization.codec.CodecUtils.dynamic;
import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleAny;
import static personthecat.catlib.serialization.codec.DynamicField.field;

public class NoiseCodecs {
    private static final Codec<NoiseType> TYPE = ofEnum(NoiseType.class);
    private static final Codec<FractalType> FRACTAL = ofEnum(FractalType.class);
    private static final Codec<WarpType> WARP = ofEnum(WarpType.class);
    private static final Codec<DistanceType> DISTANCE = ofEnum(DistanceType.class);
    private static final Codec<ReturnType> RETURN = ofEnum(ReturnType.class);
    private static final Codec<MultiType> MULTI = ofEnum(MultiType.class);

    private static final Codec<Float3> FLOAT_3 = Util.make(() -> {
        final Codec<Float3> single = Codec.FLOAT.xmap(f -> new Float3(f, f, f), f3 -> f3.x);
        final Codec<Float3> array = Codec.FLOAT.listOf().flatXmap(
            l -> Util.fixedSize(l, 3).map(l2 -> new Float3(l2.getFirst(), l2.get(1), l2.get(2))),
            f3 -> DataResult.success(List.of(f3.x, f3.y, f3.z)));
        final Codec<Float3> object = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.FLOAT.fieldOf("xz").forGetter(f3 -> f3.x),
                Codec.FLOAT.fieldOf("y").forGetter(f3 -> f3.y))
            .apply(instance, (xz, y) -> new Float3(xz, y, xz)));
        return simpleAny(single, array, object)
            .withEncoder(f3 -> f3.x == f3.y && f3.y == f3.z ? single : array);
    });

    public static final MapCodec<FastNoise> NOISE_CODEC =
        MapCodec.<NoiseBuilder>recursive("NoiseBuilder", builder -> dynamic(FastNoise::builder).create(
            field(TYPE, "noise", NoiseBuilder::type, NoiseBuilder::type),
            field(FRACTAL, "fractal", NoiseBuilder::fractal, NoiseBuilder::fractal),
            field(WARP, "warp", NoiseBuilder::warp, NoiseBuilder::warp),
            field(DISTANCE, "distance", NoiseBuilder::distance, NoiseBuilder::distance),
            field(RETURN, "return", NoiseBuilder::cellularReturn, NoiseBuilder::cellularReturn),
            field(MULTI, "multi", NoiseBuilder::multi, NoiseBuilder::multi),
            field(builder, "noise_lookup", NoiseBuilder::noiseLookup, NoiseBuilder::noiseLookup),
            field(easyList(builder), "reference", b -> List.of(b.references()), NoiseBuilder::references),
            field(Codec.INT, "seed", NoiseBuilder::seed, NoiseBuilder::seed),
            field(FLOAT_3, "frequency", NoiseCodecs::getFrequency, NoiseCodecs::setFrequency),
            field(Codec.INT, "octaves", NoiseBuilder::octaves, NoiseBuilder::octaves),
            field(FLOAT_3, "lacunarity", NoiseCodecs::getLacunarity, NoiseCodecs::setLacunarity),
            field(Codec.FLOAT, "gain", NoiseBuilder::gain, NoiseBuilder::gain),
            field(Codec.FLOAT, "ping_pong_strength", NoiseBuilder::pingPongStrength, NoiseBuilder::pingPongStrength),
            field(FLOAT_3, "jitter", NoiseCodecs::getJitter, NoiseCodecs::setJitter),
            field(FLOAT_3, "warp_amplitude", NoiseCodecs::getWarpAmplitude, NoiseCodecs::setWarpAmplitude),
            field(FLOAT_3, "warp_frequency", NoiseCodecs::getWarpFrequency, NoiseCodecs::setWarpFrequency),
            field(FLOAT_3, "offset", NoiseCodecs::getOffset, NoiseCodecs::setOffset),
            field(Codec.BOOL, "invert", NoiseBuilder::invert, NoiseBuilder::invert),
            field(FloatRange.CODEC, "range", NoiseCodecs::getRange, NoiseCodecs::setRange),
            field(FloatRange.CODEC, "threshold", NoiseCodecs::getThreshold, NoiseCodecs::setThreshold)
        ).applyFilters((key) -> switch (key) {
            case "reference" -> NoiseCodecs::isWrapperType;
            case "octaves", "gain", "lacunarity" -> NoiseCodecs::isFractal;
            case "ping_pong_strength" -> NoiseCodecs::isPingPong;
            case "jitter" -> NoiseCodecs::isCellular;
            case "warp_amplitude", "warp_frequency" -> NoiseCodecs::isWarped;
            default -> null;
        }).validate(b -> {
            if (isWrapperType(b, null) && b.references().length == 0) {
                return DataResult.error(() -> "Must provide a reference for wrapper type: " + b.type(), b);
            } else if (isWrapperType(b, null) && containsUpdatedFrequency(b.references())) {
                return DataResult.error(() -> "Non-default frequency in reference will get ignored, move it up");
            } else if (b.type() == NoiseType.MULTI && isWrappedAutomatically(b)) {
                return DataResult.error(() -> "Multi noise cannot be warped or have a fractal effect (yet)", b);
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
        })).xmap(NoiseBuilder::build, FastNoise::toBuilder);

    private static Float3 getFrequency(final NoiseBuilder b) {
        return new Float3(b.frequencyX(), b.frequencyY(), b.frequencyZ());
    }

    private static void setFrequency(final NoiseBuilder b, final Float3 f3) {
        b.frequencyX(f3.x).frequencyY(f3.y).frequencyZ(f3.z);
    }

    private static Float3 getLacunarity(final NoiseBuilder b) {
        return new Float3(b.lacunarityX(), b.lacunarityY(), b.lacunarityZ());
    }

    private static void setLacunarity(final NoiseBuilder b, final Float3 f3) {
        b.lacunarityX(f3.x).lacunarityY(f3.y).lacunarityZ(f3.z);
    }

    private static Float3 getJitter(final NoiseBuilder b) {
        return new Float3(b.jitterX(), b.jitterY(), b.jitterZ());
    }

    private static void setJitter(final NoiseBuilder b, final Float3 f3) {
        b.jitterX(f3.x).jitterY(f3.y).jitterZ(f3.z);
    }

    private static Float3 getWarpAmplitude(final NoiseBuilder b) {
        return new Float3(b.warpAmplitudeX(), b.warpAmplitudeY(), b.warpAmplitudeZ());
    }

    private static void setWarpAmplitude(final NoiseBuilder b, final Float3 f3) {
        b.warpAmplitudeX(f3.x).warpAmplitudeY(f3.y).warpAmplitudeZ(f3.z);
    }

    private static Float3 getWarpFrequency(final NoiseBuilder b) {
        return new Float3(b.warpFrequencyX(), b.warpFrequencyY(), b.warpFrequencyZ());
    }

    private static void setWarpFrequency(final NoiseBuilder b, final Float3 f3) {
        b.warpFrequencyX(f3.x).warpFrequencyY(f3.y).warpFrequencyZ(f3.z);
    }

    private static Float3 getOffset(final NoiseBuilder b) {
        return new Float3(b.offsetX(), b.offsetY(), b.offsetZ());
    }

    private static void setOffset(final NoiseBuilder b, final Float3 f3) {
        b.offsetX(f3.x).offsetY(f3.y).offsetZ(f3.z);
    }

    private static <T> boolean isWrapperType(final NoiseBuilder b, final T any) {
        return b.type() == NoiseType.FRACTAL || b.type() == NoiseType.WARPED;
    }

    private static boolean isWrappedAutomatically(final NoiseBuilder b) {
        return b.fractal() != FractalType.NONE || b.warp() != WarpType.NONE;
    }

    private static <T> boolean isFractal(final NoiseBuilder b, final T any) {
        return b.fractal() != FractalType.NONE;
    }

    private static <T> boolean isPingPong(final NoiseBuilder b, final T any) {
        return b.fractal() == FractalType.PING_PONG;
    }
    
    private static <T> boolean isCellular(final NoiseBuilder b, final T any) {
        return b.type() == NoiseType.CELLULAR;
    }

    private static <T> boolean isWarped(final NoiseBuilder b, final T any) {
        return b.warp() != WarpType.NONE;
    }

    private static FloatRange getRange(final NoiseBuilder b) {
        return new FloatRange(b.scaleAmplitude() + b.scaleOffset(), -b.scaleAmplitude() + b.scaleOffset());
    }

    private static void setRange(final NoiseBuilder b, final FloatRange r) {
        b.range(r.min(), r.max());
    }

    private static FloatRange getThreshold(final NoiseBuilder b) {
        return new FloatRange(b.minThreshold(), b.maxThreshold());
    }

    private static void setThreshold(final NoiseBuilder b, final FloatRange r) {
        b.threshold(r.min(), r.max());
    }

    private static boolean containsUpdatedFrequency(final NoiseBuilder[] references) {
        for (final NoiseBuilder ref : references) {
            if (ref.frequencyX() != 0.01 || ref.frequencyY() != 0.01 || ref.frequencyZ() != 0.02) {
                return true;
            }
        }
        return false;
    }
}
