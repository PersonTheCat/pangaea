package personthecat.pangaea.world.provider;

import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.pangaea.world.level.PangaeaContext;

public abstract class ScalableIntProvider extends IntProvider {

    @Override
    public final int sample(RandomSource rand) {
        return this.sample(rand, PangaeaContext.get().targetPos);
    }

    protected abstract int sample(RandomSource rand, FunctionContext ctx);
}
