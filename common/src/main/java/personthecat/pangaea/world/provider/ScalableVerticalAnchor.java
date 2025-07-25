package personthecat.pangaea.world.provider;

import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import personthecat.pangaea.world.level.PangaeaContext;

public interface ScalableVerticalAnchor extends VerticalAnchor {

    @Override
    default int resolveY(WorldGenerationContext gen) {
        return this.resolveY(gen, PangaeaContext.get().targetPos);
    }

    int resolveY(WorldGenerationContext gen, FunctionContext fn);
}
