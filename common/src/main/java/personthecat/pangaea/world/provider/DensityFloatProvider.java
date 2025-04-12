package personthecat.pangaea.world.provider;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProviderType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import org.jetbrains.annotations.NotNull;
import personthecat.pangaea.world.density.AutoWrapDensity;

public class DensityFloatProvider extends ScalableFloatProvider {
    public static final MapCodec<DensityFloatProvider> CODEC =
        AutoWrapDensity.HELPER_CODEC.fieldOf("density")
            .xmap(DensityFloatProvider::new, provider -> provider.density);
    public static final FloatProviderType<DensityFloatProvider> TYPE = () -> CODEC;
    private final DensityFunction density;

    public DensityFloatProvider(DensityFunction density) {
        this.density = density;
    }

    @Override
    protected float sample(RandomSource rand, FunctionContext ctx) {
        return (float) this.density.compute(ctx);
    }

    @Override
    public float getMinValue() {
        return (float) this.density.minValue();
    }

    @Override
    public float getMaxValue() {
        return (float) this.density.maxValue();
    }

    @Override
    public @NotNull FloatProviderType<DensityFloatProvider> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return this.density.toString();
    }
}
