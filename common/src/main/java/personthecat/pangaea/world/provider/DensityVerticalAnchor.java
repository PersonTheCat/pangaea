package personthecat.pangaea.world.provider;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import personthecat.pangaea.world.density.AutoWrapDensity;

public record DensityVerticalAnchor(DensityFunction density) implements ScalableVerticalAnchor {
    public static final Codec<DensityVerticalAnchor> CODEC =
        AutoWrapDensity.HELPER_CODEC.fieldOf("density")
            .xmap(DensityVerticalAnchor::new, DensityVerticalAnchor::density)
            .codec();

    @Override
    public int resolveY(WorldGenerationContext gen, FunctionContext fn) {
        return (int) this.density.compute(fn);
    }

    @Override
    public String toString() {
        return this.density.toString();
    }
}
