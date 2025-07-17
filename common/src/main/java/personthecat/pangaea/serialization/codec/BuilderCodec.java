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
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.defaultType;
import static personthecat.catlib.serialization.codec.CodecUtils.ifMap;

public class BuilderCodec<A> extends MapCodec<A> {
    private final List<BuilderField<A, ?, ?>> fields;
    private final BooleanSupplier encode;
    private final @Nullable MapCodec<A> union;

    public BuilderCodec(List<BuilderField<A, ?, ?>> fields) {
        this(fields, () -> true);
    }

    public BuilderCodec(List<BuilderField<A, ?, ?>> fields, BooleanSupplier encode) {
        this(fields, encode, null);
    }

    private BuilderCodec(
            List<BuilderField<A, ?, ?>> fields, BooleanSupplier encode, @Nullable MapCodec<A> union) {
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
        for (final BuilderField<A, ?, ?> f : this.fields) {
            if (f.type().isInstance(input)) {
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
        for (final BuilderField<A, ?, ?> f : this.fields) {
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
            for (final BuilderField<A, ?, ?> f : this.fields) {
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

    public interface BuilderField<A, B extends A, T> {
        Class<B> type();
        B wrap(A a, T t);
        Pair<@Nullable A, T> unwrap(B b);
        MapCodec<Optional<T>> codec();

        default @Nullable <O> DataResult<A> append(@Nullable DataResult<A> result, DynamicOps<O> ops, MapLike<O> map) {
            if (result != null && result.isError()) {
                return result;
            }
            final var a = result != null ? result.getOrThrow() : null;
            return this.codec().decode(ops, map).mapOrElse(
                o -> o.map(t -> DataResult.success((A) this.wrap(a, t))).orElse(result),
                e -> DataResult.error(e.messageSupplier())
            );
        }

        default <O> Pair<A, RecordBuilder<O>> next(A input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
            if (this.type().isInstance(input)) {
                final var p = this.unwrap(this.type().cast(input));
                final var a = p.getFirst();
                final var t = p.getSecond();
                return Pair.of(a, this.codec().encode(Optional.of(t), ops, prefix));
            }
            return Pair.of(input, prefix);
        }

        static <A, B extends A> Builder<A, B> of(Class<A> parent, Class<B> type) {
            return new Builder<A, B>(parent, type);
        }

        record Builder<A, B extends A>(Class<A> parent, Class<B> type) {
            public <T> Parsing<A, B, T> parsingRequired(Codec<T> codec, String name) {
                return this.parsingRequired(codec, name, "'" + name + "' is required for builder pattern");
            }

            public <T> Parsing<A, B, T> parsingRequired(Codec<T> codec, String name, String errorMessage) {
                return this.parsing(codec.fieldOf(name).flatXmap(
                    t -> DataResult.success(Optional.of(t)),
                    o -> o.map(DataResult::success).orElseGet(() -> DataResult.error(() -> errorMessage))
                ));
            }

            public <T> Parsing<A, B, T> parsing(Codec<T> codec, String name) {
                return this.parsing(codec.optionalFieldOf(name));
            }

            public <T> Parsing<A, B, T> parsing(MapCodec<Optional<T>> codec) {
                return new Parsing<>(this, codec);
            }
        }

        record Parsing<A, B extends A, T>(Builder<A, B> builder, MapCodec<Optional<T>> codec) {
            public WithWrapper<A, B, T> wrap(BiFunction<T, A, B> wrapper) {
                return new WithWrapper<>(this, (a, t) -> wrapper.apply(t, a));
            }
        }

        record WithWrapper<A, B extends A, T>(Parsing<A, B, T> parsing, BiFunction<A, T, B> wrapper) {
            public WithUnwrapper<A, B, T> unwrap(Function<B, A> unwrapA, Function<B, T> unwrapT) {
                return this.unwrap(b -> Pair.of(unwrapA.apply(b), unwrapT.apply(b)));
            }

            public WithUnwrapper<A, B, T> unwrap(Function<B, Pair<A, T>> unwrapper) {
                return new WithUnwrapper<>(this, unwrapper);
            }
        }

        record WithUnwrapper<A, B extends A, T>(
                Class<B> type,
                BiFunction<A, T, B> wrap,
                Function<B, Pair<@Nullable A, T>> unwrap,
                MapCodec<Optional<T>> codec) implements BuilderField<A, B, T> {

            public WithUnwrapper(WithWrapper<A, B, T> withWrapper, Function<B, Pair<A, T>> unwrapper) {
                this(withWrapper.parsing.builder.type, withWrapper.wrapper, unwrapper, withWrapper.parsing.codec);
            }

            @Override
            public B wrap(A a, T t) {
                return this.wrap.apply(a, t);
            }

            @Override
            public Pair<@Nullable A, T> unwrap(B b) {
                return this.unwrap.apply(b);
            }
        }
    }
}
