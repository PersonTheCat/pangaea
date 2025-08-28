package personthecat.pangaea.world.surface;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules.Condition;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.FloatRange;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.extras.ContextExtras;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.weight.WeightFunction;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record WeightConditionSource(WeightFunction weight, FloatRange threshold) implements ConditionSource {
    public static final MapCodec<WeightConditionSource> CODEC = codecOf(
        field(WeightFunction.CODEC, "weight", WeightConditionSource::weight),
        defaulted(FloatRange.RANGE_UP_CODEC, "threshold", FloatRange.of(0, Float.MAX_VALUE), WeightConditionSource::threshold),
        WeightConditionSource::new
    );

    @Override
    public Condition apply(Context ctx) {
        return new DensityCondition(ctx, ContextExtras.getPangaea(ctx), new MutableFunctionContext(), this.weight, this.threshold);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends ConditionSource> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    record DensityCondition(
            Context ctx, PangaeaContext pg, MutableFunctionContext pos, WeightFunction weight, FloatRange threshold) implements Condition {
        @Override
        public boolean test() {
            return this.threshold.contains((float) this.weight.compute(this.pg, this.pos.set(this.ctx.pos)));
        }
    }
}
