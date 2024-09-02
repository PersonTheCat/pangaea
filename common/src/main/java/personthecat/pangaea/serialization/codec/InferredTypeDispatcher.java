package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.catlib.serialization.codec.DefaultTypeCodec;

import java.util.function.Function;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.asMapCodec;

public class InferredTypeDispatcher<K, V> extends MapCodec<V> {
    private final RegistryHandle<K> handle;
    private final ResourceKey<Registry<V>> type;
    private final Function<? super K, ? extends MapCodec<? extends V>> codec;

    private InferredTypeDispatcher(
            RegistryHandle<K> handle,
            ResourceKey<Registry<V>> type,
            Function<? super K, ? extends MapCodec<? extends V>> codec) {
        this.handle = handle;
        this.type = type;
        this.codec = codec;
    }

    public static <K, V> Builder<K, V> builder(RegistryHandle<K> handle, ResourceKey<Registry<V>> type) {
        return (typeGetter, codecGetter) ->
            new DefaultTypeCodec<>(
                asMapCodec(handle.codec().dispatch(typeGetter, codecGetter)),
                new InferredTypeDispatcher<>(handle, type, codecGetter),
                (a, ops) -> PgCodecs.inferFromPath(handle, type, ops) != null);
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.empty(); // Not compressible
    }

    @Override
    public <T> DataResult<V> decode(DynamicOps<T> ops, MapLike<T> input) {
        return this.doInfer(ops).flatMap(codec -> codec.decode(ops, input));
    }

    @Override
    public <T> RecordBuilder<T> encode(V input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return this.doInfer(ops).mapOrElse(
            codec -> codec.encode(input, ops, prefix), prefix::withErrorsFrom);
    }

    @SuppressWarnings("unchecked")
    private <T> DataResult<MapCodec<V>> doInfer(DynamicOps<T> ops) {
        final var k = PgCodecs.inferFromPath(this.handle, this.type, ops);
        if (k == null) return DataResult.error(() -> "Cannot infer type: " + this.type);
        return DataResult.success((MapCodec<V>) this.codec.apply(k));
    }

    public interface Builder<K, V> {
        MapCodec<V> buildMap(Function<? super V, ? extends K> type, Function<? super K, ? extends MapCodec<? extends V>> codec);

        default Codec<V> build(Function<? super V, ? extends K> type, Function<? super K, ? extends MapCodec<? extends V>> codec) {
            return this.buildMap(type, codec).codec();
        }
    }
}
