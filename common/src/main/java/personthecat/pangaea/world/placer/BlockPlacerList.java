package personthecat.pangaea.world.placer;

import com.mojang.serialization.MapCodec;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.List;

public record BlockPlacerList(List<BlockPlacer> place) implements BlockPlacer {
    public static final MapCodec<BlockPlacerList> CODEC =
        BlockPlacer.CODEC.listOf().fieldOf("place").xmap(BlockPlacerList::new, BlockPlacerList::place);

    @Override
    public boolean place(PangaeaContext ctx, int x, int y, int z, int updates) {
        for (final var placer : this.place) {
            if (placer.place(ctx, x, y, z, updates)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MapCodec<BlockPlacerList> codec() {
        return CODEC;
    }
}
