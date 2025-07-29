package personthecat.pangaea.serialization.extras;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.VerticalAnchor.Absolute;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import personthecat.catlib.serialization.codec.CodecUtils;
import personthecat.pangaea.data.AnchorCutoff;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.mixin.accessor.TrapezoidHeightAccessor;
import personthecat.pangaea.mixin.accessor.UniformHeightAccessor;
import personthecat.pangaea.serialization.codec.PatternCodec;
import personthecat.pangaea.serialization.extras.HeightParams.AnchorList;
import personthecat.pangaea.serialization.extras.HeightParams.AnchorMatrix;
import personthecat.pangaea.serialization.extras.HeightParams.AnchorOnly;
import personthecat.pangaea.world.density.DensityCutoff;
import personthecat.pangaea.world.provider.AnchorRangeColumnProvider;
import personthecat.pangaea.world.provider.ColumnProvider;
import personthecat.pangaea.world.provider.ConstantColumnProvider;
import personthecat.pangaea.world.provider.DynamicColumnProvider;
import personthecat.pangaea.world.provider.ExactColumnProvider;

import java.util.List;
import java.util.stream.Stream;

import static net.minecraft.world.level.levelgen.VerticalAnchor.absolute;
import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.pangaea.world.density.DensityCutoff.DEFAULT_HARSHNESS;

public sealed interface HeightPattern {
    HeightProvider toHeightProvider(AnchorDistribution d);
    ColumnProvider toColumnProvider();
    Info<? extends HeightPattern> info();

    default HeightProvider toHeightProvider() {
        return this.toHeightProvider(AnchorDistribution.DEFAULT);
    }

    interface Info<P extends HeightPattern> {
        DataResult<P> fromHeightProvider(HeightProvider p);
        DataResult<P> fromColumnProvider(ColumnProvider p);
        Codec<P> codec();

        default PatternCodec.Pattern<HeightProvider> heightPattern() {
            return PatternCodec.Pattern.of(this.heightCodec(), this::matchesHeight);
        }

        default PatternCodec.Pattern<ColumnProvider> columnPattern() {
            return PatternCodec.Pattern.of(this.columnCodec(), this::matchesColumn);
        }

        default boolean matchesHeight(HeightProvider p) {
            return this.fromHeightProvider(p).isSuccess();
        }

        default boolean matchesColumn(ColumnProvider p) {
            return this.fromColumnProvider(p).isSuccess();
        }

        default Codec<HeightProvider> heightCodec() {
            return this.codec().flatComapMap(HeightPattern::toHeightProvider, this::fromHeightProvider);
        }

        default Codec<ColumnProvider> columnCodec() {
            return this.codec().flatComapMap(HeightPattern::toColumnProvider, this::fromColumnProvider);
        }

        default DataResult<P> mismatch() {
            return DataResult.error(() -> "Pattern mismatch");
        }

        default boolean harshnessIsDefaulted(ColumnBounds b) {
            return this.harshnessIsDefaulted(b.lower().harshness()) && this.harshnessIsDefaulted(b.upper().harshness());
        }

        default boolean harshnessIsDefaulted(AnchorCutoff l, AnchorCutoff u) {
            return this.harshnessIsDefaulted(l.harshness()) && this.harshnessIsDefaulted(u.harshness());
        }

        default boolean harshnessIsDefaulted(double harshness) {
            return harshness == DEFAULT_HARSHNESS;
        }

        default AnchorMatrix matrix(ColumnBounds b) {
            final var lower = b.lower();
            final var upper = b.upper();
            return new AnchorMatrix(abs(lower.min()), abs(lower.max()), abs(upper.min()), abs(upper.max()));
        }

        default AnchorMatrix matrix(OffsetBounds l, OffsetBounds u) {
            return new AnchorMatrix(l.min().anchor(), l.max().anchor(), u.min().anchor(), u.max().anchor());
        }

        private static VerticalAnchor abs(double d) {
            return absolute((int) d);
        }
    }

    // Value | [ Value, Value ] | [[ Value, Value ], [ Value, Value ]]
    record ParamsOnly(HeightParams params) implements HeightPattern {
        public static final Codec<ParamsOnly> CODEC =
            HeightParams.CODEC.xmap(ParamsOnly::new, ParamsOnly::params);

        public static final Info<ParamsOnly> INFO = new Info<>() {
            @Override
            public DataResult<ParamsOnly> fromHeightProvider(HeightProvider p) {
                return switch (p) {
                    case ConstantHeight h ->
                        DataResult.success(new ParamsOnly(new AnchorOnly(h.getValue())));
                    case TrapezoidHeightAccessor h ->
                        OffsetValue.flatRange(h.getMinInclusive(), h.getMaxInclusive(), this::fromRange);
                    default ->
                        this.mismatch();
                };
            }

            private DataResult<ParamsOnly> fromRange(OffsetValue min, OffsetValue max) {
                return (min.isAbsolute() && max.isAbsolute()) || min.type() != max.type()
                    ? DataResult.success(new ParamsOnly(new AnchorList(min.anchor(), max.anchor())))
                    : this.mismatch();
            }

            @Override
            public DataResult<ParamsOnly> fromColumnProvider(ColumnProvider p) {
                return switch (p) {
                    case ExactColumnProvider(var a) ->
                        DataResult.success(new ParamsOnly(new AnchorOnly(a)));
                    case ConstantColumnProvider(var b) when this.harshnessIsDefaulted(b) ->
                        DataResult.success(new ParamsOnly(this.matrix(b)));
                    case DynamicColumnProvider(var min, var max, var h) when this.harshnessIsDefaulted(h) ->
                        DataResult.success(new ParamsOnly(new AnchorList(min, max)));
                    case AnchorRangeColumnProvider(var l, var u) when this.harshnessIsDefaulted(l, u) ->
                        OffsetBounds.flatRange(l, u, this::fromRange);
                    default ->
                        this.mismatch();
                };
            }

            private DataResult<ParamsOnly> fromRange(OffsetBounds l, OffsetBounds u) {
                return l.min().type() != l.max().type() || u.min().type() != u.max().type()
                    ? DataResult.success(new ParamsOnly(this.matrix(l, u)))
                    : this.mismatch();
            }

            @Override
            public Codec<ParamsOnly> codec() {
                return CODEC;
            }
        };

        @Override
        public HeightProvider toHeightProvider(AnchorDistribution d) {
            return switch (this.params) {
                case AnchorOnly(var v) -> ConstantHeight.of(v);
                case AnchorList(var l, var u) -> d.apply(l, u);
                case AnchorMatrix(var l, var ignored1, var ignored2, var u) -> d.apply(l, u);
            };
        }

        @Override
        public ColumnProvider toColumnProvider() {
            return switch (this.params) {
                case AnchorOnly(var a) -> new ExactColumnProvider(a);
                case AnchorList l when l.isAbsolute() -> new ConstantColumnProvider(ColumnBounds.create(l.min(), l.max(), DEFAULT_HARSHNESS));
                case AnchorMatrix m when m.isAbsolute() -> new ConstantColumnProvider(ColumnBounds.create(m.lower(), m.upper(), DEFAULT_HARSHNESS));
                case AnchorList(var l, var u) -> new DynamicColumnProvider(l, u, DEFAULT_HARSHNESS);
                case AnchorMatrix m -> new AnchorRangeColumnProvider(m.lowerCutoff(), m.upperCutoff());
            };
        }

        @Override
        public Info<? extends HeightPattern> info() {
            return INFO;
        }
    }

    // { [type]: HeightParams, harshness? }
    record OffsetParams(AnchorType type, HeightParams params, double harshness) implements HeightPattern {
        public static final Codec<OffsetParams> CODEC =
            AnchorType.dispatchCodec(OffsetParams::createCodec, OffsetParams::type)
                .validate(OffsetParams::validate);

        public static final Info<OffsetParams> INFO = new Info<>() {
            @Override
            public DataResult<OffsetParams> fromHeightProvider(HeightProvider p) {
                if (p instanceof TrapezoidHeightAccessor h) {
                    return OffsetValue.flatRange(h.getMinInclusive(), h.getMaxInclusive(), this::fromRange);
                }
                return this.mismatch();
            }

            private DataResult<OffsetParams> fromRange(OffsetValue min, OffsetValue max) {
                return this.fromRange(min, max, DEFAULT_HARSHNESS);
            }

            @Override
            public DataResult<OffsetParams> fromColumnProvider(ColumnProvider p) {
                return switch (p) {
                    case ConstantColumnProvider(var b) when !this.harshnessIsDefaulted(b) ->
                        DataResult.success(new OffsetParams(AnchorType.ABSOLUTE, this.matrix(b)));
                    case DynamicColumnProvider(var min, var max, var h) when !this.harshnessIsDefaulted(h) ->
                        OffsetValue.flatRange(min, max, (l, u) -> this.fromRange(l, u, h));
                    case AnchorRangeColumnProvider(var l, var u) when !this.harshnessIsDefaulted(l, u) ->
                        OffsetBounds.flatRange(l, u, this::fromRange);
                    default ->
                        this.mismatch();
                };
            }

            private DataResult<OffsetParams> fromRange(OffsetValue min, OffsetValue max, double harshness) {
                return min.type() == max.type()
                    ? DataResult.success(new OffsetParams(min.type(), new AnchorList(min.absolute(), max.absolute()), harshness))
                    : this.mismatch();
            }

            private DataResult<OffsetParams> fromRange(OffsetBounds l, OffsetBounds u) {
                return Stream.of(l.min(), l.max(), u.min(), u.max()).allMatch(v -> v.type() == l.min().type())
                    ? DataResult.success(new OffsetParams(l.min().type(), this.matrix(l, u)))
                    : this.mismatch();
            }

            @Override
            public Codec<OffsetParams> codec() {
                return CODEC;
            }
        };

        public OffsetParams(AnchorType type, HeightParams params) {
            this(type, params, DEFAULT_HARSHNESS);
        }

        private static Codec<OffsetParams> createCodec(AnchorType type) {
            return codecOf(
                field(HeightParams.CODEC, type.fieldName(), OffsetParams::params),
                defaulted(Codec.DOUBLE, "harshness", DEFAULT_HARSHNESS, OffsetParams::harshness),
                (params, harshness) -> new OffsetParams(type, params, harshness)
            ).codec();
        }

        private DataResult<OffsetParams> validate() {
            if (!this.params.isAbsolute()) {
                return DataResult.error(() -> "Cannot offset another offset: " + this.params);
            }
            return DataResult.success(this);
        }

        @Override
        public HeightProvider toHeightProvider(AnchorDistribution d) {
            return switch (this.params) {
                case AnchorOnly(Absolute(var v)) -> ConstantHeight.of(this.type.at(v));
                case AnchorList(Absolute(var l), Absolute(var u)) -> d.apply(this.type.at(l), this.type.at(u));
                case AnchorMatrix(Absolute(var l), var ignored1, var ignored2, Absolute(var u)) -> d.apply(this.type.at(l), this.type.at(u));
                default -> throw new UnsupportedOperationException("Non-numeric params: " + this.params);
            };
        }

        @Override
        public ColumnProvider toColumnProvider() {
            return switch (this.params) {
                // optimize to skip resolving constant values later on
                case AnchorList l when this.type == AnchorType.ABSOLUTE ->
                    new ConstantColumnProvider(ColumnBounds.create(l.min(), l.max(), this.harshness));
                case AnchorMatrix m when this.type == AnchorType.ABSOLUTE ->
                    new ConstantColumnProvider(ColumnBounds.create(m.lower(), m.upper(), this.harshness));
                case AnchorOnly(Absolute(var o)) ->
                    new ExactColumnProvider(this.type.at(o));
                case AnchorList(Absolute(var l), Absolute(var u)) ->
                    new DynamicColumnProvider(this.type.at(l), this.type.at(u), this.harshness);
                case AnchorMatrix m ->
                    new AnchorRangeColumnProvider(m.lowerCutoff(this.type, this.harshness), m.upperCutoff(this.type, this.harshness));
                default ->
                    throw new UnsupportedOperationException("Non-numeric params: " + this.params);
            };
        }

        public DensityCutoff densityCutoff() {
            return new DensityCutoff(this.params.min(), this.params.max(), this.harshness);
        }

        public AnchorCutoff anchorCutoff() {
            return new AnchorCutoff(this.type.at(this.params.min()), this.type.at(this.params.max()), this.harshness);
        }

        @Override
        public Info<? extends HeightPattern> info() {
            return INFO;
        }
    }

    // [{ [type]: HeightParams, harshness? }, {...}]
    record OffsetParamsList(OffsetParams lower, OffsetParams upper) implements HeightPattern {
        public static final Codec<OffsetParamsList> CODEC =
            OffsetParams.CODEC.listOf(2, 2).xmap(OffsetParamsList::fromList, OffsetParamsList::toList)
                .validate(OffsetParamsList::validate);

        public static final Info<OffsetParamsList> INFO = new Info<>() {
            @Override
            public DataResult<OffsetParamsList> fromHeightProvider(HeightProvider p) {
                return this.mismatch();
            }

            @Override
            public DataResult<OffsetParamsList> fromColumnProvider(ColumnProvider p) {
                return switch (p) {
                    case ConstantColumnProvider(var b) when b.lower().harshness() != b.upper().harshness() ->
                        DataResult.success(new OffsetParamsList(params(b.lower()), params(b.upper())));
                    case DynamicColumnProvider(var min, var max, var h) ->
                        OffsetValue.flatRange(min, max, (u, l) -> this.fromRange(u, l, h));
                    case AnchorRangeColumnProvider(var u, var l) ->
                        OffsetBounds.flatRange(u, l, this::fromRange);
                    default ->
                        this.mismatch();
                };
            }

            private static OffsetParams params(DensityCutoff c) {
                return new OffsetParams(AnchorType.ABSOLUTE, new AnchorList(Info.abs(c.min()), Info.abs(c.max())), c.harshness());
            }

            private DataResult<OffsetParamsList> fromRange(OffsetValue min, OffsetValue max, double harshness) {
                return min.type() != max.type()
                    ? DataResult.success(new OffsetParamsList(params(min, harshness), params(max, harshness)))
                    : this.mismatch();
            }

            private static OffsetParams params(OffsetValue value, double harshness) {
                return new OffsetParams(value.type(), new AnchorOnly(value.absolute()), harshness);
            }

            private DataResult<OffsetParamsList> fromRange(OffsetBounds l, OffsetBounds u) {
                return l.min().type() == l.max().type() && u.min().type() == u.max().type()
                    ? DataResult.success(new OffsetParamsList(params(l), params(u)))
                    : this.mismatch();
            }

            private static OffsetParams params(OffsetBounds bounds) {
                return new OffsetParams(bounds.min().type(), new AnchorList(bounds.min().absolute(), bounds.max().absolute()), bounds.harshness());
            }

            @Override
            public Codec<OffsetParamsList> codec() {
                return CODEC;
            }
        };

        public static OffsetParamsList fromList(List<OffsetParams> list) {
            return new OffsetParamsList(list.getFirst(), list.getLast());
        }

        // Is implicitly number-only because of OffsetParams#CODEC
        private DataResult<OffsetParamsList> validate() {
            if (this.lower.params instanceof AnchorMatrix || this.upper.params instanceof AnchorMatrix) {
                return DataResult.error(() -> "Too many params for map-list pattern: " + this.toList());
            }
            return DataResult.success(this);
        }

        public List<OffsetParams> toList() {
            return List.of(this.lower, this.upper);
        }

        @Override
        public HeightProvider toHeightProvider(AnchorDistribution d) {
            return d.apply(
                this.lower.type.at(this.lower.params.min()),
                this.upper.type.at(this.upper.params.max())
            );
        }

        @Override
        public ColumnProvider toColumnProvider() {
            final var lower = this.lower;
            final var upper = this.upper;
            if (lower.params instanceof AnchorOnly l && upper.params instanceof AnchorOnly u) {
                return new DynamicColumnProvider(lower.type.at(l.min()), upper.type.at(u.max()), lower.harshness);
            } else if (lower.type == AnchorType.ABSOLUTE && upper.type == AnchorType.ABSOLUTE) {
                // optimize to skip resolving constant values later on
                return new ConstantColumnProvider(new ColumnBounds(lower.densityCutoff(), upper.densityCutoff()));
            }
            return new AnchorRangeColumnProvider(lower.anchorCutoff(), upper.anchorCutoff());
        }

        @Override
        public Info<? extends HeightPattern> info() {
            return INFO;
        }
    }

    // { range: ListOfNumbers | OffsetMapList(NumberOnly) | OffsetMap(ListOfNumbers), distribution? }
    record MapWithRange(HeightPattern range, AnchorDistribution distribution) implements HeightPattern {
        private static final Codec<HeightPattern> RANGE_PATTERN =
            CodecUtils.<HeightPattern>simpleAny(ParamsOnly.CODEC, OffsetParams.CODEC, OffsetParamsList.CODEC)
                .withEncoder(p -> p.info().codec());
        public static final Codec<MapWithRange> CODEC = codecOf(
            field(RANGE_PATTERN, "range", MapWithRange::range),
            defaulted(AnchorDistribution.CODE, "distribution", AnchorDistribution.DEFAULT, MapWithRange::distribution),
            MapWithRange::new
        ).codec().validate(MapWithRange::validate);

        public static final Info<MapWithRange> INFO = new Info<>() {
            @Override
            public DataResult<MapWithRange> fromHeightProvider(HeightProvider p) {
                if (p instanceof UniformHeightAccessor h) {
                    return OffsetValue.range(h.getMinInclusive(), h.getMaxInclusive(), this::fromRange);
                }
                return this.mismatch();
            }

            private MapWithRange fromRange(OffsetValue min, OffsetValue max) {
                return min.type() != max.type() || min.type() == AnchorType.ABSOLUTE
                    ? new MapWithRange(new ParamsOnly(new AnchorList(min.anchor(), max.anchor())), AnchorDistribution.UNIFORM)
                    : new MapWithRange(new OffsetParams(min.type(), new AnchorList(min.absolute(), max.absolute())), AnchorDistribution.UNIFORM);
            }

            @Override
            public DataResult<MapWithRange> fromColumnProvider(ColumnProvider p) {
                return this.mismatch();
            }

            @Override
            public Codec<MapWithRange> codec() {
                return CODEC;
            }
        };

        private DataResult<MapWithRange> validate() {
            if (this.range instanceof ParamsOnly
                    || this.range instanceof OffsetParamsList
                    || (this.range instanceof OffsetParams p && p.params.isAbsolute())) {
                return DataResult.success(this);
            }
            return DataResult.error(() -> "Expected range for map-with-range pattern, got: " + this.range);
        }

        public HeightProvider toHeightProvider() {
            return this.toHeightProvider(this.distribution);
        }

        @Override
        public HeightProvider toHeightProvider(AnchorDistribution d) {
            return this.range.toHeightProvider(d);
        }

        @Override
        public ColumnProvider toColumnProvider() {
            return this.range.toColumnProvider();
        }

        @Override
        public Info<? extends HeightPattern> info() {
            return INFO;
        }
    }
}
