package personthecat.pangaea.world.road;

import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.pangaea.data.NoiseGraph;
import personthecat.pangaea.util.Utils;

public class TmpRoadUtils {
    public static final double NEVER = 1e20;

    public static double getWeight(NoiseGraph graph, Sampler sampler, FunctionContext ctx) {
        final double c = graph.getContinentalness(sampler, ctx);
        if (c < -0.19) {
            return NEVER;
        }
        double weight = 0;
        if (c < 0.15) {
            weight += Utils.squareQuantized(c - 0.15);
        }
        final float pv = graph.getPv(sampler, ctx);
        if (pv < -0.6) {
            return NEVER;
        } else if (pv < -0.45) {
            weight += Utils.squareQuantized(pv - 0.45);
        } else if (pv > 0.25) {
            return NEVER;
        } else if (pv > 0.1) {
            weight += Utils.squareQuantized(pv - 0.1);
        }
        final double sd = graph.getSd(sampler, ctx);
        weight += Utils.squareQuantized(sd) * 10;
        return weight;
    }
}
