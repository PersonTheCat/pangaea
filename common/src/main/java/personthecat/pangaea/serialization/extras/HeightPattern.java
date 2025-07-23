package personthecat.pangaea.serialization.extras;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.VerticalAnchor.AboveBottom;
import net.minecraft.world.level.levelgen.VerticalAnchor.Absolute;
import net.minecraft.world.level.levelgen.VerticalAnchor.BelowTop;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.Range;
import personthecat.catlib.serialization.codec.CodecUtils;
import personthecat.pangaea.data.AnchorCutoff;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.mixin.accessor.TrapezoidHeightAccessor;
import personthecat.pangaea.mixin.accessor.UniformHeightAccessor;
import personthecat.pangaea.serialization.extras.HeightPattern.HeightParams.NumberList;
import personthecat.pangaea.serialization.extras.HeightPattern.HeightParams.NumberMatrix;
import personthecat.pangaea.serialization.extras.HeightPattern.HeightParams.NumberOnly;
import personthecat.pangaea.world.density.DensityCutoff;
import personthecat.pangaea.world.provider.AnchorRangeColumnProvider;
import personthecat.pangaea.world.provider.ColumnProvider;
import personthecat.pangaea.world.provider.ConstantColumnProvider;
import personthecat.pangaea.world.provider.DynamicColumnProvider;
import personthecat.pangaea.world.provider.ExactColumnProvider;
import personthecat.pangaea.world.provider.SeaLevelVerticalAnchor;
import personthecat.pangaea.world.provider.SurfaceVerticalAnchor;

import java.util.List;
import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleAny;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.pangaea.world.density.DensityCutoff.DEFAULT_HARSHNESS;

public sealed interface HeightPattern {
    Codec<HeightPattern> CODEC =
        CodecUtils.<HeightPattern>simpleAny(
                ParamsOnly.CODEC, OffsetMap.CODEC, OffsetMapList.CODEC, MapWithRange.CODEC, OffsetNameOnly.CODEC)
            .withEncoder(HeightPattern::codec);
    Codec<HeightProvider> HEIGHT =
        CODEC.flatComapMap(HeightPattern::toHeightProvider, HeightPattern::fromHeightProvider);
    Codec<ColumnProvider> COLUMN =
        CODEC.flatComapMap(HeightPattern::toColumnProvider, HeightPattern::fromColumnProvider);

    HeightProvider toHeightProvider();
    ColumnProvider toColumnProvider();
    Codec<? extends HeightPattern> codec();

    static boolean matches(HeightProvider p) {
        return switch (p) {
            case ConstantHeight c -> isKnownAnchor(c.getValue());
            case TrapezoidHeightAccessor t -> isKnownAnchor(t.getMinInclusive()) && isKnownAnchor(t.getMaxInclusive());
            case UniformHeightAccessor u -> isKnownAnchor(u.getMinInclusive()) && isKnownAnchor(u.getMaxInclusive());
            default -> false;
        };
    }

    static boolean matches(ColumnProvider p) {
        return switch (p) {
            case ConstantColumnProvider ignored -> true;
            case DynamicColumnProvider(var l, var u, var ignored) -> isKnownAnchor(l) && isKnownAnchor(u);
            case ExactColumnProvider(var a) -> isKnownAnchor(a);
            case AnchorRangeColumnProvider(var l, var u) -> typesMatch(l) && typesMatch(u);
            default -> false;
        };
    }

    private static boolean isKnownAnchor(VerticalAnchor a) {
        return OffsetValue.from(a).isSuccess();
    }

    private static boolean typesMatch(AnchorCutoff c) {
        return estimateType(c.min()) == estimateType(c.max());
    }

    private static @Nullable Type estimateType(VerticalAnchor a) {
        return OffsetValue.from(a).map(OffsetValue::type).mapOrElse(Function.identity(), e -> null);
    }

    static DataResult<HeightPattern> fromHeightProvider(HeightProvider height) {
        return switch (height) {
            case ConstantHeight h -> fromConstantHeight(h);
            case TrapezoidHeightAccessor h -> fromAnchors(h.getMinInclusive(), h.getMaxInclusive(), Distribution.TRAPEZOID);
            case UniformHeightAccessor h -> fromAnchors(h.getMinInclusive(), h.getMaxInclusive(), Distribution.UNIFORM);
            default -> DataResult.error(() -> "No matching pattern for height: " + height);
        };
    }

    private static DataResult<HeightPattern> fromConstantHeight(ConstantHeight height) {
        return OffsetValue.from(height.getValue()).flatMap(v -> fromTypeAndOffset(v.type, v.offset));
    }

    // case { [type]: 0 } ->       type
    // case { absolute: 1 } ->     1
    // case { above_bottom: 1 } -> { bottom: 1 }
    private static DataResult<HeightPattern> fromTypeAndOffset(Type type, int offset) {
        if (offset == 0) {
            return DataResult.success(new OffsetNameOnly(type));
        } else if (type == Type.ABSOLUTE) {
            return DataResult.success(new ParamsOnly(new NumberOnly(offset)));
        }
        return DataResult.success(new OffsetMap(new OffsetParams(type, new NumberOnly(offset))));
    }

    private static DataResult<HeightPattern> fromAnchors(VerticalAnchor lower, VerticalAnchor upper, Distribution d) {
        return OffsetValue.from(lower).flatMap(lv -> OffsetValue.from(upper).map(uv -> fromOffsetValues(lv, uv, d)));
    }

    // case TRAPEZOID range -> fromOffsetValues(range)
    // case UNIFORM range ->   { range: fromOffsetValues(range), distribution: 'UNIFORM' }
    private static HeightPattern fromOffsetValues(OffsetValue lower, OffsetValue upper, Distribution d) {
        final var pattern = fromOffsetValues(lower, upper);
        return d != Distribution.DEFAULT ? new MapWithRange(pattern, d) : pattern;
    }

    // case [ { above_bottom: 5 }, { absolute: 10 } ] ->     [ { bottom: 5 }, { absolute: 10 } ]
    // case [ { absolute: 5 }, { absolute: 10 } ] ->         [ 5, 10 ]
    // case [ { above_bottom: 5 }, { above_bottom: 10 } ] -> { bottom: [ 5, 10 ] }
    private static HeightPattern fromOffsetValues(OffsetValue lower, OffsetValue upper) {
        if (lower.type != upper.type) {
            return new OffsetMapList(List.of(
                new OffsetParams(lower.type, new NumberOnly(lower.offset)),
                new OffsetParams(upper.type, new NumberOnly(upper.offset))
            ));
        } else if (lower.type == Type.ABSOLUTE) {
            return new ParamsOnly(new NumberList(List.of(upper.offset, lower.offset)));
        }
        return new OffsetMap(new OffsetParams(lower.type, new NumberList(List.of(lower.offset, upper.offset))));
    }

    static DataResult<HeightPattern> fromColumnProvider(ColumnProvider column) {
        return switch (column) {
            case ConstantColumnProvider(var c) -> fromColumnBounds(c);
            case DynamicColumnProvider(var lower, var upper, var harshness) -> fromAnchorsWithGradient(lower, upper, harshness);
            case AnchorRangeColumnProvider(var lower, var upper) -> fromAnchorRange(upper, lower);
            case ExactColumnProvider(var anchor) -> OffsetValue.from(anchor).flatMap(v -> fromTypeAndOffset(v.type, v.offset));
            default -> DataResult.error(() -> "No matching pattern for column: " + column);
        };
    }

    private static DataResult<HeightPattern> fromColumnBounds(ColumnBounds bounds) {
        final var lower = bounds.lower();
        final var upper = bounds.upper();
        if (lower.harshness() != upper.harshness()) {
            return DataResult.success(new OffsetMapList(List.of(
                new OffsetParams(Type.ABSOLUTE, new NumberList(List.of((int) lower.min(), (int) lower.max())), lower.harshness()),
                new OffsetParams(Type.ABSOLUTE, new NumberList(List.of((int) upper.min(), (int) upper.max())), upper.harshness())
            )));
        }
        final var matrix = new NumberMatrix(List.of(
            List.of((int) lower.min(), (int) lower.max()),
            List.of((int) upper.min(), (int) upper.max())
        ));
        if (lower.harshness() == DEFAULT_HARSHNESS && upper.harshness() == DEFAULT_HARSHNESS) {
            return DataResult.success(new ParamsOnly(matrix));
        }
        return DataResult.success(new OffsetMap(new OffsetParams(Type.ABSOLUTE, matrix, lower.harshness())));
    }

    private static DataResult<HeightPattern> fromAnchorsWithGradient(VerticalAnchor lower, VerticalAnchor upper, double harshness) {
        return OffsetValue.from(lower).flatMap(lv -> OffsetValue.from(upper).map(uv -> fromOffsetValuesWithGradient(lv, uv, harshness)));
    }

    private static HeightPattern fromOffsetValuesWithGradient(OffsetValue lower, OffsetValue upper, double harshness) {
        if (lower.type == Type.ABSOLUTE && upper.type == Type.ABSOLUTE && harshness == DEFAULT_HARSHNESS) {
            return new ParamsOnly(new NumberList(List.of(lower.offset, upper.offset)));
        } else if (lower.type == upper.type) {
            return new OffsetMap(new OffsetParams(lower.type, new NumberList(List.of(lower.offset, upper.offset)), harshness));
        }
        return new OffsetMapList(List.of(
            new OffsetParams(lower.type, new NumberOnly(lower.offset), harshness),
            new OffsetParams(upper.type, new NumberOnly(upper.offset), harshness)
        ));
    }

    private static DataResult<HeightPattern> fromAnchorRange(AnchorCutoff lower, AnchorCutoff upper) {
        return OffsetBounds.from(lower).flatMap(l -> OffsetBounds.from(upper).flatMap(u -> fromPreciseOffsetBounds(l, u)));
    }

    private static DataResult<HeightPattern> fromPreciseOffsetBounds(OffsetBounds lower, OffsetBounds upper) {
        if (lower.min.type != lower.max.type || upper.min.type != upper.max.type) {
            return DataResult.error(() -> "No pattern exists for heterogeneous cutoff range ([[a,b],[a,b]]): [" + lower + "," + upper + "]");
        }
        if (allTypesMatch(lower.min, lower.max, upper.min, upper.max) && lower.harshness == upper.harshness) {
            final var matrix = new NumberMatrix(List.of(
                List.of(lower.min.offset, lower.max.offset),
                List.of(upper.min.offset, upper.max.offset)
            ));
            if (lower.min.type == Type.ABSOLUTE) {
                return DataResult.success(new ParamsOnly(matrix));
            }
            return DataResult.success(new OffsetMap(new OffsetParams(lower.min.type, matrix, lower.harshness)));
        }
        return DataResult.success(new OffsetMapList(List.of(
            new OffsetParams(lower.min.type, new NumberList(List.of(lower.min.offset, lower.max.offset)), lower.harshness),
            new OffsetParams(upper.min.type, new NumberList(List.of(upper.min.offset, upper.max.offset)), upper.harshness)
        )));
    }

    private static boolean allTypesMatch(OffsetValue... values) {
        final var t = values[0].type;
        for (int i = 1; i < values.length; i++) {
            if (t != values[i].type) {
                return false;
            }
        }
        return true;
    }

    // number | [ number, number ]
    record ParamsOnly(HeightParams params) implements HeightPattern {
        public static final Codec<ParamsOnly> CODEC =
            HeightParams.CODEC.xmap(ParamsOnly::new, ParamsOnly::params);

        @Override
        public HeightProvider toHeightProvider() {
            return switch (this.params) {
                case NumberOnly n -> ConstantHeight.of(VerticalAnchor.absolute(n.value()));
                case HeightParams p -> TrapezoidHeight.of(VerticalAnchor.absolute(p.min()), VerticalAnchor.absolute(p.max()));
            };
        }

        @Override
        public ColumnProvider toColumnProvider() {
            return switch (this.params) {
                case NumberOnly n -> new ExactColumnProvider(VerticalAnchor.absolute(n.value()));
                case NumberList l -> new ConstantColumnProvider(ColumnBounds.create(l.min(), l.max(), DEFAULT_HARSHNESS));
                case NumberMatrix m -> new ConstantColumnProvider(ColumnBounds.create(m.lower(), m.upper(), DEFAULT_HARSHNESS));
            };
        }

        @Override
        public Codec<ParamsOnly> codec() {
            return CODEC;
        }
    }

    // { [type]: HeightParams, harshness?: double }
    record OffsetMap(OffsetParams offset) implements HeightPattern {
        public static final Codec<OffsetMap> CODEC =
            OffsetParams.CODEC.xmap(OffsetMap::new, OffsetMap::offset);

        @Override
        public HeightProvider toHeightProvider() {
            return switch (this.offset) {
                case OffsetParams(var t, NumberOnly n, var ignored) ->
                    ConstantHeight.of(t.anchor(n.value));
                case OffsetParams(var t, HeightParams p, var ignored) ->
                    TrapezoidHeight.of(t.anchor(p.min()), t.anchor(p.max()));
            };
        }

        @Override
        public ColumnProvider toColumnProvider() {
            return switch (this.offset) {
                case OffsetParams(var t, NumberOnly n, var ignored) ->
                    new ExactColumnProvider(t.anchor(n.value));
                case OffsetParams(var t, NumberList l, var h) when t == Type.ABSOLUTE ->
                    // optimize to skip resolving constant values later on
                    new ConstantColumnProvider(ColumnBounds.create(l.min(), l.max(), h));
                case OffsetParams(var t, NumberList l, var h) ->
                    new DynamicColumnProvider(t.anchor(l.min()), t.anchor(l.max()), h);
                case OffsetParams(var t, NumberMatrix m, var h) ->
                    new AnchorRangeColumnProvider(m.lowerCutoff(t, h), m.upperCutoff(t, h));
            };
        }

        @Override
        public Codec<OffsetMap> codec() {
            return CODEC;
        }
    }

    // [ { [type]: HeightParams, harshness? }, { ... } ]
    record OffsetMapList(List<OffsetParams> offsets) implements HeightPattern {
        public static final Codec<OffsetMapList> CODEC =
            OffsetParams.CODEC.listOf(2, 2).xmap(OffsetMapList::new, OffsetMapList::offsets);

        @Override
        public HeightProvider toHeightProvider() {
            return this.toHeightProvider(Distribution.DEFAULT);
        }

        public HeightProvider toHeightProvider(Distribution distribution) {
            final var lower = this.offsets.getFirst();
            final var upper = this.offsets.getLast();
            return distribution.apply(
                lower.type.anchor(lower.params.min()),
                upper.type.anchor(upper.params.max())
            );
        }

        @Override
        public ColumnProvider toColumnProvider() {
            final var lower = this.offsets.getFirst();
            final var upper = this.offsets.getLast();
            if (lower.params instanceof NumberOnly l && upper.params instanceof NumberOnly u) {
                return new DynamicColumnProvider(
                    lower.type.anchor(l.value), upper.type.anchor(u.value), lower.harshness);
            } else if (lower.type == Type.ABSOLUTE && upper.type == Type.ABSOLUTE) {
                // optimize to skip resolving constant values later on
                return new ConstantColumnProvider(new ColumnBounds(
                    new DensityCutoff(lower.params.min(), lower.params.max(), lower.harshness),
                    new DensityCutoff(upper.params.min(), upper.params.max(), upper.harshness)
                ));
            }
            return new AnchorRangeColumnProvider(
                new AnchorCutoff(lower.type.anchor(lower.params.min()), lower.type.anchor(lower.params.max()), lower.harshness),
                new AnchorCutoff(upper.type.anchor(upper.params.min()), upper.type.anchor(upper.params.max()), upper.harshness)
            );
        }

        @Override
        public Codec<OffsetMapList> codec() {
            return CODEC;
        }
    }

    // { range: ListOfNumbers | OffsetMapList(NumberOnly) | OffsetMap(ListOfNumbers), distribution? }
    record MapWithRange(HeightPattern range, Distribution distribution) implements HeightPattern {
        private static final Codec<HeightPattern> RANGE_PATTERN =
            CodecUtils.<HeightPattern>simpleAny(ParamsOnly.CODEC, OffsetMap.CODEC, OffsetMapList.CODEC)
                .withEncoder(HeightPattern::codec);
        public static final Codec<MapWithRange> CODEC = codecOf(
            field(RANGE_PATTERN, "range", MapWithRange::range),
            defaulted(Distribution.CODEC, "distribution", Distribution.DEFAULT, MapWithRange::distribution),
            MapWithRange::new
        ).codec().validate(MapWithRange::validate);

        private DataResult<MapWithRange> validate() {
            if (this.range instanceof ParamsOnly(NumberList ignored)
                    || this.range instanceof OffsetMapList ignored2
                    || (this.range instanceof OffsetMap m && m.offset.params instanceof NumberList)) {
                return DataResult.success(this);
            }
            return DataResult.error(() -> "Expected range for MapWithRange pattern, got: " + this.range);
        }

        @Override
        public HeightProvider toHeightProvider() {
            return switch (this.range) {
                case ParamsOnly(NumberList l) ->
                    this.distribution.apply(VerticalAnchor.absolute(l.min()), VerticalAnchor.absolute(l.max()));
                case OffsetMapList o ->
                    o.toHeightProvider(this.distribution);
                case OffsetMap(OffsetParams(var t, NumberList l, var ignored)) ->
                    this.distribution.apply(t.anchor(l.min()), t.anchor(l.max()));
                default ->
                    throw new UnsupportedOperationException("Unexpected pattern: " + this.range);
            };
        }

        @Override
        public ColumnProvider toColumnProvider() {
            return switch (this.range) {
                case ParamsOnly(NumberList l) ->
                    new ConstantColumnProvider(ColumnBounds.create(l.min(), l.max(), DEFAULT_HARSHNESS));
                case OffsetMapList o ->
                    o.toColumnProvider();
                case OffsetMap(OffsetParams(var t, NumberList l, var harshness)) ->
                    new DynamicColumnProvider(t.anchor(l.min()), t.anchor(l.max()), harshness);
                default ->
                    throw new UnsupportedOperationException("Unexpected pattern: " + this.range);
            };
        }

        @Override
        public Codec<MapWithRange> codec() {
            return CODEC;
        }
    }

    // type
    record OffsetNameOnly(Type type) implements HeightPattern {
        public static final Codec<OffsetNameOnly> CODEC =
            Type.CODEC.xmap(OffsetNameOnly::new, OffsetNameOnly::type);

        @Override
        public HeightProvider toHeightProvider() {
            return ConstantHeight.of(this.type.anchor(0));
        }

        @Override
        public ColumnProvider toColumnProvider() {
            return new ExactColumnProvider(this.type.anchor(0));
        }

        @Override
        public Codec<OffsetNameOnly> codec() {
            return CODEC;
        }
    }

    record OffsetParams(Type type, HeightParams params, double harshness) {
        public static final Codec<OffsetParams> CODEC =
            simpleAny(
                    Type.BOTTOM.codec, Type.TOP.codec, Type.ABSOLUTE.codec,
                    Type.SURFACE.codec, Type.SEA_LEVEL.codec)
                .withEncoder(o -> o.type.codec);

        public OffsetParams(Type type, HeightParams params) {
            this(type, params, DEFAULT_HARSHNESS);
        }
    }

    record OffsetValue(Type type, int offset) {
        public static DataResult<OffsetValue> from(VerticalAnchor anchor) {
            return switch (anchor) {
                case AboveBottom(int offset) -> DataResult.success(new OffsetValue(Type.BOTTOM, offset));
                case Absolute(int offset) -> DataResult.success(new OffsetValue(Type.ABSOLUTE, offset));
                case BelowTop(int offset) -> DataResult.success(new OffsetValue(Type.TOP, -offset));
                case SurfaceVerticalAnchor(int offset) -> DataResult.success(new OffsetValue(Type.SURFACE, offset));
                case SeaLevelVerticalAnchor(int offset) -> DataResult.success(new OffsetValue(Type.SEA_LEVEL, offset));
                default -> DataResult.error(() -> "No matching pattern for anchor: " + anchor);
            };
        }
    }

    record OffsetBounds(OffsetValue min, OffsetValue max, double harshness) {
        public static DataResult<OffsetBounds> from(AnchorCutoff cutoff) {
            return OffsetValue.from(cutoff.min()).flatMap(min -> OffsetValue.from(cutoff.max()).map(max ->
                new OffsetBounds(min, max, cutoff.harshness())));
        }
    }

    sealed interface HeightParams {
        Codec<HeightParams> CODEC =
            CodecUtils.<HeightParams>simpleAny(NumberOnly.CODEC, NumberList.CODEC, NumberMatrix.CODEC)
                .withEncoder(HeightParams::codec);

        int min();
        int max();
        Codec<? extends HeightParams> codec();

        record NumberOnly(int value) implements HeightParams {
            public static final Codec<NumberOnly> CODEC =
                Codec.INT.xmap(NumberOnly::new, NumberOnly::value);

            @Override
            public int min() {
                return this.value;
            }

            @Override
            public int max() {
                return this.value;
            }

            @Override
            public Codec<NumberOnly> codec() {
                return CODEC;
            }
        }

        record NumberList(List<Integer> values) implements HeightParams {
            public static final Codec<NumberList> CODEC =
                Codec.INT.listOf(2, 2).xmap(NumberList::new, NumberList::values);

            public NumberList {
                values = sortValues(values);
            }

            @Override
            public int min() {
                return this.values.getFirst();
            }

            @Override
            public int max() {
                return this.values.getLast();
            }

            @Override
            public Codec<NumberList> codec() {
                return CODEC;
            }
        }

        record NumberMatrix(List<List<Integer>> values) implements HeightParams {
            public static final Codec<NumberMatrix> CODEC =
                Codec.INT.listOf(2, 2).listOf(2, 2).xmap(NumberMatrix::new, NumberMatrix::values)
                    .validate(NumberMatrix::validate);

            public NumberMatrix {
                values = values.stream().map(HeightParams::sortValues).toList();
            }

            private DataResult<NumberMatrix> validate() {
                if (this.values.getFirst().getLast() > this.values.getLast().getFirst()) {
                    return DataResult.error(() -> "lower > upper: " + this.values);
                }
                return DataResult.success(this);
            }

            public Range lower() {
                return range(this.values.getFirst());
            }

            public Range upper() {
                return range(this.values.getLast());
            }

            private static Range range(List<Integer> values) {
                return Range.of(values.getFirst(), values.getLast());
            }

            public AnchorCutoff lowerCutoff(Type type, double harshness) {
                return cutoff(this.values.getFirst(), type, harshness);
            }

            public AnchorCutoff upperCutoff(Type type, double harshness) {
                return cutoff(this.values.getLast(), type, harshness);
            }

            private static AnchorCutoff cutoff(List<Integer> values, Type type, double harshness) {
                return new AnchorCutoff(type.anchor(values.getFirst()), type.anchor(values.getLast()), harshness);
            }

            @Override
            public int min() {
                return this.values.getFirst().getFirst();
            }

            @Override
            public int max() {
                return this.values.getLast().getLast();
            }

            @Override
            public Codec<NumberMatrix> codec() {
                return CODEC;
            }
        }

        private static List<Integer> sortValues(List<Integer> values) {
            return values.stream().sorted().toList();
        }
    }

    enum Type {
        BOTTOM,
        TOP,
        ABSOLUTE,
        SURFACE,
        SEA_LEVEL;

        public static final Codec<Type> CODEC = ofEnum(Type.class);

        public final Codec<OffsetParams> codec = codecOf(
            field(HeightParams.CODEC, this.name().toLowerCase(), OffsetParams::params),
            defaulted(Codec.DOUBLE, "harshness", DEFAULT_HARSHNESS, OffsetParams::harshness),
            (params, harshness) -> new OffsetParams(this, params, harshness)
        ).codec();

        public VerticalAnchor anchor(int offset) {
            return switch (this) {
                case BOTTOM -> VerticalAnchor.aboveBottom(offset);
                case TOP -> VerticalAnchor.belowTop(-offset);
                case ABSOLUTE -> VerticalAnchor.absolute(offset);
                case SURFACE -> new SurfaceVerticalAnchor(offset);
                case SEA_LEVEL -> new SeaLevelVerticalAnchor(offset);
            };
        }
    }

    enum Distribution {
        UNIFORM,
        TRAPEZOID;

        public static final Distribution DEFAULT = TRAPEZOID;
        public static final Codec<Distribution> CODEC = ofEnum(Distribution.class);

        public HeightProvider apply(VerticalAnchor min, VerticalAnchor max) {
            return switch (this) {
                case UNIFORM -> UniformHeight.of(min, max);
                case TRAPEZOID -> TrapezoidHeight.of(min, max);
            };
        }
    }
}
