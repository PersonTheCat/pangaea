package personthecat.pangaea.world.placer;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import personthecat.pangaea.world.level.GenerationContext;

public record UnconditionalBlockPlacer(BlockState state) implements BlockPlacer {
    public static final MapCodec<UnconditionalBlockPlacer> CODEC =
        BlockState.CODEC.fieldOf("state").xmap(UnconditionalBlockPlacer::new, UnconditionalBlockPlacer::state);

    @Override
    public boolean place(WorldGenLevel level, WorldgenRandom rand, BlockPos pos) {
        level.setBlock(pos, this.state, 3);
        return true;
    }

    @Override
    public boolean placeUnchecked(GenerationContext ctx, int x, int y, int z) {
        ctx.setUnchecked(x, y, z, this.state);
        return true;
    }

    @Override
    public MapCodec<UnconditionalBlockPlacer> codec() {
        return CODEC;
    }
}