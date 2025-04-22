package personthecat.pangaea.world.density;

import com.mojang.serialization.Codec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Function;

public record AutoWrapDensity(DensityFunction density) implements DensityFunction {
    public static final Codec<DensityFunction> HELPER_CODEC =
        wrap(DensityFunction.HOLDER_HELPER_CODEC);

    // does not replace the default density function codec
    public static Codec<DensityFunction> wrap(Codec<DensityFunction> codec) {
        return codec.xmap(AutoWrapDensity::create, Function.identity());
    }

    public static DensityFunction create(DensityFunction density) {
        if (density instanceof AutoWrapDensity) {
            return density;
        }
        final var isWrapped = new MutableBoolean();
        density.mapAll(f -> {
            if (f instanceof DensityFunctions.MarkerOrMarked) {
                isWrapped.setTrue();
            }
            return f;
        });
        if (isWrapped.isTrue()) {
            return new AutoWrapDensity(density);
        }
        return density;
    }

    @Override
    public double compute(FunctionContext ctx) {
        return this.wrap().compute(ctx);
    }

    @Override
    public void fillArray(double[] ds, ContextProvider contextProvider) {
        this.wrap().fillArray(ds, contextProvider);
    }

    @Override
    public @NotNull DensityFunction mapAll(Visitor visitor) {
        return this.wrap().mapAll(visitor);
    }

    @Override
    public double minValue() {
        return this.wrap().minValue();
    }

    @Override
    public double maxValue() {
        return this.wrap().maxValue();
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return this.wrap().codec();
    }

    private DensityFunction wrap() {
        return PangaeaContext.get().wrap(this.density);
    }

    @Override
    public String toString() {
        return "autoWrapped(" + this.density + ")";
    }
}
