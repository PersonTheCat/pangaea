package personthecat.pangaea.world.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.KeyDispatchCodec;
import lombok.extern.log4j.Log4j2;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import personthecat.catlib.data.FloatRange;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.exception.GenericFormattedException;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.mixin.DensityFunctionsAccessor;
import personthecat.pangaea.mixin.KeyDispatchCodecAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;

@Log4j2
public class DensityFunctionBuilder {
    private static final AtomicReference<Backup<MapCodec<DensityFunction>, DensityFunction>> BACKUP = new AtomicReference<>();
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

    @SuppressWarnings("unchecked")
    public static void install(boolean encoders) {
        try { // it would be ideal to replace the codec altogether, but that is not possible
            final var codec =
                (KeyDispatchCodecAccessor<MapCodec<DensityFunction>, DensityFunction>) resolveDensityCodec();
            if (BACKUP.get() == null) {
                BACKUP.set(new Backup<>(codec.getType(), codec.getDecoder(), codec.getEncoder()));
            }
            codec.setDecoder(k -> DataResult.success(new DensityCodecWrapper(k)));
            if (encoders) {
                codec.setType(DensityFunctionBuilder::resolveCompressedType);
                codec.setEncoder(
                    v -> DataResult.success(new DensityCodecWrapper((MapCodec<DensityFunction>) v.codec().codec())));
            }
            log.info("Successfully installed density builder into codec");
        } catch (final RuntimeException e) {
            LibErrorContext.warn(Pangaea.MOD, new GenericFormattedException(e));
        }
    }

    @SuppressWarnings("unchecked")
    public static void uninstall() {
        final var backup = BACKUP.get();
        if (backup != null) {
            final var codec =
                (KeyDispatchCodecAccessor<MapCodec<DensityFunction>, DensityFunction>) resolveDensityCodec();
            codec.setType(backup.type);
            codec.setDecoder(backup.decoder);
            codec.setEncoder(backup.encoder);
            BACKUP.set(null);
            log.info("Density codec has been restored");
        }
    }

    @SuppressWarnings("unchecked")
    private static KeyDispatchCodec<MapCodec<DensityFunction>, DensityFunction> resolveDensityCodec() {
        final var codec = DensityFunctionsAccessor.getCodec();
        if (codec instanceof MapCodec.MapCodecCodec<DensityFunction> mapCodecCodec) {
            if (mapCodecCodec.codec() instanceof KeyDispatchCodec<?, ?> dispatch) {
                return (KeyDispatchCodec<MapCodec<DensityFunction>, DensityFunction>) dispatch;
            }
        }
        throw new IllegalStateException("Density codec hot-swapped by another mod. Cannot install builders: " + codec);
    }

    @SuppressWarnings("unchecked")
    private static DataResult<MapCodec<DensityFunction>> resolveCompressedType(DensityFunction f) {
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
                    return DataResult.success((MapCodec<DensityFunction>) f.codec().codec());
                }
            }
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

    private record Backup<K, V>(
        Function<V, DataResult<K>> type,
        Function<K, DataResult<MapDecoder< V>>> decoder,
        Function<V, DataResult<MapEncoder<V>>> encoder) {}

    private static class DensityCodecWrapper extends MapCodec<DensityFunction> {
        private static final List<String> ADDED_KEYS = List.of("interpolate", "blend", "cache", "clamp");
        private static final Codec<List<CacheType>> CACHE_CODEC = easyList(ofEnum(CacheType.class));
        private final MapCodec<DensityFunction> wrapped;

        private DensityCodecWrapper(MapCodec<DensityFunction> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.concat(this.wrapped.keys(ops), ADDED_KEYS.stream().map(ops::createString));
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
        @SuppressWarnings("unchecked")
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
            return ((MapCodec<DensityFunction>) input.codec().codec()).encode(input, ops, prefix);
        }
    }
}
