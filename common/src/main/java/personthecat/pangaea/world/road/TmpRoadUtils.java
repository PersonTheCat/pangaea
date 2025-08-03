package personthecat.pangaea.world.road;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.catlib.data.Lazy;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.data.NoiseGraph;
import personthecat.pangaea.util.Utils;
import personthecat.pangaea.world.density.DensityController;
import personthecat.pangaea.world.density.SimpleDensity;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.weight.WeightFunction;

// will be ugly until deleted
public class TmpRoadUtils implements WeightFunction {
    public static final double NEVER = 1e20;
    public static final TmpRoadUtils INSTANCE = new TmpRoadUtils();
    public static final MapCodec<TmpRoadUtils> CODEC = MapCodec.unit(INSTANCE);

    private static Algorithm ALGORITHM = Algorithm.SLOPE_ONLY; // DEBUG
    private static SlopeFunction SLOPE_FUNCTION = SlopeFunction.DIRECT_SQUARE;
    private static double SLOPE_MULTIPLE = 100.0F;

    public static final Lazy<DensityFunction> SURFACE = Lazy.of(() -> {
        final var overworld = Pangaea.SERVER.overworld();
        final var router = overworld.getChunkSource().randomState().router();
        final var controller = (DensityController) router.finalDensity();
        return new DebugSampler(controller.unwrapMain().surface);
    });

    @Override
    public double compute(PangaeaContext pg, FunctionContext fn) {
        return getWeight(pg.noise, fn);
    }

    public static double getWeight(NoiseGraph graph, FunctionContext ctx) {
        return switch (ALGORITHM) {
            case OLD -> getWeightOld(graph, ctx);
            case NEW -> getWeightNew(graph, ctx);
            case SLOPE_ONLY -> getSlopeOnly(graph, ctx);
        };
    }

    public static double getWeightOld(NoiseGraph graph, FunctionContext ctx) {
        final double c = graph.getContinentalness(ctx);
        if (c < -0.19) {
            return NEVER;
        }
        double weight = 0;
        if (c < 0.15) {
            weight += Utils.squareQuantized(c - 0.15);
        }
        final float pv = graph.getPv(ctx);
        if (pv < -0.6) {
            return NEVER;
        } else if (pv < -0.45) {
            weight += Utils.squareQuantized(pv - 0.45);
        } else if (pv > 0.25) {
            return NEVER;
        } else if (pv > 0.1) {
            weight += Utils.squareQuantized(pv - 0.1);
        }
//        final double sd = graph.getSd(sampler, ctx);
        final var f = SURFACE.get();
        final var pos = MutableFunctionContext.from(ctx);
        weight += applySlope(Utils.stdDev(
            f.compute(pos),
            f.compute(pos.north(2)),
            f.compute(pos.south(4)),
            f.compute(pos.north(2).west(2)),
            f.compute(pos.east(4))
        ));
        return weight;
    }

    public static double getWeightNew(NoiseGraph graph, FunctionContext ctx) {
        final double c = graph.getContinentalness(ctx);
        if (c < -0.19) {
            return NEVER;
        }
        double weight = 0;
        if (c < 0.15) {
            weight += Utils.squareQuantized(c - 0.15);
        }
        final float pv = graph.getPv(ctx);
        if (pv < -0.6) {
            return NEVER;
        } else if (pv < -0.45) {
            weight += Utils.squareQuantized(pv - 0.45);
        } else if (pv > 0.25) {
            return NEVER;
        } else if (pv > 0.1) {
            weight += Utils.squareQuantized(pv - 0.1);
        }
        final var f = SURFACE.get();
        final var p = MutableFunctionContext.from(ctx).at(0);
        final var slope = Math.sqrt(
            Math.pow(f.compute(p.south(2)) - f.compute(p.north(4)), 2) +
            Math.pow(f.compute(p.south(2).west(2)) - f.compute(p.east(4)), 2)
        );
        weight += applySlope(slope);
        return weight;
    }

    public static double getSlopeOnly(NoiseGraph graph, FunctionContext ctx) {
        final double c = graph.getContinentalness(ctx);
        if (c < -0.19) {
            return NEVER;
        }
        final var f = SURFACE.get();
        final var p = MutableFunctionContext.from(ctx).at(0);
        final var slope = Math.sqrt(
            Math.pow(f.compute(p.south(2)) - f.compute(p.north(4)), 2) +
            Math.pow(f.compute(p.south(2).west(2)) - f.compute(p.east(4)), 2)
        );
        return applySlope(slope);
    }

    private static double applySlope(double slope) {
        return switch (SLOPE_FUNCTION) {
            case SQUARE_QUANTIZED -> Utils.squareQuantized(slope) * SLOPE_MULTIPLE;
            case DIRECT_SQUARE -> (slope * SLOPE_MULTIPLE) * (slope * SLOPE_MULTIPLE);
            case MULTIPLY -> slope * SLOPE_MULTIPLE;
            case DIRECT -> slope;
        };
    }

    @Override
    public MapCodec<? extends WeightFunction> codec() {
        return CODEC;
    }

    enum Algorithm { OLD, NEW, SLOPE_ONLY }
    enum SlopeFunction { SQUARE_QUANTIZED, DIRECT, MULTIPLY, DIRECT_SQUARE }

    private record DebugSampler(DensityFunction input) implements SimpleDensity {
        private static HorizontalMode HORIZONTAL_MODE = HorizontalMode.SUBTRACT_FOUR; // DEBUG
        private static VerticalMode VERTICAL_MODE = VerticalMode.DIRECT; // DEBUG

        @Override
        public double compute(FunctionContext ctx) {
            ctx = switch (HORIZONTAL_MODE) {
                case QUARTER -> new SinglePointContext(ctx.blockX() / 4, ctx.blockY(), ctx.blockZ() / 4);
                case EIGHTH -> new SinglePointContext(ctx.blockX() / 8, ctx.blockY(), ctx.blockZ() / 8);
                case DIRECT -> ctx;
                case FOUR_TIMES -> new SinglePointContext(ctx.blockX() * 4, ctx.blockY(), ctx.blockZ() * 4);
                case EIGHT_TIMES -> new SinglePointContext(ctx.blockX() * 8, ctx.blockY(), ctx.blockZ() * 8);
                case ADD_FOUR -> new SinglePointContext(ctx.blockX() + 4, ctx.blockY(), ctx.blockZ() + 4);
                case SUBTRACT_FOUR -> new SinglePointContext(ctx.blockX() - 4, ctx.blockY(), ctx.blockZ() - 4);
            };
            ctx = switch (VERTICAL_MODE) {
                case AT_0 -> new SinglePointContext(ctx.blockX(), 0, ctx.blockZ());
                case DIRECT -> ctx;
            };
            return this.input.compute(ctx);
        }

        enum HorizontalMode { QUARTER, EIGHTH, DIRECT, FOUR_TIMES, EIGHT_TIMES, ADD_FOUR, SUBTRACT_FOUR}
        enum VerticalMode { AT_0, DIRECT }
    }
}
