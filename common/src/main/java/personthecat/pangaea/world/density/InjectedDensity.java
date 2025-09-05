package personthecat.pangaea.world.density;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.SimpleFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.pangaea.serialization.codec.DensityHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.nullable;

public class InjectedDensity implements SimpleFunction {
    private static final DensityFunction DEFAULT_SURFACE = DensityFunctions.constant(1);
    private static final DensityFunction DEFAULT_ENTRANCES = DensityFunctions.constant(1000000);
    private static final DensityFunction DEFAULT_CAVES = DensityFunctions.constant(1);
    private static final DensityFunction DEFAULT_FILLER = DensityFunctions.constant(-1000000);
    private static final DensityCutoff DEFAULT_UPPER_CUTOFF = new DensityCutoff(255, 255, 0.1);
    private static final DensityCutoff DEFAULT_LOWER_CUTOFF = new DensityCutoff(-64, -64, 0.1);
    private static final double DEFAULT_SURFACE_THRESHOLD = 1.5625;
    private static final double DEFAULT_FILLER_THRESHOLD = 0.03;
    private static final double DEFAULT_THICKNESS = 0.64;

    private static final MapCodec<InjectedDensity> DIRECT_CODEC = codecOf(
        defaulted(easyList(DensityHelper.CODEC), "entrances", List.of(), d -> d.entrances),
        defaulted(easyList(DensityHelper.CODEC), "underground_caverns", List.of(), d -> d.undergroundCaverns),
        defaulted(easyList(DensityHelper.CODEC), "underground_filler", List.of(), d -> d.undergroundFiller),
        defaulted(easyList(DensityHelper.CODEC), "global_caverns", List.of(), d -> d.globalCaverns),
        nullable(DensityHelper.CODEC, "surface", d -> d.surface),
        nullable(DensityCutoff.CODEC, "upper_cutoff", d -> d.upperCutoff),
        nullable(DensityCutoff.CODEC, "lower_cutoff", d -> d.lowerCutoff),
        nullable(Codec.DOUBLE, "surface_threshold", d -> d.surfaceThreshold),
        nullable(Codec.DOUBLE, "filler_threshold", d -> d.fillerThreshold),
        nullable(Codec.DOUBLE, "thickness", d -> d.thickness),
        InjectedDensity::new
    );
    public static final MapCodec<InjectedDensity> CODEC = FastNoiseDensity.as3dCodec(DIRECT_CODEC);

    public final List<DensityFunction> entrances = new ArrayList<>();
    public final List<DensityFunction> undergroundCaverns = new ArrayList<>();
    public final List<DensityFunction> undergroundFiller = new ArrayList<>();
    public final List<DensityFunction> globalCaverns = new ArrayList<>();
    public volatile @Nullable DensityFunction surface;
    public volatile @Nullable DensityCutoff upperCutoff;
    public volatile @Nullable DensityCutoff lowerCutoff;
    public volatile @Nullable Double surfaceThreshold;
    public volatile @Nullable Double fillerThreshold;
    public volatile @Nullable Double thickness;
    private final Supplier<DensityFunction> optimized;

    private InjectedDensity(
        List<DensityFunction> entrances,
        List<DensityFunction> undergroundCaverns,
        List<DensityFunction> undergroundFiller,
        List<DensityFunction> globalCaverns,
        @Nullable DensityFunction surface,
        @Nullable DensityCutoff upperCutoff,
        @Nullable DensityCutoff lowerCutoff,
        @Nullable Double surfaceThreshold,
        @Nullable Double fillerThreshold,
        @Nullable Double thickness
    ) {
        this.entrances.addAll(entrances);
        this.undergroundCaverns.addAll(undergroundCaverns);
        this.undergroundFiller.addAll(undergroundFiller);
        this.globalCaverns.addAll(globalCaverns);
        this.surface = surface;
        this.upperCutoff = upperCutoff;
        this.lowerCutoff = lowerCutoff;
        this.surfaceThreshold = surfaceThreshold;
        this.fillerThreshold = fillerThreshold;
        this.thickness = thickness;
        this.optimized = Suppliers.memoize(this::optimize);
    }

    @Override
    public double compute(FunctionContext ctx) {
        return this.optimized.get().compute(ctx);
    }

    public DensityFunction optimize() {
        return new DensityController(
            getOrElse(this.surface, DEFAULT_SURFACE),
            DensityList.min(this.entrances, DEFAULT_ENTRANCES),
            getOrElse(this.upperCutoff, DEFAULT_UPPER_CUTOFF),
            getOrElse(this.lowerCutoff, DEFAULT_LOWER_CUTOFF),
            DensityList.min(this.undergroundCaverns, DEFAULT_CAVES),
            DensityList.max(this.undergroundFiller, DEFAULT_FILLER),
            getOrElse(this.surfaceThreshold, DEFAULT_SURFACE_THRESHOLD),
            getOrElse(this.fillerThreshold, DEFAULT_FILLER_THRESHOLD),
            DensityList.min(this.globalCaverns, DEFAULT_CAVES),
            getOrElse(this.thickness, DEFAULT_THICKNESS)
        );
    }

    @Override
    public double minValue() {
        return this.optimized.get().minValue();
    }

    @Override
    public double maxValue() {
        return this.optimized.get().maxValue();
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return this.optimized.get().codec();
    }

    private static <T> T getOrElse(@Nullable T t, T orElse) {
         return t != null ? t : orElse;
    }
}
