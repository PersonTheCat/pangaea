package personthecat.pangaea.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import personthecat.catlib.data.ResettableLazy;
import personthecat.catlib.serialization.codec.capture.CaptureCategory;
import personthecat.catlib.serialization.codec.capture.CapturingCodec;
import personthecat.catlib.serialization.codec.capture.Captor;
import personthecat.catlib.serialization.codec.capture.Key;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.defaultType;

public class PangaeaCodec<A> implements Codec<A> {
    private static final Map<Key<?>, PangaeaCodec<?>> CODECS = new ConcurrentHashMap<>();
    private final ResettableLazy<Codec<A>> base;
    private final Map<String, List<Captor<?>>> presets;
    private volatile Function<A, DataResult<String>> presetName;
    private final List<Captor<?>> captors;

    private PangaeaCodec(Codec<A> base) {
        this.base = ResettableLazy.of(() -> this.generateCodec(base));
        this.presets = new HashMap<>();
        this.presetName = a -> DataResult.success("default");
        this.captors = new ArrayList<>();
        this.presets.put("default", List.of());
    }

    public static <A> PangaeaCodec<A> get(Class<A> type) {
        return get(Key.of(type.getSimpleName(), type));
    }

    @SuppressWarnings("unchecked")
    public static <A> PangaeaCodec<A> get(Key<A> key) {
        return (PangaeaCodec<A>) Objects.requireNonNull(CODECS.get(key), "No codec for key: " + key);
    }

    @SafeVarargs
    public static <A> MapCodec<A> buildMap(Function<CaptureCategory<A>, MapCodec<A>> builder, A... implicitType) {
        return build(builder, implicitType).mapCodec();
    }

    @SafeVarargs
    public static <A> MapCodec<A> buildMap(String name, Function<CaptureCategory<A>, MapCodec<A>> builder, A... implicitType) {
        return build(Key.of(name, implicitType), builder).mapCodec();
    }

    @SafeVarargs
    public static <A> PangaeaCodec<A> build(Function<CaptureCategory<A>, MapCodec<A>> builder, A... implicitType) {
        final var type = Key.inferType(implicitType);
        return build(Key.of(type.getSimpleName(), type), builder);
    }

    public static <A> PangaeaCodec<A> build(Key<A> key, Function<CaptureCategory<A>, MapCodec<A>> builder) {
        final var codec = new PangaeaCodec<>(builder.apply(CaptureCategory.get(key.name(), key.type())).codec());
        CODECS.put(key, codec);
        return codec;
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        return this.base.get().decode(ops, input);
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        return this.base.get().encode(input, ops, prefix);
    }

    public PangaeaCodec<A> addPreset(String name, List<Captor<?>> preset) {
        this.presets.put(name, preset);
        return this.reset();
    }

    public PangaeaCodec<A> addPresetNameFunction(Function<A, DataResult<String>> getter) {
        this.presetName = getter;
        return this.reset();
    }

    public PangaeaCodec<A> capturing(Captor<?>... captors) {
        return this.capturing(List.of(captors));
    }

    public PangaeaCodec<A> capturing(Collection<Captor<?>> captors) {
        this.captors.addAll(captors);
        return this;
    }

    private PangaeaCodec<A> reset() {
        this.base.reset();
        return this;
    }

    public MapCodec<A> mapCodec() {
        return MapCodec.assumeMapUnsafe(this);
    }

    private Codec<A> generateCodec(Codec<A> base) {
        final MapCodec<A> baseMap = MapCodec.assumeMapUnsafe(base);
        final var presets = this.compilePresets(baseMap);
        if (presets.size() > 1) {
            final var dispatcher = Codec.STRING
                .partialDispatch("preset", this.presetName, s -> getPreset(presets, s));
            base = defaultType("preset", dispatcher, baseMap);
        }
        if (!this.captors.isEmpty()) {
            base = CapturingCodec.builder().capturing(this.captors).build(base);
        }
        return base;
    }

    private Map<String, MapCodec<A>> compilePresets(MapCodec<A> base) {
        return mapValues(this.presets, p -> CapturingCodec.builder().capturing(p).build(base));
    }

    private static <K, V, R> Map<K, R> mapValues(Map<K, V> map, Function<V, R> mapper) {
        if (map.isEmpty()) {
            return Collections.emptyMap();
        }
        return map.entrySet().stream()
            .map(e -> Pair.of(e.getKey(), mapper.apply(e.getValue())))
            .collect(Pair.toMap());
    }

    private static <A> DataResult<MapCodec<A>> getPreset(Map<String, MapCodec<A>> presets, String name) {
        return Optional.ofNullable(presets.get(name))
            .map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "No preset named '" + name + "', options: " + presets.keySet()));
    }
}
