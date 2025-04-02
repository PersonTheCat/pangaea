package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import personthecat.pangaea.world.map.GenerationContext;

public abstract class MapFeature<FC extends MapFeatureConfiguration> extends PangaeaFeature<FC> {

    protected MapFeature(Codec<FC> codec) {
        super(codec);
    }

    @Override
    protected final boolean place(GenerationContext ctx, FC cfg, BlockPos pos, Border border) {
        final int cX = ctx.chunkX;
        final int cZ = ctx.chunkZ;
        final int r = cfg.chunkRadius;

        for (int x = cX - r; x < cX + r; x++) {
            for (int z = cZ - r; z < cZ +r; z++) {
                this.place(ctx, cfg, new ChunkPos(x, z), border);
            }
        }
        return true;
    }

    protected abstract void place(GenerationContext ctx, FC cfg, ChunkPos pos, Border border);
}
