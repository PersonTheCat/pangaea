package personthecat.pangaea.world.provider;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.world.level.PangaeaContext;

public record ExactColumnProvider(VerticalAnchor anchor) implements ColumnProvider {
    public static final MapCodec<ExactColumnProvider> CODEC =
        MapCodec.assumeMapUnsafe(VerticalAnchor.CODEC)
            .xmap(ExactColumnProvider::new, ExactColumnProvider::anchor);

    @Override
    public ColumnBounds getColumn(PangaeaContext ctx, int x, int z) {
        ctx.targetPos.at(x, z);
        final int y = this.anchor.resolveY(ctx);
        return ColumnBounds.create(y, y + 1, 0);
    }

    @Override
    public MapCodec<? extends ColumnProvider> codec() {
        return CODEC;
    }
}
