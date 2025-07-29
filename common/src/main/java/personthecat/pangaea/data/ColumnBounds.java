package personthecat.pangaea.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.Util;
import personthecat.catlib.data.Range;
import personthecat.pangaea.world.density.DensityCutoff;

import java.util.List;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleAny;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.pangaea.world.density.DensityCutoff.DEFAULT_HARSHNESS;

public record ColumnBounds(DensityCutoff lower, DensityCutoff upper) {
    public static final MapCodec<ColumnBounds> OBJECT_CODEC = codecOf(
        field(DensityCutoff.CODEC, "lower", ColumnBounds::lower),
        field(DensityCutoff.CODEC, "upper", ColumnBounds::upper),
        ColumnBounds::new
    );
    private static final Codec<ColumnBounds> RANGE_CODEC =
        Range.CODEC.xmap(r -> create(r.min(), r.max()), c -> new Range(c.min(), c.max()))
            .validate(ColumnBounds::validateAsRange);
    private static final Codec<ColumnBounds> ARRAY_CODEC =
        DensityCutoff.CODEC.listOf()
            .validate(l -> Util.fixedSize(l, 2))
            .xmap(l -> new ColumnBounds(l.getFirst(), l.get(1)), c -> List.of(c.lower, c.upper));
    public static final Codec<ColumnBounds> CODEC =
        simpleAny(OBJECT_CODEC.codec(), RANGE_CODEC, ARRAY_CODEC)
            .withEncoder(c -> c.canBeRange() ? RANGE_CODEC : ARRAY_CODEC);

    public static ColumnBounds create(int min, int max) {
        return create(min, max, DEFAULT_HARSHNESS);
    }

    public static ColumnBounds create(int min, int max, double harshness) {
        return create(min, max, automaticWidth(min, max), harshness);
    }

    public static ColumnBounds create(int min, int max, int width, double harshness) {
        return new ColumnBounds(
            new DensityCutoff(min, min + width, harshness),
            new DensityCutoff(max - width, max, harshness)
        );
    }

    public static ColumnBounds create(Range lower, Range upper, double harshness) {
        return new ColumnBounds(
            new DensityCutoff(lower.min(), lower.max(), harshness),
            new DensityCutoff(upper.min(), upper.max(), harshness)
        );
    }

    private static int automaticWidth(int min, int max) {
        return Math.min(10, (int) (((double) max - (double) min) * 0.1));
    }

    public double transformNoise(double n, int y) {
        return this.lower.transformLower(this.upper.transformUpper(n, y), y);
    }

    public boolean isInRange(int y) {
        return y >= this.min() && y < this.max();
    }

    public int min() {
        return (int) this.lower.min();
    }

    public int max() {
        return (int) this.upper.max();
    }

    public Range lowerRange() {
        return Range.of((int) this.lower.min(), (int) this.lower.max());
    }

    public Range upperRange() {
        return Range.of((int) this.upper.min(), (int) this.upper.max());
    }

    private DataResult<ColumnBounds> validateAsRange() {
        return this.canBeRange()
            ? DataResult.success(this)
            : DataResult.error(() -> "Cannot express as range: " + this);
    }

    private boolean canBeRange() {
        final int automatic = automaticWidth(this.min(), this.max());
        return this.lower.width() == automatic
            && this.lower.harshness() == DEFAULT_HARSHNESS
            && this.upper.width() == automatic
            && this.upper.harshness() == DEFAULT_HARSHNESS
            && this.lower.max() <= this.upper.min();
    }
}
