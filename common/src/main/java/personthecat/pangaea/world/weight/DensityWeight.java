package personthecat.pangaea.world.weight;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.pangaea.world.density.AutoWrapDensity;
import personthecat.pangaea.world.level.PangaeaContext;

public record DensityWeight(DensityFunction density) implements WeightFunction {
    public static final MapCodec<DensityWeight> CODEC =
        AutoWrapDensity.HELPER_CODEC.fieldOf("density").xmap(DensityWeight::new, DensityWeight::density);

    @Override
    public double compute(PangaeaContext pg, FunctionContext fn) {
        return this.density.compute(fn);
    }

    @Override
    public MapCodec<DensityWeight> codec() {
        return CODEC;
    }
}
