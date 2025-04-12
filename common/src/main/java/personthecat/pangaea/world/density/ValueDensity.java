package personthecat.pangaea.world.density;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import personthecat.pangaea.world.level.ScopeExtension;

public interface ValueDensity extends DensityFunction.SimpleFunction {

    @Override
    default double compute(FunctionContext ctx) {
        return this.compute(ScopeExtension.lookupDensityRand(), ctx);
    }

    double compute(RandomSource rand, FunctionContext ctx);
}
