package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import personthecat.catlib.data.FloatRange;
import personthecat.catlib.serialization.codec.capture.CapturingCodec;
import personthecat.catlib.serialization.codec.capture.Key;
import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.DistanceType;
import personthecat.fastnoise.data.Float3;
import personthecat.fastnoise.data.FractalType;
import personthecat.fastnoise.data.MultiType;
import personthecat.fastnoise.data.NoiseBuilder;
import personthecat.fastnoise.data.NoiseType;
import personthecat.fastnoise.data.ReturnType;
import personthecat.fastnoise.data.WarpType;
import personthecat.pangaea.util.SeedSupport;

import java.util.List;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.filter;
import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleAny;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public class NoiseCodecs {
    public static final Codec<NoiseType> TYPE = ofEnum(NoiseType.class);
    public static final Codec<FractalType> FRACTAL = ofEnum(FractalType.class);
    public static final Codec<WarpType> WARP = ofEnum(WarpType.class);
    public static final Codec<DistanceType> DISTANCE = ofEnum(DistanceType.class);
    public static final Codec<ReturnType> RETURN = ofEnum(ReturnType.class);
    public static final Codec<MultiType> MULTI = ofEnum(MultiType.class);
    private static final Key<NoiseBuilder> CATEGORY_KEY = Key.of("NoiseBuilder");

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

    private static final Codec<NoiseBuilder> RECURSIVE_CODEC =
        Codec.recursive("NoiseBuilder", nope -> NoiseCodecs.BUILDER_CODEC.codec());

    private static final MapCodec<NoiseBuilder> PING_PONG_CODEC = PangaeaCodec.buildMap(cat -> codecOf(
        cat.defaulted(Codec.FLOAT, "ping_pong_strength", 2.0F, NoiseBuilder::pingPongStrength),
        NoiseCodecs::pingPongBuilder
    ));

    private static final MapCodec<NoiseBuilder> FRACTAL_CODEC = PangaeaCodec.buildMap(cat -> codecOf(
        cat.defaulted(FRACTAL, "fractal", FractalType.NONE, NoiseBuilder::fractal),
        cat.defaulted(Codec.INT, "octaves", 3, NoiseBuilder::octaves),
        cat.defaulted(Codec.FLOAT, "gain", 0.5F, NoiseBuilder::gain),
        cat.defaulted(FLOAT_3, "lacunarity", float3(2.0F), NoiseCodecs::getLacunarity),
        union(filter(PING_PONG_CODEC, b -> b.fractal() == FractalType.PING_PONG), b -> b),
        NoiseCodecs::fractalBuilder
    ));

    private static final MapCodec<NoiseBuilder> NOISE_LOOKUP_CODEC = PangaeaCodec.buildMap(cat -> codecOf(
        cat.nullable(RECURSIVE_CODEC, "noise_lookup", NoiseBuilder::noiseLookup),
        NoiseCodecs::lookupBuilder
    ));

    private static final MapCodec<NoiseBuilder> WARP_CODEC = PangaeaCodec.buildMap(cat -> codecOf(
        cat.defaulted(WARP, "warp", WarpType.NONE, NoiseBuilder::warp),
        cat.defaulted(FLOAT_3, "warp_frequency", float3(0.075F), NoiseCodecs::getWarpFrequency),
        cat.defaulted(FLOAT_3, "warp_amplitude", float3(5.0F), NoiseCodecs::getWarpAmplitude),
        union(filter(NOISE_LOOKUP_CODEC, b -> b.warp() == WarpType.NOISE_LOOKUP), b -> b),
        NoiseCodecs::warpBuilder
    ));

    private static final MapCodec<NoiseBuilder> CELLULAR_CODEC = PangaeaCodec.buildMap(cat -> codecOf(
        cat.defaulted(DISTANCE, "distance", DistanceType.EUCLIDEAN, NoiseBuilder::distance),
        cat.defaulted(RETURN, "return", ReturnType.CELL_VALUE, NoiseBuilder::cellularReturn),
        cat.defaulted(FLOAT_3, "jitter", float3(1.0F), NoiseCodecs::getJitter),
        union(filter(NOISE_LOOKUP_CODEC, b -> b.cellularReturn() == ReturnType.NOISE_LOOKUP), b -> b),
        NoiseCodecs::cellularBuilder
    ));

    private static final MapCodec<NoiseBuilder> REFERENCE_CODEC = PangaeaCodec.buildMap(cat -> codecOf(
        cat.defaulted(easyList(RECURSIVE_CODEC), "reference", List.of(), b -> List.of(b.references())),
        NoiseCodecs::referenceBuilder
    ));

    private static final MapCodec<NoiseBuilder> MULTI_CODEC = PangaeaCodec.buildMap(cat -> codecOf(
        cat.defaulted(MULTI, "multi", MultiType.SUM, NoiseBuilder::multi),
        union(REFERENCE_CODEC, b -> b),
        NoiseCodecs::multiBuilder
    ));

    private static final MapCodec<NoiseBuilder> BUILDER_CODEC = PangaeaCodec.buildMap(cat -> codecOf(
        cat.defaulted(TYPE, "noise", NoiseType.SIMPLEX, NoiseBuilder::type),
        cat.defaulted(Codec.INT, "seed", 1337, NoiseBuilder::seed),
        cat.defaulted(FLOAT_3, "frequency", float3(0.01F), NoiseCodecs::getFrequency),
        cat.defaulted(FLOAT_3, "offset", float3(0.0F), NoiseCodecs::getOffset),
        cat.defaulted(Codec.BOOL, "invert", false, NoiseBuilder::invert),
        cat.defaulted(FloatRange.CODEC, "range", FloatRange.of(-1.0F, 1.0F), NoiseCodecs::getRange),
        cat.defaulted(FloatRange.RANGE_UP_CODEC, "threshold", FloatRange.of(0.0F, Float.MAX_VALUE), NoiseCodecs::getThreshold),
        union(filter(REFERENCE_CODEC, b -> isWrapperType(b.type())), b -> b),
        union(filter(FRACTAL_CODEC, b -> b.type() == NoiseType.FRACTAL || b.fractal() != FractalType.NONE), b -> b),
        union(filter(WARP_CODEC, b -> b.type() == NoiseType.WARPED || b.warp() != WarpType.NONE), b -> b),
        union(filter(CELLULAR_CODEC, b -> b.type() == NoiseType.CELLULAR), b -> b),
        union(filter(MULTI_CODEC, b -> b.type() == NoiseType.MULTI), b -> b),
        NoiseCodecs::builderBuilder
    ));

    public static final MapCodec<FastNoise> NOISE_CODEC = Util.make(() -> {
        final var codec = BUILDER_CODEC.validate(NoiseCodecs::validateBuilder)
            .xmap(NoiseBuilder::build, FastNoise::toBuilder);
        return CapturingCodec.builder()
            .capturing(SeedSupport.captureAsInt(Key.<Integer>of("seed").qualified(CATEGORY_KEY)))
            .build(codec);
    });

    private static NoiseBuilder pingPongBuilder(float pingPongStrength) {
        return FastNoise.builder().pingPongStrength(pingPongStrength);
    }

    private static NoiseBuilder fractalBuilder(
            FractalType fractal, int octaves, float gain, Float3 lacunarity, NoiseBuilder pingPongSettings) {
        final var builder = FastNoise.builder().fractal(fractal).octaves(octaves).gain(gain).pingPongStrength(pingPongSettings.pingPongStrength());
        setLacunarity(builder, lacunarity);
        return builder;
    }

    private static NoiseBuilder warpBuilder(WarpType warp, Float3 warpAmplitude, Float3 warpFrequency, NoiseBuilder lookupSettings) {
        final var builder = FastNoise.builder().warp(warp).noiseLookup(lookupSettings.noiseLookup());
        setWarpAmplitude(builder, warpAmplitude);
        setWarpFrequency(builder, warpFrequency);
        return builder;
    }

    private static NoiseBuilder lookupBuilder(NoiseBuilder noiseLookup) {
        return FastNoise.builder().noiseLookup(noiseLookup);
    }

    private static NoiseBuilder cellularBuilder(DistanceType distance, ReturnType returnType, Float3 jitter, NoiseBuilder lookupSettings) {
        final var builder = FastNoise.builder().distance(distance).cellularReturn(returnType).noiseLookup(lookupSettings.noiseLookup());
        setJitter(builder, jitter);
        return builder;
    }

    private static NoiseBuilder referenceBuilder(List<NoiseBuilder> references) {
        return FastNoise.builder().references(references);
    }

    private static NoiseBuilder multiBuilder(MultiType multi, NoiseBuilder referenceSettings) {
        return FastNoise.builder().multi(multi).references(referenceSettings.references());
    }

    private static NoiseBuilder builderBuilder(
            NoiseType noise, int seed, Float3 frequency, Float3 offset, boolean invert, FloatRange range, FloatRange threshold,
            NoiseBuilder referenceSettings, NoiseBuilder fractalSettings, NoiseBuilder warpSettings, NoiseBuilder cellularSettings,
            NoiseBuilder multiSettings) {
        final var builder = FastNoise.builder().type(noise).seed(seed).invert(invert);
        setFrequency(builder, frequency);
        setOffset(builder, offset);
        setRange(builder, range);
        setThreshold(builder, threshold);

        // reference settings
        if (referenceSettings.references().length != 0) {
            builder.references(referenceSettings.references());
        }
        // fractal settings
        builder.fractal(fractalSettings.fractal()).gain(fractalSettings.gain()).pingPongStrength(fractalSettings.pingPongStrength())
            .lacunarityX(fractalSettings.lacunarityX()).lacunarityY(fractalSettings.lacunarityY()).lacunarityZ(fractalSettings.lacunarityZ());

        // warp settings
        builder.warp(warpSettings.warp()).warpFrequencyX(warpSettings.warpFrequencyX()).warpFrequencyY(warpSettings.warpFrequencyY())
            .warpFrequencyZ(warpSettings.warpFrequencyZ()).warpAmplitudeX(warpSettings.warpAmplitudeX()).warpAmplitudeY(warpSettings.warpAmplitudeY())
            .warpAmplitudeZ(warpSettings.warpAmplitudeZ());
        if (warpSettings.noiseLookup() != null) {
            builder.noiseLookup(warpSettings.noiseLookup());
        }
        // cellular settings
        builder.distance(cellularSettings.distance()).cellularReturn(cellularSettings.cellularReturn()).jitterX(cellularSettings.jitterX())
            .jitterY(cellularSettings.jitterY()).jitterZ(cellularSettings.jitterZ());
        if (cellularSettings.noiseLookup() != null) {
            builder.noiseLookup(cellularSettings.noiseLookup());
        }
        // multi settings
        builder.multi(multiSettings.multi());
        if (multiSettings.references().length != 0) {
            builder.references(multiSettings.references());
        }
        return builder;
    }

    private static DataResult<NoiseBuilder> validateBuilder(final NoiseBuilder b) {
        if (isWrapperType(b.type()) && b.references().length == 0) {
            return DataResult.error(() -> "Must provide a reference for wrapper type: " + b.type(), b);
        } else if (isWrapperType(b.type()) && containsUpdatedFrequency(b.references())) {
            return DataResult.error(() -> "Non-default frequency in reference will get ignored, move it up");
        } else if (b.type() == NoiseType.MULTI && isWrappedAutomatically(b)) {
            return DataResult.error(() -> "Multi noise cannot be warped or have a fractal effect (yet)", b);
        } else if (b.type() == NoiseType.MULTI && b.references().length == 0) {
            return DataResult.error(() -> "Must provided references for multi type", b);
        } else if (b.cellularReturn() == ReturnType.NOISE_LOOKUP && b.noiseLookup() == null) {
            return DataResult.error(() -> "Must provide a noise_lookup for return type: NOISE_LOOKUP", b);
        } else if (b.warp() == WarpType.NOISE_LOOKUP && b.noiseLookup() == null) {
            return DataResult.error(() -> "Must provide a noise_lookup for warp type: NOISE_LOOKUP", b);
        } else if (b.cellularReturn() == ReturnType.NOISE_LOOKUP && b.warp() == WarpType.NOISE_LOOKUP) {
            return DataResult.error(() -> "Conflicting noiseLookup (cellularReturn, warp). Use type == WARPED", b);
        }
        return DataResult.success(b);
    }

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

    private static boolean isWrapperType(final NoiseType type) {
        return type == NoiseType.FRACTAL || type == NoiseType.WARPED;
    }

    private static boolean isWrappedAutomatically(final NoiseBuilder b) {
        return b.fractal() != FractalType.NONE || b.warp() != WarpType.NONE;
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

    private static Float3 float3(float value) {
        return new Float3(value, value, value);
    }
}
