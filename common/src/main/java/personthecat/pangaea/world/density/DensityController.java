package personthecat.pangaea.world.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.SimpleFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.DensityFunctions.BlendDensity;
import net.minecraft.world.level.levelgen.DensityFunctions.MarkerOrMarked;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.serialization.codec.CodecUtils;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public class DensityController implements SimpleFunction {
    private static final DensityFunction DEFAULT_SURFACE = DensityFunctions.constant(1);
    private static final DensityFunction DEFAULT_ENTRANCES = DensityFunctions.constant(1000000);
    private static final DensityFunction DEFAULT_CAVES = DensityFunctions.constant(1);
    private static final DensityFunction DEFAULT_FILLER = DensityFunctions.constant(-1000000);
    private static final DensityCutoff DEFAULT_UPPER_CUTOFF = new DensityCutoff(255, 255, 0.1);
    private static final DensityCutoff DEFAULT_LOWER_CUTOFF = new DensityCutoff(-64, -64, 0.1);
    private static final MapCodec<DensityController> DIRECT_CODEC = codecOf(
        defaulted(DensityFunction.HOLDER_HELPER_CODEC, "surface", DEFAULT_SURFACE, c -> c.unwrapMain().surface),
        defaulted(DensityList.Min.LIST_CODEC, "entrances", DEFAULT_ENTRANCES, c -> c.unwrapMain().entrances),
        defaulted(DensityCutoff.CODEC, "upper_cutoff", DEFAULT_UPPER_CUTOFF, c -> c.unwrapMain().upperCutoff),
        defaulted(DensityCutoff.CODEC, "lower_cutoff", DEFAULT_LOWER_CUTOFF, c -> c.unwrapMain().lowerCutoff),
        defaulted(DensityList.Min.LIST_CODEC, "underground_caverns", DEFAULT_CAVES, c -> c.unwrapMain().undergroundCaverns),
        defaulted(DensityList.Max.LIST_CODEC, "underground_filler", DEFAULT_FILLER, c -> c.unwrapMain().undergroundFiller),
        defaulted(Codec.DOUBLE, "surface_threshold", 1.5625, c -> c.unwrapMain().surfaceThreshold),
        defaulted(Codec.DOUBLE, "filler_threshold", 0.03, c -> c.unwrapMain().fillerThreshold),
        defaulted(DensityList.Min.LIST_CODEC, "global_caverns", DEFAULT_CAVES, c -> c.globalCaverns),
        defaulted(Codec.DOUBLE, "primary_scale", 0.64, c -> c.primaryScale),
        DensityController::new
    );
    public static final MapCodec<DensityController> CODEC = FastNoiseDensity.as3dCodec(DIRECT_CODEC);
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

    public MainFunction unwrapMain() {
        DensityFunction f = this.main;
        while (true) {
            switch (f) {
                case MarkerOrMarked m -> f = m.wrapped();
                case BlendDensity b -> f = b.input();
                case MainFunction m -> {
                    return m;
                }
                case null, default -> throw new IllegalStateException("Unexpected value in controller: " + f);
            }
        }

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
        return Math.min(this.globalCaverns.maxValue(), squeeze(this.primaryScale * this.main.maxValue()));
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
    public static class MainFunction implements SimpleFunction {
        private static final MapCodec<MainFunction> CODEC =
            MapCodec.assumeMapUnsafe(CodecUtils.neverCodec());
        public final DensityFunction surface;
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
                if (d >= this.entrances.minValue()) {
                    d = Math.min(d, this.entrances.compute(ctx));
                }
            } else {
                d = this.undergroundCaverns.compute(ctx);
                if (d <= this.undergroundFiller.maxValue()) {
                    final double f = this.undergroundFiller.compute(ctx);
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
            final double surfaceEntrance =
                Math.min(this.surface.minValue(), this.entrances.minValue());
            final double cavernsFiller =
                Math.max(this.undergroundCaverns.minValue(), this.undergroundFiller.minValue());
            final double d = Math.min(surfaceEntrance, cavernsFiller);
            return Math.min(0, d);
        }

        private double computeMaxValue() {
            final double surfaceEntrance =
                Math.min(this.surface.maxValue(), this.entrances.maxValue());
            final double cavernsFiller =
                Math.max(this.undergroundCaverns.maxValue(), this.undergroundFiller.maxValue());
            final double d = Math.max(surfaceEntrance, cavernsFiller);
            return Math.max(0, d);
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
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return new MainFunction(
                this.surface.mapAll(visitor),
                this.entrances.mapAll(visitor),
                this.upperCutoff,
                this.lowerCutoff,
                this.undergroundCaverns.mapAll(visitor),
                this.undergroundFiller.mapAll(visitor),
                this.surfaceThreshold,
                this.fillerThreshold);
        }

        @Override
        public @NotNull KeyDispatchDataCodec<MainFunction> codec() {
            return KeyDispatchDataCodec.of(CODEC);
        }
    }
}
