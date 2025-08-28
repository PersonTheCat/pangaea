package personthecat.pangaea.world.placer;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import personthecat.pangaea.world.level.PangaeaContext;

public record SurfaceBlockPlacer(BlockState alternate) implements BlockPlacer {
    public static final MapCodec<SurfaceBlockPlacer> CODEC =
        BlockState.CODEC.fieldOf("alternate").xmap(SurfaceBlockPlacer::new, SurfaceBlockPlacer::alternate);

    @Override
    public boolean place(PangaeaContext ctx, int x, int y, int z, int updates) {
        if (ctx.chunk.getStatus().isOrAfter(ChunkStatus.NOISE)) {
            ctx.setBlock(x, y, z, this.alternate, updates);
        } else {
            ctx.setBlock(x, y, z, Blocks.STONE.defaultBlockState(), updates);
        }
        return true;
    }

    @Override
    public MapCodec<SurfaceBlockPlacer> codec() {
        return CODEC;
    }
}
