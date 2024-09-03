package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import personthecat.catlib.serialization.codec.DefaultTypeCodec;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.world.density.DensityList;

import java.util.List;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.asMapCodec;
import static net.minecraft.world.level.levelgen.DensityFunction.HOLDER_HELPER_CODEC;

public class StructuralDensityCodec extends MapCodec<DensityFunction> {
    public static final StructuralDensityCodec INSTANCE = new StructuralDensityCodec();
    private static final List<String> KEYS = List.of(
        "mul", "times", "add", "plus", "min", "max", "sum", "abs",
        "square", "cube", "half_negative", "quarter_negative", "squeeze");

    private StructuralDensityCodec() {}

    public static Codec<DensityFunction> wrap(Codec<DensityFunction> codec) {
        return new DefaultTypeCodec<>(asMapCodec(codec), INSTANCE,
            (f, ops) -> Cfg.encodeStructuralDensity() && canBeStructural(f)).codec();
    }

    private static boolean canBeStructural(DensityFunction f) {
        return f instanceof DensityFunctions.TwoArgumentSimpleFunction
            || f instanceof DensityFunctions.Mapped
            || f instanceof DensityList;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return KEYS.stream().map(ops::createString);
    }

    @Override
    public <T> DataResult<DensityFunction> decode(DynamicOps<T> ops, MapLike<T> input) {
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
        if (input.get("min") != null) {
            return DensityList.Min.OPTIMIZED_DECODER.decode(ops, input);
        }
        if (input.get("max") != null) {
            return DensityList.Max.OPTIMIZED_DECODER.decode(ops, input);
        }
        if (input.get("sum") != null) {
            return DensityList.Sum.OPTIMIZED_DECODER.decode(ops, input);
        }
        arg1 = input.get("abs");
        if (arg1 != null) {
            return HOLDER_HELPER_CODEC.parse(ops, arg1).map(DensityFunction::abs);
        }
        arg1 = input.get("square");
        if (arg1 != null) {
            return HOLDER_HELPER_CODEC.parse(ops, arg1).map(DensityFunction::square);
        }
        arg1 = input.get("cube");
        if (arg1 != null) {
            return HOLDER_HELPER_CODEC.parse(ops, arg1).map(DensityFunction::cube);
        }
        arg1 = input.get("half_negative");
        if (arg1 != null) {
            return HOLDER_HELPER_CODEC.parse(ops, arg1).map(DensityFunction::halfNegative);
        }
        arg1 = input.get("quarter_negative");
        if (arg1 != null) {
            return HOLDER_HELPER_CODEC.parse(ops, arg1).map(DensityFunction::quarterNegative);
        }
        arg1 = input.get("squeeze");
        if (arg1 != null) {
            return HOLDER_HELPER_CODEC.parse(ops, arg1).map(DensityFunction::squeeze);
        }
        return DataResult.error(() -> "no structural fields or type present");
    }

    private <T> DataResult<DensityFunction> decodeMulTimes(DynamicOps<T> ops, T mul, T times) {
        return HOLDER_HELPER_CODEC.parse(ops, mul)
            .flatMap(arg1 -> HOLDER_HELPER_CODEC.parse(ops, times)
                .map(arg2 -> DensityFunctions.mul(arg1, arg2)));
    }

    private <T> DataResult<DensityFunction> decodeAddPlus(DynamicOps<T> ops, T add, T plus) {
        return HOLDER_HELPER_CODEC.parse(ops, add)
            .flatMap(arg1 -> HOLDER_HELPER_CODEC.parse(ops, plus)
                .map(arg2 -> DensityFunctions.add(arg1, arg2)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> RecordBuilder<T> encode(DensityFunction input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        if (input instanceof DensityFunctions.TwoArgumentSimpleFunction twoArg) {
            return switch (twoArg.type()) {
                case MUL -> this.encodeMulTimes(twoArg, ops, prefix);
                case ADD -> this.encodeAddPlus(twoArg, ops, prefix);
                case MIN -> DensityList.Min.CODEC.encode(DensityList.Min.from(twoArg), ops, prefix);
                case MAX -> DensityList.Max.CODEC.encode(DensityList.Max.from(twoArg), ops, prefix);
            };
        } else if (input instanceof DensityList) {
            return ((MapCodec<DensityFunction>) input.codec().codec()).encode(input, ops, prefix);
        } else if (input instanceof DensityFunctions.Mapped mapped) {
            return prefix.add(mapped.type().getSerializedName(), HOLDER_HELPER_CODEC.encodeStart(ops, mapped.input()));
        }
        return prefix.withErrorsFrom(DataResult.error(() -> "not a structural type: " + input));
    }

    private <T> RecordBuilder<T> encodeMulTimes(
            DensityFunctions.TwoArgumentSimpleFunction input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return prefix.add("mul", HOLDER_HELPER_CODEC.encodeStart(ops, input.argument1()))
            .add("times", HOLDER_HELPER_CODEC.encodeStart(ops, input.argument2()));
    }

    private <T> RecordBuilder<T> encodeAddPlus(
            DensityFunctions.TwoArgumentSimpleFunction input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return prefix.add("add", HOLDER_HELPER_CODEC.encodeStart(ops, input.argument1()))
            .add("plus", HOLDER_HELPER_CODEC.encodeStart(ops, input.argument2()));
    }
}
