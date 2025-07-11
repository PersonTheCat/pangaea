package personthecat.pangaea.world.weight;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.List;

public record WeightList(List<WeightFunction> weights) implements WeightFunction {
    public static final MapCodec<WeightList> CODEC =
        WeightFunction.CODEC.listOf().fieldOf("weights").xmap(WeightList::new, WeightList::weights);

    @Override
    public double compute(PangaeaContext pg, FunctionContext fn) {
        double weight = 0;
        for (final var f : this.weights) {
            weight += f.compute(pg, fn);
            if (weight >= NEVER) {
                return NEVER;
            }
        }
        return weight;
    }

    @Override
    public MapCodec<WeightList> codec() {
        return CODEC;
    }
}
