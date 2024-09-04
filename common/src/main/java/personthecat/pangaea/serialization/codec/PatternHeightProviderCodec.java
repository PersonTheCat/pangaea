package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import personthecat.catlib.data.Range;
import personthecat.catlib.serialization.codec.DefaultTypeCodec;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.mixin.TrapezoidHeightAccessor;
import personthecat.pangaea.mixin.UniformHeightAccessor;

import java.util.List;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleAny;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleEither;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public final class PatternHeightProviderCodec {
    private static final Codec<HeightProvider> CODEC =
        HeightInfo.CODEC.flatXmap(HeightInfo::toHeightProvider, HeightInfo::fromHeightProvider);

    private PatternHeightProviderCodec() {}

    public static Codec<HeightProvider> wrap(Codec<HeightProvider> codec) {
        return new DefaultTypeCodec<>(codec, CODEC,
            (h, ops) -> Cfg.encodePatternHeightProvider() && HeightInfo.fromHeightProvider(h).isSuccess());
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
    }

    private record HeightRange(NamedOffset lower, NamedOffset upper) {
        private static final Codec<HeightRange> CODEC =
            easyList(NamedOffset.CODEC).validate(HeightRange::validateList)
                .xmap(HeightRange::fromList, HeightRange::toList);

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
                return List.of(new NamedOffset(this.lower.type, Range.of(this.lower.y.min, this.upper.y.max)));
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

        private boolean isConstant() {
            return this.lower.type == this.upper.type && this.lower.y.min == this.upper.y.max;
        }

        private VerticalAnchor upperBound() {
            return bound(this.upper.type, this.upper.y.max);
        }

        private VerticalAnchor lowerBound() {
            return bound(this.lower.type, this.lower.y.min);
        }

        private static VerticalAnchor bound(Type type, int y) {
            return switch (type) {
                case BOTTOM -> VerticalAnchor.aboveBottom(y);
                case TOP -> VerticalAnchor.belowTop(-y);
                case ABSOLUTE -> VerticalAnchor.absolute(y);
            };
        }
    }

    private record NamedOffset(Type type, Range y) {
        private static final Codec<NamedOffset> RANGE_CODEC =
            Range.CODEC.xmap(y -> new NamedOffset(Type.ABSOLUTE, y), o -> o.y);
        private static final Codec<NamedOffset> CODEC =
            simpleAny(RANGE_CODEC, Type.TOP.codec, Type.BOTTOM.codec, Type.ABSOLUTE.codec)
                .withEncoder(o -> o.type == Type.ABSOLUTE ? RANGE_CODEC : o.type.codec);

        private static DataResult<NamedOffset> fromVerticalAnchor(VerticalAnchor anchor) {
            if (anchor instanceof VerticalAnchor.BelowTop top) {
                return DataResult.success(new NamedOffset(Type.TOP, Range.of(-top.offset())));
            } else if (anchor instanceof VerticalAnchor.AboveBottom bottom) {
                return DataResult.success(new NamedOffset(Type.BOTTOM, Range.of(bottom.offset())));
            } else if (anchor instanceof VerticalAnchor.Absolute absolute) {
                return DataResult.success(new NamedOffset(Type.ABSOLUTE, Range.of(absolute.y())));
            }
            return DataResult.error(() -> "Unsupported vertical anchor: " + anchor);
        }

        private boolean isConstant() {
            return this.y.diff() == 0;
        }
    }

    private enum Type {
        BOTTOM,
        TOP,
        ABSOLUTE;

        private final Codec<NamedOffset> codec =
            Range.CODEC.fieldOf(this.key()).xmap(y -> new NamedOffset(this, y), o -> o.y).codec();

        public String key() {
            return this.name().toLowerCase();
        }
    }

    private enum Distribution {
        UNIFORM,
        TRAPEZOID
    }
}
