package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.defaultType;

public class StructuralCodec<A> extends MapCodec<A> {
    private final List<Structure<A>> structures;
    private final BooleanSupplier encode;

    public StructuralCodec(List<Structure<A>> structures) {
        this(structures, () -> true);
    }

    public StructuralCodec(List<Structure<A>> structures, BooleanSupplier encode) {
        this.structures = structures;
        this.encode = encode;
    }

    public Codec<A> wrap(Codec<A> original) {
        return defaultType(original, this, (o, a) ->
            this.encode.getAsBoolean() && this.hasStructureForInput(a));
    }

    public boolean hasStructureForInput(A input) {
        if (this.encode.getAsBoolean()) {
            for (final Structure<A> s : this.structures) {
                if (s.type.isInstance(input)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return this.structures.stream().flatMap(s -> s.codec.keys(ops));
    }

    @Override
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> map) {
        for (final Structure<A> s : this.structures) {
            final var result = s.codec.decode(ops, map);
            if (result.isSuccess()) {
                return result;
            }
        }
        return DataResult.error(() -> "No structural fields or type present");
    }

    @Override
    public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        if (!this.encode.getAsBoolean()) {
            return prefix.withErrorsFrom(
                DataResult.error(() -> "Encoding structural patterns disabled for input: " + input));
        }
        for (final Structure<A> s : this.structures) {
            if (s.type.isInstance(input)) {
                return s.codec.encode(input, ops, prefix);
            }
        }
        return prefix.withErrorsFrom(DataResult.error(() -> "Not a structural type: " + input));
    }

    public record Structure<A>(MapCodec<A> codec, Class<?> type) {
        @SuppressWarnings("unchecked")
        public static <A, B extends A, T> Function<A, T> get(Function<B, T> getter) {
            return a -> getter.apply((B) a);
        }

        public Structure<A> withRequiredFields(String... required) {
            return new Structure<>(new RequiredFieldsMapCodec<>(this.codec, required), this.type);
        }
    }

    private static class RequiredFieldsMapCodec<A> extends MapCodec<A> {
        private final MapCodec<A> delegate;
        private final String[] required;

        private RequiredFieldsMapCodec(MapCodec<A> delegate, String... required) {
            this.delegate = delegate;
            this.required = required;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return this.delegate.keys(ops);
        }

        @Override
        public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> map) {
            final var missing = Stream.of(this.required).filter(field -> map.get(field) == null).toList();
            if (!missing.isEmpty()) {
                return DataResult.error(() -> "Missing required fields: " + missing);
            }
            return this.delegate.decode(ops, map);
        }

        @Override
        public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return this.delegate.encode(input, ops, prefix);
        }
    }
}
