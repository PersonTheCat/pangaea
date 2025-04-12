package personthecat.pangaea.world.provider;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProviderType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import org.jetbrains.annotations.NotNull;
import personthecat.pangaea.world.density.AutoWrapDensity;

public class DensityIntProvider extends ScalableIntProvider {
    public static final MapCodec<DensityIntProvider> CODEC =
        AutoWrapDensity.HELPER_CODEC.fieldOf("density")
            .xmap(DensityIntProvider::new, provider -> provider.density);
    public static final IntProviderType<DensityIntProvider> TYPE = () -> CODEC;
    private final DensityFunction density;

    public DensityIntProvider(DensityFunction density) {
        this.density = density;
    }

    @Override
    protected int sample(RandomSource rand, FunctionContext ctx) {
        return (int) this.density.compute(ctx);
    }

    @Override
    public int getMinValue() {
        return (int) this.density.minValue();
    }

    @Override
    public int getMaxValue() {
        return (int) this.density.maxValue();
    }

    @Override
    public @NotNull IntProviderType<DensityIntProvider> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return this.density.toString();
    }
}
