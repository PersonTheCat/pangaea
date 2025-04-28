package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import org.jetbrains.annotations.Nullable;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.world.placer.BlockPlacer;
import personthecat.pangaea.world.placer.ChanceBlockPlacer;
import personthecat.pangaea.world.placer.ColumnRestrictedBlockPlacer;
import personthecat.pangaea.world.placer.TargetedBlockPlacer;
import personthecat.pangaea.world.provider.ColumnProvider;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.defaultType;

public final class BlockPlacerBuilder extends MapCodec<BlockPlacer> {
    public static final BlockPlacerBuilder INSTANCE = new BlockPlacerBuilder();

    private static final List<StructuralType<?, ?>> STRUCTURAL_TYPES = List.of(
        StructuralType.of(BlockPlacer.class)
            .withCodec(BlockPlacer.CODEC, "place")
            .withConstructor((p, w) -> p)
            .withDestructor(Function.identity(), Function.identity()),
        StructuralType.of(ColumnRestrictedBlockPlacer.class)
            .withCodec(ColumnProvider.CODEC, "column")
            .withConstructor(ColumnRestrictedBlockPlacer::new)
            .withDestructor(ColumnRestrictedBlockPlacer::column, ColumnRestrictedBlockPlacer::place),
        StructuralType.of(ChanceBlockPlacer.class)
            .withCodec(Codec.DOUBLE, "chance")
            .withConstructor(ChanceBlockPlacer::new)
            .withDestructor(ChanceBlockPlacer::chance, ChanceBlockPlacer::place),
        StructuralType.of(TargetedBlockPlacer.class)
            .withCodec(RuleTest.CODEC, "target")
            .withConstructor(TargetedBlockPlacer::new)
            .withDestructor(TargetedBlockPlacer::target, TargetedBlockPlacer::place)
    );

    private BlockPlacerBuilder() {}

    public static Codec<BlockPlacer> wrap(Codec<BlockPlacer> codec) {
        return defaultType(codec, INSTANCE.codec(),
            (p, o) -> Cfg.encodeStructuralBlockPlacers() && canBeStructural(p));
    }

    private static boolean canBeStructural(BlockPlacer p) {
        for (final var type : STRUCTURAL_TYPES) {
            if (type.type.isInstance(p)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return STRUCTURAL_TYPES.stream().map(StructuralType::field).map(ops::createString);
    }

    @Override
    public <T> DataResult<BlockPlacer> decode(DynamicOps<T> ops, MapLike<T> input) {
        DataResult<BlockPlacer> result = null;
        for (int i = 0; i < STRUCTURAL_TYPES.size(); i++) {
            result = STRUCTURAL_TYPES.get(i).append(result, ops, input);
            if (i == 0 && result == null) {
                return DataResult.error(() -> "nothing to place (missing 'place')");
            }
        }
        if (result == null) {
            return DataResult.error(() -> "no structural fields present");
        }
        return result;
    }

    @Override
    public <T> RecordBuilder<T> encode(BlockPlacer input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        boolean anyChanges = false;

        unwrapping: while (true) {
            for (final var type : STRUCTURAL_TYPES) {
                final var next = type.unwrap(input, ops, prefix);
                if (next != input) {
                    anyChanges = true;
                    input = next;
                    continue unwrapping;
                }
            }
            break;
        }
        if (!anyChanges) {
            final var finalInput = input;
            return prefix.withErrorsFrom(DataResult.error(() -> "not a structural type: " + finalInput));
        }
        return prefix;
    }

    private record StructuralType<P, A>(
            Codec<A> codec,
            String field,
            Class<P> type,
            PlacerConstructor<A> constructor,
            PlacerDestructor<P, A> destructor) {

        private static <P> StructuralTypeBuilder<P> of(Class<P> type) {
            return new StructuralTypeBuilder<>(type);
        }

        private @Nullable <T> DataResult<BlockPlacer> append(
                @Nullable DataResult<BlockPlacer> result, DynamicOps<T> ops, MapLike<T> input) {
            if (result != null && result.isError()) {
                return result;
            }
            final var arg = input.get(this.field);
            if (arg == null) {
                return result;
            }
            return this.codec.parse(ops, arg)
                .map(a -> this.constructor.construct(a, result != null ? result.getOrThrow() : null))
                .mapError(e -> this.field + ": " + e);
        }

        private <T> BlockPlacer unwrap(BlockPlacer input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            if (this.type.isInstance(input)) {
                final var p = this.type.cast(input);
                prefix.add(this.field, this.codec.encodeStart(ops, this.destructor.aGetter.apply(p)));
                return this.destructor.nextGetter.apply(p);
            }
            return input;
        }
    }

    private record StructuralTypeBuilder<P>(Class<P> type) {
        <A> BuilderWithCodec<P, A> withCodec(Codec<A> codec, String field) {
            return new BuilderWithCodec<>(codec, field, this.type);
        }
    }

    private record BuilderWithCodec<P, A>(Codec<A> codec, String field, Class<P> type) {
        BuilderWithConstructor<P, A> withConstructor(PlacerConstructor<A> constructor) {
            return new BuilderWithConstructor<>(this.codec, this.field, this.type, constructor);
        }
    }

    private record BuilderWithConstructor<P, A>(
            Codec<A> codec, String field, Class<P> type, PlacerConstructor<A> constructor) {
        StructuralType<P, A> withDestructor(Function<P, A> getter, Function<P, BlockPlacer> nextGetter) {
            return new StructuralType<>(
                this.codec, this.field, this.type, this.constructor, new PlacerDestructor<>(getter, nextGetter));
        }
    }

    @FunctionalInterface
    private interface PlacerConstructor<A> {
        BlockPlacer construct(A a, BlockPlacer wrapped);
    }

    private record PlacerDestructor<P, A>(Function<P, A> aGetter, Function<P, BlockPlacer> nextGetter) {}
}
