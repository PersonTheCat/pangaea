package personthecat.pangaea.serialization.extras;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import personthecat.pangaea.serialization.codec.DensityHelper;
import personthecat.pangaea.world.weight.MultipleWeight;
import personthecat.pangaea.world.weight.WeightFunction;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

record MulTimesStructure<A>(A mul, A times) {
    static final MapCodec<MulTimesStructure<DensityFunction>> DENSITY_CODEC =
        createCodec(DensityHelper.CODEC);
    static final MapCodec<MulTimesStructure<WeightFunction>> WEIGHT_CODEC =
        createCodec(WeightFunction.CODEC);

    static DensityFunctions.TwoArgumentSimpleFunction toDensity(MulTimesStructure<DensityFunction> s) {
        return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.MUL, s.mul, s.times);
    }

    static MulTimesStructure<DensityFunction> fromDensity(DensityFunctions.TwoArgumentSimpleFunction t) {
        return new MulTimesStructure<>(t.argument1(), t.argument2());
    }

    static WeightFunction toWeight(MulTimesStructure<WeightFunction> s) {
        return MultipleWeight.create(s.mul, s.times);
    }

    static MulTimesStructure<WeightFunction> fromWeight(WeightFunction f) {
        final var s = (MultipleWeight) f;
        return new MulTimesStructure<>(s.argument1(), s.argument2());
    }

    private static <A> MapCodec<MulTimesStructure<A>> createCodec(Codec<A> base) {
        return codecOf(
            field(base, "mul", MulTimesStructure::mul),
            field(base, "times", MulTimesStructure::times),
            MulTimesStructure::new
        );
    }
}
