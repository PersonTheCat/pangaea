package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext;
import net.minecraft.world.level.levelgen.DensityFunctions;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.data.DimensionPredicate;
import personthecat.catlib.serialization.codec.CapturingCodec.Receiver;
import personthecat.pangaea.world.density.AutoWrapDensity;

import static personthecat.catlib.serialization.codec.CapturingCodec.receive;
import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaultTry;

public record ConditionConfiguration(
        DimensionPredicate dimensions,
        BiomePredicate biomes,
        DensityFunction bounds,
        double distanceFromBounds,
        double boundaryWidth) {

    private static final Receiver<DimensionPredicate> DEFAULT_DIMENSIONS =
        receive("dimensions", DimensionPredicate.ALL_DIMENSIONS);
    private static final Receiver<BiomePredicate> DEFAULT_BIOMES =
        receive("biomes", BiomePredicate.ALL_BIOMES);
    private static final Receiver<DensityFunction> DEFAULT_BOUNDS =
        receive("bounds", DensityFunctions.constant(1));
    private static final Receiver<Double> DEFAULT_DISTANCE =
        receive("distance_from_bounds", 8.0);
    private static final Receiver<Double> DEFAULT_WIDTH =
        receive("boundary_width", 16.0);

    public static final MapCodec<ConditionConfiguration> CODEC = codecOf(
        defaultTry(DimensionPredicate.CODEC, "dimensions", DEFAULT_DIMENSIONS, ConditionConfiguration::dimensions),
        defaultTry(BiomePredicate.CODEC, "biomes", DEFAULT_BIOMES, ConditionConfiguration::biomes),
        defaultTry(AutoWrapDensity.HELPER_CODEC, "bounds", DEFAULT_BOUNDS, ConditionConfiguration::bounds),
        defaultTry(Codec.DOUBLE, "distance_from_bounds", DEFAULT_DISTANCE, ConditionConfiguration::distanceFromBounds),
        defaultTry(Codec.DOUBLE, "boundary_width", DEFAULT_WIDTH, ConditionConfiguration::boundaryWidth),
        ConditionConfiguration::new
    );

    public boolean hasBiomes() {
        return !this.biomes.isExplicitAll();
    }

    public boolean hasBounds() {
        return this.bounds != DensityFunctions.zero();
    }

    public PositionalBiomePredicate buildPredicate() {
        if (this.hasBiomes() || this.hasBounds()) {
            return (b, x, z) -> this.checkBiome(b) && this.checkPos(x, z);
        }
        return PositionalBiomePredicate.ALWAYS;
    }

    private boolean checkBiome(Holder<Biome> b) {
        return this.biomes.test(b);
    }

    private boolean checkPos(int x, int z) {
        return this.bounds.compute(new SinglePointContext(x, 64, z)) > 0;
    }
}
