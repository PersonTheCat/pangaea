package personthecat.pangaea.world.placer;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.pangaea.world.level.GenerationContext;

public record UnconditionalBlockPlacer(BlockState place) implements BlockPlacer {
    public static final MapCodec<UnconditionalBlockPlacer> CODEC =
        BlockState.CODEC.fieldOf("place").xmap(UnconditionalBlockPlacer::new, UnconditionalBlockPlacer::place);

    @Override
    public boolean placeUnchecked(GenerationContext ctx, int x, int y, int z) {
        ctx.setUnchecked(x, y, z, this.place);
        return true;
    }

    @Override
    public MapCodec<UnconditionalBlockPlacer> codec() {
        return CODEC;
    }
}