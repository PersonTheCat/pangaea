package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import personthecat.pangaea.extras.WorldGenRegionExtras;
import personthecat.pangaea.world.level.PangaeaContext;

public abstract class PangaeaFeature<FC extends PangaeaFeatureConfiguration> extends Feature<FC> {

    protected PangaeaFeature(Codec<FC> codec) {
        super(codec);
    }

    @Override
    public final boolean place(FeaturePlaceContext<FC> ctx) {
        return false;
    }

    @Override
    public final boolean place(FC cfg, WorldGenLevel level, ChunkGenerator gen, RandomSource rand, BlockPos pos) {
        final var ctx = WorldGenRegionExtras.getPangaeaContext(level);
        if (!cfg.conditions.dimensions().test(ctx.chunk)) {
            return false;
        }
        final var predicate = cfg.conditions.buildPredicate();
        if (!predicate.isDisabled()) {
            if (!ctx.noise.chunkMatches(ctx.biomes, predicate, ctx.chunkX, ctx.chunkZ)) {
                return false;
            }
        }
        final var r = cfg.conditions.distanceFromBounds();
        final var t = r + cfg.conditions.boundaryWidth();
        final var border = Border.forPredicate(ctx, predicate, r, t);
        return this.place(ctx, cfg, pos, border);
    }

    protected abstract boolean place(PangaeaContext ctx, FC cfg, BlockPos pos, Border border);
}
