package personthecat.pangaea.world.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.SimpleFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.DensityFunctions.MarkerOrMarked;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.serialization.codec.CodecUtils;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public class DensityController implements SimpleFunction {
    private static final DensityFunction DEFAULT_SURFACE = DensityFunctions.constant(2.5);
    private static final DensityFunction DEFAULT_ENTRANCES = DensityFunctions.constant(1000000);
    private static final DensityFunction DEFAULT_CAVES = DensityFunctions.constant(1);
    private static final DensityFunction DEFAULT_FILLER = DensityFunctions.constant(-1000000);
    private static final DensityCutoff DEFAULT_UPPER_CUTOFF = new DensityCutoff(255, 255, 0.1);
    private static final DensityCutoff DEFAULT_LOWER_CUTOFF = new DensityCutoff(-64, -64, 0.1);
    public static final MapCodec<DensityController> CODEC = codecOf(
        defaulted(DensityFunction.HOLDER_HELPER_CODEC, "surface", DEFAULT_SURFACE, c -> c.unwrapMain().surface),
        defaulted(DensityFunction.HOLDER_HELPER_CODEC, "entrances", DEFAULT_ENTRANCES, c -> c.unwrapMain().entrances),
        defaulted(DensityCutoff.CODEC.codec(), "upper_cutoff", DEFAULT_UPPER_CUTOFF, c -> c.unwrapMain().upperCutoff),
        defaulted(DensityCutoff.CODEC.codec(), "lower_cutoff", DEFAULT_LOWER_CUTOFF, c -> c.unwrapMain().lowerCutoff),
        defaulted(DensityList.Min.LIST_CODEC, "underground_caverns", DEFAULT_CAVES, c -> c.unwrapMain().undergroundCaverns),
        defaulted(DensityList.Max.LIST_CODEC, "underground_filler", DEFAULT_FILLER, c -> c.unwrapMain().undergroundFiller),
        defaulted(Codec.DOUBLE, "surface_threshold", 1.5625, c -> c.unwrapMain().surfaceThreshold),
        defaulted(Codec.DOUBLE, "filler_threshold", 0.3, c -> c.unwrapMain().fillerThreshold),
        defaulted(DensityList.Min.LIST_CODEC, "global_caverns", DEFAULT_CAVES, c -> c.globalCaverns),
        defaulted(Codec.DOUBLE, "primary_scale", 0.64, c -> c.primaryScale),
        DensityController::new
    );
    private final DensityFunction main;
    private final DensityFunction globalCaverns;
    private final double primaryScale;
    private final double minValue;
    private final double maxValue;

    private DensityController(
            DensityFunction surface, DensityFunction entrances, DensityCutoff upperCutoff, DensityCutoff lowerCutoff,
            DensityFunction undergroundCaverns, DensityFunction undergroundFiller, double surfaceThreshold,
            double fillerThreshold, DensityFunction globalCaverns, double primaryScale) {
        this(new MainFunction(
                surface, entrances, upperCutoff, lowerCutoff, undergroundCaverns,
                undergroundFiller, surfaceThreshold, fillerThreshold),
            globalCaverns, primaryScale);
    }
    
    private DensityController(
            DensityFunction main, DensityFunction globalCaverns, double primaryScale) {
        this.main = main;
        this.globalCaverns = globalCaverns;
        this.primaryScale = primaryScale;
        this.minValue = this.computeMinValue();
        this.maxValue = this.computeMaxValue();
    }

    private MainFunction unwrapMain() {
        DensityFunction f = this.main;
        while (f instanceof MarkerOrMarked m) {
            f = m.wrapped();
        }
        if (f instanceof MainFunction m) {
            return m;
        }
        throw new IllegalStateException("Unexpected value in controller: " + f);
    }

    @Override
    public double compute(final FunctionContext ctx) {
        final double d = squeeze(this.primaryScale * this.main.compute(ctx));
        return d < this.globalCaverns.minValue() ? d : Math.min(d, this.globalCaverns.compute(ctx));
    }

    private double computeMinValue() {
        return Math.min(this.globalCaverns.minValue(), squeeze(this.primaryScale * this.main.minValue()));
    }

    private double computeMaxValue() {
        return Math.max(this.globalCaverns.maxValue(), squeeze(this.primaryScale * this.main.maxValue()));
    }

    @Override
    public double minValue() {
        return this.minValue;
    }

    @Override
    public double maxValue() {
        return this.maxValue;
    }

    @Override
    public @NotNull DensityFunction mapAll(final Visitor visitor) {
        DensityFunction main = this.main;
        if (main instanceof MainFunction) {
            main = DensityFunctions.interpolated(DensityFunctions.blendDensity(main));
        }
        return new DensityController(main.mapAll(visitor), this.globalCaverns.mapAll(visitor), this.primaryScale);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    private static double squeeze(double d) {
        d = Mth.clamp(d, -1.0, 1.0);
        return d / 2.0 - d * d * d / 24.0;
    }

    // abstracted so it may be interpolated / cached
    protected static class MainFunction implements SimpleFunction {
        private static final MapCodec<MainFunction> CODEC =
            MapCodec.assumeMapUnsafe(CodecUtils.neverCodec());
        protected final DensityFunction surface;
        protected final DensityFunction entrances;
        protected final DensityCutoff upperCutoff;
        protected final DensityCutoff lowerCutoff;
        protected final DensityFunction undergroundCaverns;
        protected final DensityFunction undergroundFiller;
        protected final double surfaceThreshold;
        protected final double fillerThreshold;
        protected final double minValue;
        protected final double maxValue;

        protected MainFunction(
                DensityFunction surface,
                DensityFunction entrances,
                DensityCutoff upperCutoff,
                DensityCutoff lowerCutoff,
                DensityFunction undergroundCaverns,
                DensityFunction undergroundFiller,
                double surfaceThreshold,
                double fillerThreshold) {
            this.surface = surface;
            this.entrances = entrances;
            this.upperCutoff = upperCutoff;
            this.lowerCutoff = lowerCutoff;
            this.undergroundCaverns = undergroundCaverns;
            this.undergroundFiller = undergroundFiller;
            this.surfaceThreshold = surfaceThreshold;
            this.fillerThreshold = fillerThreshold;
            this.minValue = this.computeMinValue();
            this.maxValue = this.computeMaxValue();
        }

        @Override
        public double compute(FunctionContext ctx) {
            double d = this.surface.compute(ctx);
            if (d < this.surfaceThreshold) {
                if (d > this.entrances.minValue()) {
                    d = Math.min(d, this.entrances.compute(ctx));
                }
            } else {
                d = this.undergroundCaverns.compute(ctx);
                if (d < this.undergroundFiller.maxValue()) {
                    final double f = this.undergroundFiller.maxValue();
                    if (f >= this.fillerThreshold) {
                        d = Math.max(d, f);
                    }
                }
            }
            d = this.upperCutoff.transformUpper(d, ctx.blockY());
            d = this.lowerCutoff.transformLower(d, ctx.blockY());
            return d;
        }

        private double computeMinValue() {
            return -1000000; // todo
        }

        private double computeMaxValue() {
            return 1000000; // todo
        }

        @Override
        public double minValue() {
            return this.minValue;
        }

        @Override
        public double maxValue() {
            return this.maxValue;
        }

        @Override
        public @NotNull KeyDispatchDataCodec<MainFunction> codec() {
            return KeyDispatchDataCodec.of(CODEC);
        }
    }
}
