package personthecat.pangaea.serialization.extras;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.VerticalAnchor.Absolute;
import personthecat.catlib.data.Range;
import personthecat.catlib.serialization.codec.CodecUtils;
import personthecat.pangaea.data.AnchorCutoff;

import java.util.Comparator;
import java.util.List;

import static personthecat.pangaea.world.density.DensityCutoff.DEFAULT_HARSHNESS;

// A | [ A, A ] | [[ A, A ], [ A, A ]]
public sealed interface HeightParams {
    Codec<HeightParams> CODEC =
        CodecUtils.<HeightParams>simpleAny(AnchorOnly.CODEC, AnchorList.CODEC, AnchorMatrix.CODEC)
            .withEncoder(HeightParams::codec);

    int min();
    int max();
    boolean isAbsolute();
    Codec<? extends HeightParams> codec();

    default Range range() {
        return Range.of(this.min(), this.max());
    }

    private static List<VerticalAnchor> sortAnchors(List<VerticalAnchor> anchors) {
        if (anchors.stream().allMatch(a -> a instanceof Absolute)) {
            return anchors.stream()
                .sorted(Comparator.comparingInt(a -> ((Absolute) a).y()))
                .toList();
        }
        return anchors;
    }

    record AnchorOnly(VerticalAnchor anchor) implements HeightParams {
        public static final Codec<AnchorOnly> CODEC =
            VerticalAnchor.CODEC.xmap(AnchorOnly::new, AnchorOnly::anchor);

        @Override
        public int min() {
            return ((Absolute) this.anchor).y();
        }

        @Override
        public int max() {
            return ((Absolute) this.anchor).y();
        }

        @Override
        public boolean isAbsolute() {
            return this.anchor instanceof Absolute;
        }

        @Override
        public Codec<? extends HeightParams> codec() {
            return CODEC;
        }
    }

    record AnchorList(VerticalAnchor first, VerticalAnchor last) implements HeightParams {
        public static final Codec<AnchorList> CODEC =
            VerticalAnchor.CODEC.listOf(2, 2).xmap(AnchorList::fromList, AnchorList::toList);

        public static AnchorList fromList(List<VerticalAnchor> list) {
            list = sortAnchors(list);
            return new AnchorList(list.getFirst(), list.getLast());
        }

        public List<VerticalAnchor> toList() {
            return List.of(this.first, this.last);
        }

        @Override
        public int min() {
            return ((Absolute) this.first).y();
        }

        @Override
        public int max() {
            return ((Absolute) this.last).y();
        }

        @Override
        public boolean isAbsolute() {
            return this.first instanceof Absolute && this.last instanceof Absolute;
        }

        @Override
        public Codec<? extends HeightParams> codec() {
            return CODEC;
        }
    }

    record AnchorMatrix(VerticalAnchor lMin, VerticalAnchor lMax, VerticalAnchor uMin, VerticalAnchor uMax) implements HeightParams {
        public static final Codec<AnchorMatrix> CODEC =
            VerticalAnchor.CODEC.listOf(2, 2).listOf(2, 2).xmap(AnchorMatrix::fromMatrix, AnchorMatrix::toMatrix)
                .validate(AnchorMatrix::validate);

        public static AnchorMatrix fromMatrix(List<List<VerticalAnchor>> matrix) {
            final var lower = sortAnchors(matrix.getFirst());
            final var upper = sortAnchors(matrix.getLast());
            return new AnchorMatrix(lower.getFirst(), lower.getLast(), upper.getFirst(), upper.getLast());
        }

        private DataResult<AnchorMatrix> validate() {
            if (this.lMax instanceof Absolute(var ln) && this.uMin instanceof Absolute(var un)) {
                if (ln > un) {
                    return DataResult.error(() -> "lower > upper: " + this.toMatrix());
                }
            }
            final var t1 = AnchorType.zeroOffsetType(this.lMax);
            final var t2 = AnchorType.zeroOffsetType(this.uMin);
            if (t1.isSuccess() && t2.isSuccess()) {
                if (t1.getOrThrow().isAbove(t2.getOrThrow())) {
                    return DataResult.error(() -> "Names out of sequence: " + this.toMatrix());
                }
            }
            return DataResult.success(this);
        }

        public List<List<VerticalAnchor>> toMatrix() {
            return List.of(List.of(this.lMin, this.lMax), List.of(this.uMin, this.uMax));
        }

        @Override
        public int min() {
            return ((Absolute) this.lMin).y();
        }

        @Override
        public int max() {
            return ((Absolute) this.uMax).y();
        }

        public Range lower() {
            return Range.of(((Absolute) this.lMin).y(), ((Absolute) this.lMax).y());
        }

        public Range upper() {
            return Range.of(((Absolute) this.uMin).y(), ((Absolute) this.uMax).y());
        }

        public AnchorCutoff lowerCutoff() {
            return new AnchorCutoff(this.lMin, this.lMax, DEFAULT_HARSHNESS);
        }

        public AnchorCutoff upperCutoff() {
            return new AnchorCutoff(this.uMin, this.uMax, DEFAULT_HARSHNESS);
        }

        public AnchorCutoff lowerCutoff(AnchorType type, double harshness) {
            return cutoff(type, this.lMin, this.lMax, harshness);
        }

        public AnchorCutoff upperCutoff(AnchorType type, double harshness) {
            return cutoff(type, this.uMin, this.uMax, harshness);
        }

        private static AnchorCutoff cutoff(AnchorType type, VerticalAnchor min, VerticalAnchor max, double harshness) {
            return new AnchorCutoff(type.at(((Absolute) min).y()), type.at(((Absolute) max).y()), harshness);
        }

        @Override
        public boolean isAbsolute() {
            return this.lMin instanceof Absolute
                && this.lMax instanceof Absolute
                && this.uMin instanceof Absolute
                && this.uMax instanceof Absolute;
        }

        @Override
        public Codec<? extends HeightParams> codec() {
            return CODEC;
        }
    }
}
