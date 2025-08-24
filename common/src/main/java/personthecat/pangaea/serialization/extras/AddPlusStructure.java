package personthecat.pangaea.serialization.extras;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import personthecat.pangaea.serialization.codec.DensityHelper;
import personthecat.pangaea.world.weight.SumWeight;
import personthecat.pangaea.world.weight.WeightFunction;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

record AddPlusStructure<A>(A add, A plus) {
    static final MapCodec<AddPlusStructure<DensityFunction>> DENSITY_CODEC =
        createCodec(DensityHelper.CODEC);
    static final MapCodec<AddPlusStructure<WeightFunction>> WEIGHT_CODEC =
        createCodec(WeightFunction.CODEC);

    static DensityFunctions.TwoArgumentSimpleFunction toDensity(AddPlusStructure<DensityFunction> s) {
        return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.ADD, s.add, s.plus);
    }

    static AddPlusStructure<DensityFunction> fromDensity(DensityFunctions.TwoArgumentSimpleFunction t) {
        return new AddPlusStructure<>(t.argument1(), t.argument2());
    }

    static WeightFunction toWeight(AddPlusStructure<WeightFunction> s) {
        return SumWeight.create(s.add, s.plus);
    }

    static AddPlusStructure<WeightFunction> fromWeight(WeightFunction f) {
        final var s = (SumWeight) f;
        return new AddPlusStructure<>(s.argument1(), s.argument2());
    }

    private static <A> MapCodec<AddPlusStructure<A>> createCodec(Codec<A> base) {
        return codecOf(
            field(base, "add", AddPlusStructure::add),
            field(base, "plus", AddPlusStructure::plus),
            AddPlusStructure::new
        );
    }
}
