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
import personthecat.pangaea.world.density.AutoWrapDensity;
import personthecat.pangaea.world.density.FastNoiseDensity;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public record ConditionConfiguration(
        DimensionPredicate dimensions,
        BiomePredicate biomes,
        DensityFunction bounds,
        double distanceFromBounds,
        double boundaryWidth) {

    private static final Codec<DensityFunction> BOUNDS_CODEC =
        AutoWrapDensity.wrap(FastNoiseDensity.DEFAULT_2D);

    public static final MapCodec<ConditionConfiguration> CODEC = codecOf(
        defaulted(DimensionPredicate.CODEC, "dimensions", DimensionPredicate.ALL_DIMENSIONS, ConditionConfiguration::dimensions),
        defaulted(BiomePredicate.CODEC, "biomes", BiomePredicate.ALL_BIOMES, ConditionConfiguration::biomes),
        defaulted(BOUNDS_CODEC, "bounds", DensityFunctions.zero(), ConditionConfiguration::bounds),
        defaulted(Codec.DOUBLE, "distance_from_bounds", 18.0, ConditionConfiguration::distanceFromBounds),
        defaulted(Codec.DOUBLE, "boundary_width", 10.0, ConditionConfiguration::boundaryWidth),
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
        return PositionalBiomePredicate.NEVER;
    }

    private boolean checkBiome(Holder<Biome> b) {
        return this.biomes.test(b);
    }

    private boolean checkPos(int x, int z) {
        return this.bounds.compute(new SinglePointContext(x, 64, z)) > 0;
    }
}
