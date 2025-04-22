package personthecat.pangaea.world.provider;

import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.pangaea.world.level.PangaeaContext;

public abstract class ScalableFloatProvider extends FloatProvider {

    @Override
    public final float sample(RandomSource rand) {
        return this.sample(rand, PangaeaContext.get().targetPos);
    }

    protected abstract float sample(RandomSource rand, FunctionContext ctx);
}
