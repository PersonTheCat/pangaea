package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.asParent;
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
                if (s.filter.test(s.normalizer.apply(input))) {
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
            if (s.test.decode(ops, map).isSuccess()) {
                return s.codec.decode(ops, map);
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
            final var normalized = s.normalizer.apply(input);
            if (s.filter.test(normalized)) {
                return s.codec.encode(normalized, ops, prefix);
            }
        }
        return prefix.withErrorsFrom(DataResult.error(() -> "Not a structural type: " + input));
    }

    public record Structure<A>(MapCodec<A> codec, MapDecoder<?> test, UnaryOperator<A> normalizer, Predicate<A> filter) {
        @SuppressWarnings("unchecked")
        public static <A, B extends A, T> Function<A, T> get(Function<B, T> getter) {
            return a -> getter.apply((B) a);
        }

        public static <A, B extends A> Structure<A> of(MapCodec<? extends A> codec, Class<B> type) {
            return of(codec, type::isInstance);
        }

        public static <A> Structure<A> of(MapCodec<? extends A> codec, Predicate<A> filter) {
            return new Structure<>(asParent(codec), new SimpleTestCodec(codec), a -> a, filter);
        }

        public Structure<A> withRequiredFields(String... required) {
            return this.withTestPatterns(Stream.of(required).map(f -> Codec.unit(Unit.INSTANCE).fieldOf(f)).toArray(MapCodec[]::new));
        }

        public Structure<A> withTestPatterns(MapDecoder<?>... patterns) {
            return this.withTestPattern(new FullPatternTestCodec(patterns));
        }

        public Structure<A> withTestPattern(MapDecoder<?> test) {
            return new Structure<>(this.codec, test, this.normalizer, this.filter);
        }

        public Structure<A> normalized(UnaryOperator<A> normalizer) {
            return new Structure<>(this.codec, this.test, normalizer, this.filter);
        }
    }

    private static class FullPatternTestCodec extends MapDecoder.Implementation<Unit> {
        private final MapDecoder<?>[] test;

        private FullPatternTestCodec(MapDecoder<?>... test) {
            this.test = test;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(this.test).flatMap(t -> t.keys(ops));
        }

        @Override
        public <T> DataResult<Unit> decode(DynamicOps<T> ops, MapLike<T> map) {
            final var errors = Stream.of(this.test)
                .map(t -> t.decode(ops, map))
                .filter(DataResult::isError)
                .map(r -> r.error().orElseThrow().messageSupplier())
                .toList();
            if (!errors.isEmpty()) {
                return DataResult.error(() -> {
                    final var sb = new StringBuilder("Pattern mismatch");
                    for (final var e : errors) {
                        sb.append("; ").append(e.get());
                    }
                    return sb.toString();
                });
            }
            return DataResult.success(Unit.INSTANCE);
        }
    }

    private static class SimpleTestCodec extends MapDecoder.Implementation<Unit> {
        private final MapDecoder<?> test;

        private SimpleTestCodec(MapDecoder<?> test) {
            this.test = test;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return this.test.keys(ops);
        }

        @Override
        public <T> DataResult<Unit> decode(DynamicOps<T> ops, MapLike<T> map) {
            final var missing = this.test.keys(ops).filter(t -> map.get(t) == null).toList();
            if (!missing.isEmpty()) {
                return DataResult.error(() -> "Pattern mismatch; Missing required fields: " + missing);
            }
            return DataResult.success(Unit.INSTANCE);
        }
    }
}
