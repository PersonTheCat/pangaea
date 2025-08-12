package personthecat.pangaea.world.weight;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import personthecat.pangaea.util.Utils;
import personthecat.pangaea.world.level.PangaeaContext;

public final class DefaultWeight implements WeightFunction {
    public static final DefaultWeight INSTANCE = new DefaultWeight();
    public static final MapCodec<DefaultWeight> CODEC = MapCodec.unit(INSTANCE);

    private DefaultWeight() {}

    @Override
    public double compute(PangaeaContext pg, DensityFunction.FunctionContext fn) {
        final var graph = pg.noise;
        final double c = graph.getContinentalness(fn);
        if (c < -0.19) {
            return NEVER;
        }
        double weight = 0;
        if (c < 0.15) {
            weight += Utils.squareQuantized(c - 0.15);
        }
        final float pv = graph.getPv(fn);
        if (pv < -0.6) {
            return NEVER;
        } else if (pv < -0.45) {
            weight += Utils.squareQuantized(pv - 0.45);
        } else if (pv > 0.25) {
            return NEVER;
        } else if (pv > 0.1) {
            weight += Utils.squareQuantized(pv - 0.1);
        }
        final double sd = graph.getSd(fn);
        weight += Utils.squareQuantized(sd) * 10.0;
        return weight;
    }

    @Override
    public MapCodec<DefaultWeight> codec() {
        return CODEC;
    }
}
