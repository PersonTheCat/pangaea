package personthecat.pangaea.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.defaultType;
import static personthecat.catlib.serialization.codec.CodecUtils.ifMap;

public class BuilderCodec<A> extends MapCodec<A> {
    private final List<BuilderField<A, ?>> fields;
    private final BooleanSupplier encode;
    private final @Nullable MapCodec<A> union;

    public BuilderCodec(List<BuilderField<A, ?>> fields) {
        this(fields, () -> true);
    }

    public BuilderCodec(List<BuilderField<A, ?>> fields, BooleanSupplier encode) {
        this(fields, encode, null);
    }

    private BuilderCodec(
            List<BuilderField<A, ?>> fields, BooleanSupplier encode, @Nullable MapCodec<A> union) {
        this.fields = fields;
        this.encode = encode;
        this.union = union;
    }

    public BuilderCodec<A> asUnionOf(@Nullable MapCodec<A> union) {
        return new BuilderCodec<>(this.fields, this.encode, union);
    }

    public Codec<A> asUnionOf(Codec<A> union) {
        return ifMap(union, this.asUnionOf(assumeMapUnsafe(union)), (a, o) ->
            this.encode.getAsBoolean() && this.hasFieldForInput(a));
    }

    public Codec<A> wrap(Codec<A> original) {
        return defaultType(original, this, (o, a) ->
            this.encode.getAsBoolean() && this.hasFieldForInput(a));
    }

    private boolean hasFieldForInput(A input) {
        for (final BuilderField<A, ?> f : this.fields) {
            if (f.filter(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return this.fields.stream().flatMap(f -> f.codec().keys(ops));
    }

    @Override
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> map) {
        DataResult<A> result = null;
        if (this.union != null) {
            result = this.union.decode(ops, map);
        }
        for (final BuilderField<A, ?> f : this.fields) {
            result = f.append(result, ops, map);
        }
        if (result == null) {
            return DataResult.error(() -> "No builder fields present");
        }
        return result;
    }

    @Override
    public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        unwrapping: while (true) {
            for (final BuilderField<A, ?> f : this.fields) {
                final var next = f.next(input, ops, prefix);
                if (next.getFirst() == null) {
                    return prefix; // end
                }
                if (next.getFirst() != input) {
                    input = next.getFirst();
                    prefix = next.getSecond();
                    continue unwrapping;
                }
            }
            break;
        }
        if (this.union != null) {
            return this.union.encode(input, ops, prefix);
        }
        final var finalInput = input;
        return prefix.withErrorsFrom(DataResult.error(() -> "Not a buildable type: " + finalInput));
    }

    public interface BuilderField<A, T> {
        boolean filter(A a);
        A wrap(A a, T t);
        Pair<@Nullable A, T> unwrap(A a);
        MapCodec<Optional<T>> codec();

        default @Nullable <O> DataResult<A> append(@Nullable DataResult<A> result, DynamicOps<O> ops, MapLike<O> map) {
            if (result != null && result.isError()) {
                return result;
            }
            final var a = result != null ? result.getOrThrow() : null;
            return this.codec().decode(ops, map).mapOrElse(
                o -> o.map(t -> DataResult.success(this.wrap(a, t))).orElse(result),
                e -> DataResult.error(e.messageSupplier())
            );
        }

        default <O> Pair<A, RecordBuilder<O>> next(A input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
            if (this.filter(input)) {
                final var p = this.unwrap(input);
                final var a = p.getFirst();
                final var t = p.getSecond();
                return Pair.of(a, this.codec().encode(Optional.of(t), ops, prefix));
            }
            return Pair.of(input, prefix);
        }

        static <A, B extends A> Builder<A> of(Class<A> parent, Class<B> type) {
            return of(parent, type::isInstance);
        }

        static <A> Builder<A> of(Class<A> parent, Predicate<A> filter) {
            return new Builder<>(parent, filter);
        }

        record Builder<A>(Class<A> parent, Predicate<A> filter) {
            public <T> Parsing<A, T> parsingRequired(Codec<T> codec, String name) {
                return this.parsingRequired(codec, name, "'" + name + "' is required for builder pattern");
            }

            public <T> Parsing<A, T> parsingRequired(Codec<T> codec, String name, String errorMessage) {
                return this.parsing(codec.fieldOf(name).flatXmap(
                    t -> DataResult.success(Optional.of(t)),
                    o -> o.map(DataResult::success).orElseGet(() -> DataResult.error(() -> errorMessage))
                ));
            }

            public <T> Parsing<A, T> parsing(Codec<T> codec, String name) {
                return this.parsing(codec.optionalFieldOf(name));
            }

            public <T> Parsing<A, T> parsing(MapCodec<Optional<T>> codec) {
                return new Parsing<>(this, codec);
            }
        }

        record Parsing<A, T>(Builder<A> builder, MapCodec<Optional<T>> codec) {
            public WithWrapper<A, T> wrap(BiFunction<T, A, ? extends A> wrapper) {
                return new WithWrapper<>(this, (a, t) -> wrapper.apply(t, a));
            }
        }

        record WithWrapper<A, T>(Parsing<A, T> parsing, BiFunction<A, T, ? extends A> wrapper) {
            public <B extends A> WithUnwrapper<A, T> unwrap(Function<B, A> unwrapA, Function<B, T> unwrapT) {
                return this.unwrap((B b) -> Pair.of(unwrapA.apply(b), unwrapT.apply(b)));
            }

            public <B extends A> WithUnwrapper<A, T> unwrap(Function<B, Pair<A, T>> unwrapper) {
                return new WithUnwrapper<>(this, asParent(unwrapper));
            }

            @SuppressWarnings("unchecked")
            private static <A, B extends A, R> Function<A, R> asParent(Function<B, R> f) {
                return (Function<A, R>) f;
            }
        }

        record WithUnwrapper<A, T>(
                Predicate<A> filter,
                BiFunction<A, T, ? extends A> wrap,
                Function<A, Pair<@Nullable A, T>> unwrap,
                MapCodec<Optional<T>> codec) implements BuilderField<A, T> {

            public WithUnwrapper(WithWrapper<A, T> ww, Function<A, Pair<A, T>> uw) {
                this(ww.parsing.builder.filter, ww.wrapper, uw, ww.parsing.codec);
            }

            @Override
            public A wrap(A a, T t) {
                return this.wrap.apply(a, t);
            }

            @Override
            public Pair<@Nullable A, T> unwrap(A a) {
                return this.unwrap.apply(a);
            }

            @Override
            public boolean filter(A a) {
                return this.filter.test(a);
            }
        }
    }
}
