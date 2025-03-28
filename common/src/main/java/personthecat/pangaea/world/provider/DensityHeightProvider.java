package personthecat.pangaea.world.provider;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import org.jetbrains.annotations.NotNull;
import personthecat.pangaea.world.density.AutoWrapDensity;

public class DensityHeightProvider extends ScalableHeightProvider {
    public static final MapCodec<DensityHeightProvider> CODEC =
        AutoWrapDensity.HELPER_CODEC.fieldOf("density")
            .xmap(DensityHeightProvider::new, p -> p.density);
    public static final HeightProviderType<DensityHeightProvider> TYPE = () -> CODEC;

    private final DensityFunction density;

    public DensityHeightProvider(DensityFunction density) {
        this.density = density;
    }

    @Override
    protected int sample(RandomSource rand, WorldGenerationContext gen, FunctionContext fn) {
        return (int) this.density.compute(fn);
    }

    @Override
    public @NotNull HeightProviderType<DensityHeightProvider> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return this.density.toString();
    }
}
