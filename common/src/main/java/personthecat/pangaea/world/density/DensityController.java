package personthecat.pangaea.world.density;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.SimpleFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.serialization.codec.CodecUtils;

public class DensityController implements SimpleFunction {
    private final DensityFunction main;
    private final DensityFunction globalCaverns;
    private final double thickness;
    private final double minValue;
    private final double maxValue;

    public DensityController(
            DensityFunction surface, DensityFunction entrances, DensityCutoff upperCutoff, DensityCutoff lowerCutoff,
            DensityFunction undergroundCaverns, DensityFunction undergroundFiller, double surfaceThreshold,
            double fillerThreshold, DensityFunction globalCaverns, double thickness) {
        this(new MainFunction(
                surface, entrances, upperCutoff, lowerCutoff, undergroundCaverns,
                undergroundFiller, surfaceThreshold, fillerThreshold),
            globalCaverns, thickness);
    }
    
    private DensityController(
            DensityFunction main, DensityFunction globalCaverns, double thickness) {
        this.main = main;
        this.globalCaverns = globalCaverns;
        this.thickness = thickness;
        this.minValue = this.computeMinValue();
        this.maxValue = this.computeMaxValue();
    }

    @Override
    public double compute(final FunctionContext ctx) {
        final double d = squeeze(this.thickness * this.main.compute(ctx));
        return d < this.globalCaverns.minValue() ? d : Math.min(d, this.globalCaverns.compute(ctx));
    }

    private double computeMinValue() {
        return Math.min(this.globalCaverns.minValue(), squeeze(this.thickness * this.main.minValue()));
    }

    private double computeMaxValue() {
        return Math.min(this.globalCaverns.maxValue(), squeeze(this.thickness * this.main.maxValue()));
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
        return new DensityController(main.mapAll(visitor), this.globalCaverns.mapAll(visitor), this.thickness);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return KeyDispatchDataCodec.of(CodecUtils.neverMapCodec());
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
