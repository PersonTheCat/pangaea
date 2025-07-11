package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import personthecat.catlib.serialization.codec.CodecUtils;
import personthecat.pangaea.world.density.AutoWrapDensity;
import personthecat.pangaea.world.weight.CutoffWeight;
import personthecat.pangaea.world.weight.DensityWeight;
import personthecat.pangaea.world.weight.MultipleWeight;
import personthecat.pangaea.world.weight.SumWeight;
import personthecat.pangaea.world.weight.WeightFunction;

import java.util.List;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.defaultType;

public class StructuralWeightCodec extends MapCodec<WeightFunction> {
    public static final StructuralWeightCodec INSTANCE = new StructuralWeightCodec();
    private static final List<String> KEYS = List.of("mul", "times", "add", "plus", "density", "input", "upper", "lower");

    private StructuralWeightCodec() {}

    public static Codec<WeightFunction> wrap(Codec<WeightFunction> codec) {
        return defaultType(codec, INSTANCE, (o, w) -> canBeStructural(w));
    }

    private static boolean canBeStructural(WeightFunction weight) {
        return weight instanceof CutoffWeight
            || weight instanceof DensityWeight
            || weight instanceof MultipleWeight
            || weight instanceof SumWeight;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return KEYS.stream().map(ops::createString);
    }

    @Override
    public <T> DataResult<WeightFunction> decode(DynamicOps<T> ops, MapLike<T> input) {
        var arg1 = input.get("mul");
        var arg2 = input.get("times");
        if (arg1 != null && arg2 != null) {
            return this.decodeMulTimes(ops, arg1, arg2);
        }
        arg1 = input.get("add");
        arg2 = input.get("plus");
        if (arg1 != null && arg2 != null) {
            return this.decodeAddPlus(ops, arg1, arg2);
        }
        arg1 = input.get("density");
        if (arg1 != null) {
            return AutoWrapDensity.HELPER_CODEC.parse(ops, arg1).map(DensityWeight::new);
        }
        if (input.get("input") != null && (input.get("upper") != null || input.get("lower") != null)) {
            return CodecUtils.asParent(CutoffWeight.CODEC.decode(ops, input));
        }
        return DataResult.error(() -> "no structural fields or type present");
    }

    private <T> DataResult<WeightFunction> decodeMulTimes(DynamicOps<T> ops, T mul, T times) {
        return WeightFunction.CODEC.parse(ops, mul)
            .flatMap(arg1 -> WeightFunction.CODEC.parse(ops, times)
                .map(arg2 -> MultipleWeight.create(arg1, arg2)));
    }

    private <T> DataResult<WeightFunction> decodeAddPlus(DynamicOps<T> ops, T add, T plus) {
        return WeightFunction.CODEC.parse(ops, add)
            .flatMap(arg1 -> WeightFunction.CODEC.parse(ops, plus)
                .map(arg2 -> SumWeight.create(arg1, arg2)));
    }

    @Override
    public <T> RecordBuilder<T> encode(WeightFunction input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        if (input instanceof MultipleWeight multiple) {
            return this.encodeMulTimes(multiple, ops, prefix);
        } else if (input instanceof SumWeight sum) {
            return this.encodeAddPlus(sum, ops, prefix);
        } else if (input instanceof DensityWeight density) {
            return DensityWeight.CODEC.encode(density, ops, prefix);
        } else if (input instanceof CutoffWeight cutoff) {
            return CutoffWeight.CODEC.encode(cutoff, ops, prefix);
        }
        return prefix.withErrorsFrom(DataResult.error(() -> "not a structural type: " + input));
    }

    private <T> RecordBuilder<T> encodeMulTimes(MultipleWeight input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return prefix.add("mul", WeightFunction.CODEC.encodeStart(ops, input.argument1()))
            .add("times", WeightFunction.CODEC.encodeStart(ops, input.argument2()));
    }

    private <T> RecordBuilder<T> encodeAddPlus(SumWeight input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return prefix.add("add", WeightFunction.CODEC.encodeStart(ops, input.argument1()))
            .add("plus", WeightFunction.CODEC.encodeStart(ops, input.argument2()));
    }
}
