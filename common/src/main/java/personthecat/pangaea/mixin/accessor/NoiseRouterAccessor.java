package personthecat.pangaea.mixin.accessor;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NoiseRouter.class)
public interface NoiseRouterAccessor {
    @Mutable
    @Accessor
    void setFinalDensity(DensityFunction density);
}
