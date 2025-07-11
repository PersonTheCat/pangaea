package personthecat.pangaea.world.weight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.pangaea.world.level.PangaeaContext;

public record ConstantWeight(double weight) implements WeightFunction {
    public static final MapCodec<ConstantWeight> CODEC =
        Codec.DOUBLE.fieldOf("weight").xmap(ConstantWeight::new, ConstantWeight::weight);

    @Override
    public double compute(PangaeaContext pg, FunctionContext fn) {
        return this.weight;
    }

    @Override
    public MapCodec<ConstantWeight> codec() {
        return CODEC;
    }
}
