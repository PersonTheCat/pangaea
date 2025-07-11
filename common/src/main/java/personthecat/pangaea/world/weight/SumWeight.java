package personthecat.pangaea.world.weight;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.pangaea.world.level.PangaeaContext;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public sealed interface SumWeight extends TwoArgumentWeightFunction {
    MapCodec<WeightFunction> CODEC =
        Apply2.CODEC.flatXmap(SumWeight::optimize, SumWeight::normalize);

    @Override
    default MapCodec<WeightFunction> codec() {
        return CODEC;
    }

    static WeightFunction create(WeightFunction argument1, WeightFunction argument2) {
        if (argument1 instanceof ConstantWeight c1 && argument2 instanceof ConstantWeight c2) {
            return new ConstantWeight(c1.weight() * c2.weight());
        } else if (argument1 instanceof ConstantWeight c) {
            return new ApplyConstant(argument2, c.weight());
        } else if (argument2 instanceof ConstantWeight c) {
            return new ApplyConstant(argument1, c.weight());
        }
        return new Apply2(argument1, argument2);
    }

    static DataResult<WeightFunction> optimize(Apply2 apply2) {
        return DataResult.success(create(apply2.argument1, apply2.argument2));
    }

    static DataResult<Apply2> normalize(WeightFunction weight) {
        if (weight instanceof ConstantWeight) {
            return DataResult.success(new Apply2(weight, new ConstantWeight(1)));
        } else if (weight instanceof ApplyConstant c) {
            return DataResult.success(new Apply2(c.argument, new ConstantWeight(c.constant)));
        } else if (weight instanceof Apply2 a) {
            return DataResult.success(a);
        }
        return DataResult.error(() -> "Cannot normalize weight as multiple: " + weight);
    }

    record Apply2(WeightFunction argument1, WeightFunction argument2) implements SumWeight {
        private static final MapCodec<Apply2> CODEC = codecOf(
            field(WeightFunction.CODEC, "argument1", Apply2::argument1),
            field(WeightFunction.CODEC, "argument2", Apply2::argument2),
            Apply2::new
        );

        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return this.argument1.compute(pg, fn) + this.argument2.compute(pg, fn);
        }
    }

    record ApplyConstant(WeightFunction argument, double constant) implements SumWeight {

        @Override
        public double compute(PangaeaContext pg, FunctionContext fn) {
            return this.argument.compute(pg, fn) + this.constant;
        }

        @Override
        public WeightFunction argument1() {
            return this.argument;
        }

        @Override
        public WeightFunction argument2() {
            return new ConstantWeight(this.constant);
        }
    }
}
