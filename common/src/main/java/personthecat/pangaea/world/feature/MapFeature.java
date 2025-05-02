package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import personthecat.pangaea.world.level.PangaeaContext;

public abstract class MapFeature<FC extends MapFeatureConfiguration> extends PangaeaFeature<FC> {

    protected MapFeature(Codec<FC> codec) {
        super(codec);
    }

    @Override
    protected final boolean place(PangaeaContext ctx, FC cfg, BlockPos pos, Border border) {
        final int cX = ctx.chunkX;
        final int cZ = ctx.chunkZ;
        final int r = cfg.chunkRadius;
        final var strict = cfg.strictOrigin;
        final var predicate = strict ? cfg.conditions.buildPredicate() : PositionalBiomePredicate.ALWAYS;

        for (int x = cX - r; x < cX + r; x++) {
            for (int z = cZ - r; z < cZ +r; z++) {
                final var pos2 = new ChunkPos(x, z);
                final int aX = (x << 4) + 8;
                final int aZ = (z << 4) + 8;
                ctx.rand.setLargeFeatureSeed(ctx.seed + ctx.featureIndex.get(), pos2.x, pos2.z);
                ctx.targetPos.at(aX, aZ);
                if (strict) {
                    final var biome = ctx.noise.getApproximateBiome(ctx.biomes, aX, aZ);
                    if (!predicate.test(biome, aX, aZ)) {
                        continue;
                    }
                }
                this.place(ctx, cfg, pos2, border);
            }
        }
        return true;
    }

    protected abstract void place(PangaeaContext ctx, FC cfg, ChunkPos pos, Border border);
}
