package personthecat.pangaea.world.placer;

import com.mojang.serialization.MapCodec;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.List;

public record BlockPlacerList(List<BlockPlacer> place) implements BlockPlacer {
    public static final MapCodec<BlockPlacerList> CODEC =
        BlockPlacer.CODEC.listOf().fieldOf("place").xmap(BlockPlacerList::new, BlockPlacerList::place);

    @Override
    public boolean placeUnchecked(PangaeaContext ctx, int x, int y, int z) {
        for (final var placer : this.place) {
            if (placer.placeUnchecked(ctx, x, y ,z)) {
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
