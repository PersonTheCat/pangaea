package personthecat.pangaea.serialization.codec.appender;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import personthecat.catlib.serialization.codec.capture.Captor;
import personthecat.catlib.serialization.codec.capture.CapturingCodec;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.defaultType;

public final class PresetAppender implements CodecAppender {
    public static final int PRIORITY = 50; // should probably be the first
    public static final Info<PresetAppender> INFO = PresetAppender::new;
    private final Map<String, List<Captor<?>>> presets = new HashMap<>();
    private volatile Function<?, DataResult<String>> nameFunction;

    public PresetAppender() {
        this.presets.put("default", List.of());
        this.nameFunction = a -> DataResult.success("default");
    }

    public void addPreset(String name, List<Captor<?>> preset) {
        this.presets.put(name, preset);
    }

    public <A> void setNameFunction(Function<A, DataResult<String>> f) {
        this.nameFunction = f;
    }

    @SuppressWarnings("unchecked")
    private <A> DataResult<String> getName(A a) {
        return ((Function<A, DataResult<String>>) this.nameFunction).apply(a);
    }

    @Override
    public <A> Codec<A> append(Codec<A> codec) {
        final MapCodec<A> map = MapCodec.assumeMapUnsafe(codec);
        final var presets = this.compilePresets(map);
        if (presets.size() > 1) {
            final var dispatcher = Codec.STRING
                .partialDispatch("preset", this::getName, s -> getPreset(presets, s));
            return defaultType("preset", dispatcher, map);
        }
        return codec;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Info<PresetAppender> info() {
        return INFO;
    }

    private <A> Map<String, MapCodec<A>> compilePresets(MapCodec<A> base) {
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
