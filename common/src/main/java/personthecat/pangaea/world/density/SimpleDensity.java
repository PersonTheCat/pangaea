package personthecat.pangaea.world.density;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface SimpleDensity extends DensityFunction.SimpleFunction {

    static SimpleDensity density(final SimpleDensity density) {
        return density;
    }

    @Override
    default double minValue() {
        return Double.MIN_VALUE;
    }

    @Override
    default double maxValue() {
        return Double.MAX_VALUE;
    }

    @Override
    default @NotNull KeyDispatchDataCodec<SimpleDensity> codec() {
        return KeyDispatchDataCodec.of(MapCodec.unit(this));
    }
}
