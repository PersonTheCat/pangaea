package personthecat.pangaea.world.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.CompressorHolder;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import lombok.extern.log4j.Log4j2;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import personthecat.catlib.data.FloatRange;
import personthecat.pangaea.config.Cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;

@Log4j2
public class DensityFunctionBuilder {
    private static final List<String> ADDED_KEYS = List.of("interpolate", "blend", "cache", "clamp");
    private static final Codec<List<CacheType>> CACHE_CODEC = easyList(ofEnum(CacheType.class));
    private final DensityFunction wrapped;
    private boolean interpolate;
    private boolean blend;
    private List<CacheType> cache;
    private FloatRange clamp;

    public DensityFunctionBuilder(DensityFunction wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped);
        this.interpolate = false;
        this.blend = false;
        this.cache = new ArrayList<>();
        this.clamp = null;
    }

    public static void install(DensityModificationHook.Injector injector) {
        final var codec = injector.codec();
        codec.setDecoder(codec.getDecoder().andThen(result -> result.map(DensityDecoderWrapper::new)));
        if (Cfg.encodeDensityBuilders()) {
            final var wrapper = new DensityEncoderWrapper(codec.getEncoder());
            codec.setEncoder(codec.getEncoder().andThen(result -> result.map(ignored -> wrapper)));
            codec.setType(new TypeFunctionWrapper(codec.getType()));
        }
    }

    public void interpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }

    public void blend(boolean blend) {
        this.blend = blend;
    }

    public void cache(List<CacheType> cache) {
        this.cache = cache;
    }

    public void clamp(float min, float max) {
        this.clamp = new FloatRange(min, max);
    }

    public DensityFunction build() {
        DensityFunction f = this.wrapped;
        if (this.clamp != null) f = f.clamp(this.clamp.min, this.clamp.max);
        if (this.blend) f = DensityFunctions.blendDensity(f);
        if (this.interpolate) f = DensityFunctions.interpolated(f);
        for (final var c : this.cache) {
            switch (c) {
                case FLAT_CACHE -> f = DensityFunctions.flatCache(f);
                case CACHE_2D -> f = DensityFunctions.cache2d(f);
                case CACHE_ONCE -> f = DensityFunctions.cacheOnce(f);
                case CACHE_ALL_IN_CELL -> f = DensityFunctions.cacheAllInCell(f);
            }
        }
        return f;
    }

    public enum CacheType {
        FLAT_CACHE,
        CACHE_2D,
        CACHE_ONCE,
        CACHE_ALL_IN_CELL
    }

    private record TypeFunctionWrapper(
            Function<DensityFunction, DataResult<MapCodec<DensityFunction>>> wrapped)
            implements Function<DensityFunction, DataResult<MapCodec<DensityFunction>>> {

        @Override
        public DataResult<MapCodec<DensityFunction>> apply(DensityFunction f) {
            if (f == null) {
                return DataResult.error(() -> "Cannot resolve type from null function");
            }
            while (true) {
                switch (f) {
                    case DensityFunctions.MarkerOrMarked m -> f = m.wrapped();
                    case DensityFunctions.BlendDensity b -> f = b.input();
                    case DensityFunctions.Clamp c -> f = c.input();
                    case DensityFunctions.HolderHolder h -> f = h.function().value();
                    default -> {
                        return this.wrapped.apply(f);
                    }
                }
            }
        }
    }

    private static class DensityEncoderWrapper extends CompressorHolder implements MapEncoder<DensityFunction> {
        private final Function<DensityFunction, DataResult<MapEncoder<DensityFunction>>> wrapped;

        private DensityEncoderWrapper(Function<DensityFunction, DataResult<MapEncoder<DensityFunction>>> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public <T> RecordBuilder<T> encode(DensityFunction input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            final List<CacheType> cacheTypes = new ArrayList<>();
            while (true) {
                if (input instanceof DensityFunctions.MarkerOrMarked m) {
                    switch (m.type()) {
                        case Interpolated -> prefix.add("interpolated", ops.createBoolean(true));
                        case FlatCache -> cacheTypes.add(CacheType.FLAT_CACHE);
                        case Cache2D -> cacheTypes.add(CacheType.CACHE_2D);
                        case CacheOnce -> cacheTypes.add(CacheType.CACHE_ONCE);
                        case CacheAllInCell -> cacheTypes.add(CacheType.CACHE_ALL_IN_CELL);
                    }
                    input = m.wrapped();
                } else if (input instanceof DensityFunctions.BlendDensity b) {
                    prefix.add("blend", ops.createBoolean(true));
                    input = b.input();
                } else if (input instanceof DensityFunctions.Clamp c) {
                    final var range = new FloatRange((float) c.minValue(), (float) c.maxValue());
                    prefix.add("clamp", FloatRange.CODEC.encodeStart(ops, range));
                    input = c.input();
                } else if (input instanceof DensityFunctions.HolderHolder holder) {
                    input = holder.function().value();
                } else {
                    break;
                }
            }
            if (!cacheTypes.isEmpty()) {
                prefix.add("cache", CACHE_CODEC.encodeStart(ops, cacheTypes.reversed()));
            }
            if (input == null) {
                return prefix.withErrorsFrom(DataResult.error(() -> "Cannot encode null element from wrapper"));
            }
            final var encoderResult = this.wrapped.apply(input);
            if (encoderResult.isError()) {
                return prefix.withErrorsFrom(encoderResult);
            }
            return encoderResult.getOrThrow().encode(input, ops, prefix);
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return ADDED_KEYS.stream().map(ops::createString);
        }
    }

    private static class DensityDecoderWrapper extends CompressorHolder implements MapDecoder<DensityFunction> {
        private final MapDecoder<DensityFunction> wrapped;

        private DensityDecoderWrapper(MapDecoder<DensityFunction> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public <T> DataResult<DensityFunction> decode(DynamicOps<T> ops, MapLike<T> input) {
            return this.wrapped.decode(ops, input).flatMap(f -> {
                final DensityFunctionBuilder builder = new DensityFunctionBuilder(f);
                ops.getBooleanValue(input.get("interpolate")).ifSuccess(builder::interpolate);
                ops.getBooleanValue(input.get("blend")).ifSuccess(builder::blend);
                final var cache = input.get("cache");
                if (cache != null) {
                    final var result = CACHE_CODEC.decode(ops, cache)
                        .ifSuccess(p -> builder.cache(p.getFirst()));
                    if (result instanceof DataResult.Error<?> error) {
                        return DataResult.error(error.messageSupplier(), f);
                    }
                }
                final var clamp = input.get("clamp");
                if (clamp != null) {
                    final var result = FloatRange.CODEC.decode(ops, clamp)
                        .ifSuccess(p -> builder.clamp(p.getFirst().min, p.getFirst().max));
                    if (result instanceof DataResult.Error<?> error) {
                        return DataResult.error(error.messageSupplier(), f);
                    }
                }
                return DataResult.success(builder.build());
            });
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.concat(this.wrapped.keys(ops), ADDED_KEYS.stream().map(ops::createString));
        }
    }
}
