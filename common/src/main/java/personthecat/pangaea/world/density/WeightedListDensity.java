package personthecat.pangaea.world.density;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

public record WeightedListDensity(
        SimpleWeightedRandomList<DensityFunction> distribution,
        double minValue,
        double maxValue) implements ValueDensity {
    public static final MapCodec<WeightedListDensity> CODEC =
        SimpleWeightedRandomList.wrappedCodec(DensityFunction.HOLDER_HELPER_CODEC)
            .fieldOf("distribution")
            .xmap(WeightedListDensity::create, WeightedListDensity::distribution);

    public static WeightedListDensity create(SimpleWeightedRandomList<DensityFunction> distribution) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (final var density : distribution.unwrap()) {
            min = Math.min(min, density.data().minValue());
            max = Math.max(max, density.data().maxValue());
        }
        return new WeightedListDensity(distribution, min, max);
    }

    @Override
    public double compute(RandomSource rand, FunctionContext ctx) {
        return this.distribution.getRandomValue(rand)
            .orElseThrow(IllegalStateException::new)
            .compute(ctx);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<WeightedListDensity> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    @Override
    public String toString() {
        return "weighted" + this.distribution.unwrap();
    }
}
