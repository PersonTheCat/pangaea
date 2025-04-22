package personthecat.pangaea.world.provider;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import personthecat.pangaea.world.level.GenerationContext;

public abstract class ScalableHeightProvider extends HeightProvider {

    @Override
    public final int sample(RandomSource rand, WorldGenerationContext gen) {
        return this.sample(rand, gen, GenerationContext.get().targetPos);
    }

    protected abstract int sample(RandomSource rand, WorldGenerationContext gen, FunctionContext fn);
}
