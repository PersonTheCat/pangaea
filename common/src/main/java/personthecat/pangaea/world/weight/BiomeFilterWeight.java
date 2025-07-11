package personthecat.pangaea.world.weight;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import personthecat.catlib.data.BiomePredicate;
import personthecat.pangaea.world.level.PangaeaContext;

public record BiomeFilterWeight(BiomePredicate biomes) implements WeightFunction {
    public static final MapCodec<BiomeFilterWeight> CODEC =
        BiomePredicate.CODEC.fieldOf("biomes").xmap(BiomeFilterWeight::new, BiomeFilterWeight::biomes);

    @Override
    public double compute(PangaeaContext pg, FunctionContext fn) {
        return this.biomes.test(pg.noise.getApproximateBiome(pg.biomes, fn.blockX(), fn.blockZ())) ? 0 : NEVER;
    }

    @Override
    public MapCodec<BiomeFilterWeight> codec() {
        return CODEC;
    }
}
