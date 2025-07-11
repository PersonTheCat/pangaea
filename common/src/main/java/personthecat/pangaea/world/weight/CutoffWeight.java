package personthecat.pangaea.world.weight;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.catlib.data.FloatRange;
import personthecat.pangaea.util.Utils;
import personthecat.pangaea.world.level.PangaeaContext;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record CutoffWeight(WeightFunction input, FloatRange lower, FloatRange upper) implements WeightFunction {
    private static final FloatRange DEFAULT_LOWER = FloatRange.of(-99999);
    private static final FloatRange DEFAULT_UPPER = FloatRange.of(99999);
    public static final MapCodec<CutoffWeight> CODEC = codecOf(
        field(WeightFunction.CODEC, "input", CutoffWeight::input),
        defaulted(FloatRange.CODEC, "lower", DEFAULT_LOWER, CutoffWeight::lower),
        defaulted(FloatRange.CODEC, "upper", DEFAULT_UPPER, CutoffWeight::upper),
        CutoffWeight::new
    ).validate(CutoffWeight::validate);

    @Override
    public double compute(PangaeaContext pg, FunctionContext fn) {
        final var cutoff = this.input.compute(pg, fn);
        final var lower = this.lower;
        final var upper = this.upper;
        if (cutoff < lower.min()) {
            return NEVER;
        } else if (cutoff < lower.max()) {
            return Utils.squareQuantized(cutoff - lower.max());
        } else if (cutoff > upper.max()) {
            return NEVER;
        } else if (cutoff > upper.min()) {
            Utils.squareQuantized(cutoff - upper.min());
        }
        return 0;
    }

    private DataResult<CutoffWeight> validate() {
        if (this.lower.equals(DEFAULT_LOWER) && this.upper.equals(DEFAULT_UPPER)) {
            return DataResult.error(() -> "Must specify upper, lower, or both");
        }
        return DataResult.success(this);
    }

    @Override
    public MapCodec<CutoffWeight> codec() {
        return CODEC;
    }
}
