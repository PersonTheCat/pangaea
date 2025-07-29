package personthecat.pangaea.serialization.extras;

import com.mojang.serialization.DataResult;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.VerticalAnchor.AboveBottom;
import net.minecraft.world.level.levelgen.VerticalAnchor.Absolute;
import net.minecraft.world.level.levelgen.VerticalAnchor.BelowTop;
import personthecat.pangaea.world.provider.MiddleVerticalAnchor;
import personthecat.pangaea.world.provider.SeaLevelVerticalAnchor;
import personthecat.pangaea.world.provider.SurfaceVerticalAnchor;

import java.util.function.BiFunction;

public record OffsetValue(AnchorType type, int value) {
    public static DataResult<OffsetValue> from(VerticalAnchor a) {
        return switch (a) {
            case AboveBottom(int o) -> DataResult.success(new OffsetValue(AnchorType.BOTTOM, o));
            case Absolute(int o) -> DataResult.success(new OffsetValue(AnchorType.ABSOLUTE, o));
            case BelowTop(int o) -> DataResult.success(new OffsetValue(AnchorType.TOP, -o));
            case SeaLevelVerticalAnchor(int o) -> DataResult.success(new OffsetValue(AnchorType.SEA_LEVEL, o));
            case SurfaceVerticalAnchor(int o) -> DataResult.success(new OffsetValue(AnchorType.SURFACE, o));
            case MiddleVerticalAnchor(int o) -> DataResult.success(new OffsetValue(AnchorType.MIDDLE, o));
            default -> DataResult.error(() -> "No pattern for anchor type: " + a);
        };
    }

    public static <R> DataResult<R> range(
            VerticalAnchor min, VerticalAnchor max, BiFunction<OffsetValue, OffsetValue, R> f) {
        return flatRange(min, max, f.andThen(DataResult::success));
    }

    public static <R> DataResult<R> flatRange(
            VerticalAnchor min, VerticalAnchor max, BiFunction<OffsetValue, OffsetValue, DataResult<R>> f) {
        return from(min).flatMap(l -> from(max).flatMap(u -> f.apply(l, u)));
    }

    public boolean isAbsolute() {
        return this.type == AnchorType.ABSOLUTE;
    }

    public VerticalAnchor anchor() {
        return this.type.at(this.value);
    }

    public VerticalAnchor absolute() {
        return new Absolute(this.value);
    }
}
