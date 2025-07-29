package personthecat.pangaea.serialization.extras;

import com.mojang.serialization.DataResult;
import personthecat.pangaea.data.AnchorCutoff;

import java.util.function.BiFunction;

public record OffsetBounds(OffsetValue min, OffsetValue max, double harshness) {
    public static DataResult<OffsetBounds> from(AnchorCutoff cutoff) {
        return OffsetValue.from(cutoff.min()).flatMap(min -> OffsetValue.from(cutoff.max()).map(max ->
            new OffsetBounds(min, max, cutoff.harshness())));
    }

    public static <R> DataResult<R> range(
            AnchorCutoff lower, AnchorCutoff upper, BiFunction<OffsetBounds, OffsetBounds, R> f) {
        return flatRange(lower, upper, f.andThen(DataResult::success));
    }

    public static <R> DataResult<R> flatRange(
            AnchorCutoff lower, AnchorCutoff upper, BiFunction<OffsetBounds, OffsetBounds, DataResult<R>> f) {
        return from(lower).flatMap(l -> from(upper).flatMap(u -> f.apply(l, u)));
    }
}
