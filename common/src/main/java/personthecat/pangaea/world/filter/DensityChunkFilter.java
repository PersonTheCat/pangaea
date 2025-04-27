package personthecat.pangaea.world.filter;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext;
import personthecat.pangaea.world.density.AutoWrapDensity;
import personthecat.pangaea.world.level.PangaeaContext;

public record DensityChunkFilter(DensityFunction density) implements ChunkFilter {
    public static final MapCodec<DensityChunkFilter> CODEC =
        AutoWrapDensity.HELPER_CODEC.fieldOf("density")
            .xmap(DensityChunkFilter::new, DensityChunkFilter::density);

    @Override
    public boolean test(PangaeaContext ctx, int x, int z) {
        return this.density.compute(new SinglePointContext(x, 64, z)) >= 0;
    }

    @Override
    public MapCodec<DensityChunkFilter> codec() {
        return CODEC;
    }
}
