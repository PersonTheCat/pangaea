package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.VerticalAnchor.AboveBottom;
import net.minecraft.world.level.levelgen.VerticalAnchor.Absolute;
import net.minecraft.world.level.levelgen.VerticalAnchor.BelowTop;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import personthecat.catlib.data.Range;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.data.AnchorCutoff;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.mixin.accessor.TrapezoidHeightAccessor;
import personthecat.pangaea.mixin.accessor.UniformHeightAccessor;
import personthecat.pangaea.world.density.DensityCutoff;
import personthecat.pangaea.world.provider.AnchorRangeColumnProvider;
import personthecat.pangaea.world.provider.DynamicColumnProvider;
import personthecat.pangaea.world.provider.ColumnProvider;
import personthecat.pangaea.world.provider.ConstantColumnProvider;
import personthecat.pangaea.world.provider.ExactColumnProvider;
import personthecat.pangaea.world.provider.SeaLevelVerticalAnchor;
import personthecat.pangaea.world.provider.SurfaceVerticalAnchor;

import java.util.List;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleAny;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleEither;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

// todo:
//  * refactor to handle by shape,
//  * optimize range of single type,
//  * support 0-offset by Type as enum
public final class PatternHeightCodecs {
    private static final Codec<HeightProvider> HEIGHT_CODEC =
        HeightInfo.CODEC.flatXmap(HeightInfo::toHeightProvider, HeightInfo::fromHeightProvider);
    private static final Codec<ColumnProvider> COLUMN_CODEC =
        HeightInfo.CODEC.flatXmap(HeightInfo::toColumnProvider, HeightInfo::fromColumnProvider);

    private PatternHeightCodecs() {}

    public static Codec<HeightProvider> wrapHeight(Codec<HeightProvider> codec) {
        return simpleEither(codec, HEIGHT_CODEC).withEncoder(h ->
            Cfg.encodePatternHeightProvider() && HeightInfo.fromHeightProvider(h).isSuccess() ? HEIGHT_CODEC : codec);
    }

    public static Codec<ColumnProvider> wrapColumn(Codec<ColumnProvider> codec) {
        return simpleEither(codec, COLUMN_CODEC).withEncoder(p ->
            Cfg.encodePatternHeightProvider() && HeightInfo.fromColumnProvider(p).isSuccess() ? COLUMN_CODEC : codec);
    }

    private record HeightInfo(HeightRange range, Distribution distribution) {
        private static final Distribution DEFAULT_DISTRIBUTION = Distribution.TRAPEZOID;
        private static final Codec<HeightInfo> OBJECT_CODEC = codecOf(
            field(HeightRange.CODEC, "range", i -> i.range),
            defaulted(ofEnum(Distribution.class), "distribution", DEFAULT_DISTRIBUTION, i -> i.distribution),
            HeightInfo::new
        ).codec();
        private static final Codec<HeightInfo> RANGE_CODEC =
            HeightRange.CODEC.xmap(r -> new HeightInfo(r, DEFAULT_DISTRIBUTION), i -> i.range);
        private static final Codec<HeightInfo> CODEC =
            simpleEither(RANGE_CODEC, OBJECT_CODEC)
                .withEncoder(i -> i.distribution == DEFAULT_DISTRIBUTION ? RANGE_CODEC : OBJECT_CODEC);

        private DataResult<HeightProvider> toHeightProvider() {
            return DataResult.success(this.range.toHeightProvider(this.distribution));
        }

        private DataResult<ColumnProvider> toColumnProvider() {
            return DataResult.success(this.range.toColumnProvider());
        }

        private static DataResult<HeightInfo> fromHeightProvider(HeightProvider provider) {
            if (provider instanceof UniformHeightAccessor u) {
                return HeightRange.fromVerticalAnchors(u.getMinInclusive(), u.getMaxInclusive())
                    .map(r -> new HeightInfo(r, Distribution.UNIFORM));
            } else if (provider instanceof TrapezoidHeightAccessor t) {
                return HeightRange.fromVerticalAnchors(t.getMinInclusive(), t.getMaxInclusive())
                    .map(r -> new HeightInfo(r, Distribution.TRAPEZOID));
            } else if (provider instanceof ConstantHeight c) {
                return NamedOffset.fromVerticalAnchor(c.getValue())
                    .map(o -> new HeightInfo(new HeightRange(o, o), DEFAULT_DISTRIBUTION));
            }
            return DataResult.error(() -> "Unsupported height provider: " + provider);
        }

        private static DataResult<HeightInfo> fromColumnProvider(ColumnProvider provider) {
            if (provider instanceof ConstantColumnProvider c) {
                return HeightRange.fromColumn(c.column())
                    .map(r -> new HeightInfo(r, DEFAULT_DISTRIBUTION));
            } else if (provider instanceof DynamicColumnProvider d) {
                return HeightRange.fromVerticalAnchors(d.min(), d.max())
                    .map(r -> new HeightInfo(r, DEFAULT_DISTRIBUTION));
            }
            return DataResult.error(() -> "Unsupported column provider: " + provider);
        }
    }

    private record HeightRange(NamedOffset lower, NamedOffset upper) {
        private static final Codec<HeightRange> CODEC =
            easyList(NamedOffset.CODEC).validate(HeightRange::validateList)
                .xmap(HeightRange::fromList, HeightRange::toList);

        private static DataResult<HeightRange> fromColumn(ColumnBounds column) {
            return DataResult.success(new HeightRange(
                new NamedOffset(Type.ABSOLUTE, column.lowerRange()),
                new NamedOffset(Type.ABSOLUTE, column.upperRange())
            ));
        }

        private static DataResult<HeightRange> fromVerticalAnchors(VerticalAnchor lower, VerticalAnchor upper) {
            return NamedOffset.fromVerticalAnchor(lower).flatMap(l ->
                NamedOffset.fromVerticalAnchor(upper).map(u -> new HeightRange(l, u)));
        }

        private static DataResult<List<NamedOffset>> validateList(List<NamedOffset> list) {
            if (list.size() != 1 && list.size() != 2) {
                return DataResult.error(() -> "Must have 1 or 2 entries: " + list);
            } else if (list.size() == 2) {
                for (final NamedOffset offset : list) {
                    if (!offset.isConstant()) {
                        return DataResult.error(() -> "Offset in list must be constant: " + offset);
                    }
                }
            }
            return DataResult.success(list);
        }

        private static HeightRange fromList(List<NamedOffset> list) {
            return new HeightRange(list.getFirst(), list.getLast());
        }

        private List<NamedOffset> toList() {
            if (this.lower.equals(this.upper)) {
                return List.of(this.lower);
            } else if (this.lower.type == this.upper.type) {
                return List.of(new NamedOffset(this.lower.type, Range.of(this.lower.y.min(), this.upper.y.max())));
            }
            return List.of(this.lower, this.upper);
        }

        private HeightProvider toHeightProvider(Distribution distribution) {
            if (this.isConstant()) {
                return ConstantHeight.of(this.lowerBound());
            }
            return switch (distribution) {
                case UNIFORM -> UniformHeight.of(this.lowerBound(), this.upperBound());
                case TRAPEZOID -> TrapezoidHeight.of(this.lowerBound(), this.upperBound());
            };
        }

        private ColumnProvider toColumnProvider() {
            if (this.lower == this.upper || (this.lower.isConstant() && this.upper.isConstant())) {
                // 1 range -> transition is automatic at bottom and top (missing: harshness pattern validation)
                if (this.isAbsolute()) {
                    // optimize: skip resolving constant values later on
                    return new ConstantColumnProvider(
                        ColumnBounds.create(this.lower.y.min(), this.upper.y.max(), this.upper.harshness)
                    );
                } else if (this.isConstant()) {
                    // Exactly one height -> range of x...x+1 (can't determine if single value given)
                    return new ExactColumnProvider(this.lowerBound());
                }
                return new DynamicColumnProvider(
                    this.lower.minBound(), this.upper.maxBound(), this.upper.harshness
                );
            }
            // 2 ranges -> lower transition, upper transition
            if (this.isAbsolute()) {
                // optimize: skip resolving constant values later on
                return new ConstantColumnProvider(
                    new ColumnBounds(this.lower.absoluteCutoff(), this.upper.absoluteCutoff())
                );
            }
            return new AnchorRangeColumnProvider(this.lower.cutoff(), this.upper.cutoff());
        }

        private boolean isConstant() {
            return this.lower.type == this.upper.type && this.lower.y.min() == this.upper.y.max();
        }

        private boolean isAbsolute() {
            return this.lower.type == Type.ABSOLUTE && this.upper.type == Type.ABSOLUTE;
        }

        private VerticalAnchor upperBound() {
            return this.upper.maxBound();
        }

        private VerticalAnchor lowerBound() {
            return this.lower.minBound();
        }
    }

    private record NamedOffset(Type type, Range y, double harshness) {
        private static final Codec<NamedOffset> RANGE_CODEC =
            Range.CODEC.xmap(NamedOffset::new, o -> o.y);
        private static final Codec<NamedOffset> CODEC =
            simpleAny(RANGE_CODEC, Type.decoders())
                .withEncoder(o -> o.canBeRange() ? RANGE_CODEC : o.type.codec);

        private static DataResult<NamedOffset> fromVerticalAnchor(VerticalAnchor anchor) {
            if (anchor instanceof BelowTop top) {
                return DataResult.success(new NamedOffset(Type.TOP, Range.of(-top.offset())));
            } else if (anchor instanceof AboveBottom bottom) {
                return DataResult.success(new NamedOffset(Type.BOTTOM, Range.of(bottom.offset())));
            } else if (anchor instanceof Absolute absolute) {
                return DataResult.success(new NamedOffset(Type.ABSOLUTE, Range.of(absolute.y())));
            } else if (anchor instanceof SurfaceVerticalAnchor surface) {
                return DataResult.success(new NamedOffset(Type.SURFACE, Range.of(surface.offset())));
            } else if (anchor instanceof SeaLevelVerticalAnchor seaLevel) {
                return DataResult.success(new NamedOffset(Type.SEA_LEVEL, Range.of(seaLevel.offset())));
            }
            return DataResult.error(() -> "Unsupported vertical anchor: " + anchor);
        }

        private NamedOffset(Range y) {
            this(Type.ABSOLUTE, y, DensityCutoff.DEFAULT_HARSHNESS);
        }

        private NamedOffset(Type type, Range y) {
            this(type, y, DensityCutoff.DEFAULT_HARSHNESS);
        }

        private AnchorCutoff cutoff() {
            return new AnchorCutoff(this.minBound(), this.maxBound(), this.harshness);
        }

        private DensityCutoff absoluteCutoff() {
            return new DensityCutoff(this.y.min(), this.y.max(), this.harshness);
        }

        private boolean canBeRange() {
            return this.type == Type.ABSOLUTE && this.harshness == DensityCutoff.DEFAULT_HARSHNESS;
        }

        private boolean isConstant() {
            return this.y.diff() == 0;
        }

        private VerticalAnchor minBound() {
            return bound(this.type, this.y.min());
        }

        private VerticalAnchor maxBound() {
            return bound(this.type, this.y.max());
        }

        private static VerticalAnchor bound(Type type, int y) {
            return switch (type) {
                case BOTTOM -> VerticalAnchor.aboveBottom(y);
                case TOP -> VerticalAnchor.belowTop(-y);
                case ABSOLUTE -> VerticalAnchor.absolute(y);
                case SURFACE -> new SurfaceVerticalAnchor(y);
                case SEA_LEVEL -> new SeaLevelVerticalAnchor(y);
            };
        }
    }

    private enum Type {
        BOTTOM,
        TOP,
        ABSOLUTE,
        SURFACE,
        SEA_LEVEL;

        final Codec<NamedOffset> codec = codecOf(
            field(Range.CODEC, this.key(), NamedOffset::y),
            defaulted(Codec.DOUBLE, "harshness", DensityCutoff.DEFAULT_HARSHNESS, NamedOffset::harshness),
            (range, harshness) -> new NamedOffset(this, range, harshness)
        ).codec();

        String key() {
            return this.name().toLowerCase();
        }

        @SuppressWarnings("unchecked")
        static Decoder<NamedOffset>[] decoders() {
            return Stream.of(values()).map(t -> t.codec).toArray(Decoder[]::new);
        }
    }

    private enum Distribution {
        UNIFORM,
        TRAPEZOID
    }
}
