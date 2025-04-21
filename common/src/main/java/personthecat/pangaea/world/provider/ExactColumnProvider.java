package personthecat.pangaea.world.provider;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.world.level.GenerationContext;

public record ExactColumnProvider(VerticalAnchor anchor) implements ColumnProvider {
    public static final MapCodec<ExactColumnProvider> CODEC =
        MapCodec.assumeMapUnsafe(VerticalAnchor.CODEC)
            .xmap(ExactColumnProvider::new, ExactColumnProvider::anchor);

    @Override
    public ColumnBounds getColumn(GenerationContext ctx, int x, int z) {
        final int y = this.anchor.resolveY(ctx);
        return ColumnBounds.create(y, y + 1, 0);
    }

    @Override
    public MapCodec<? extends ColumnProvider> codec() {
        return CODEC;
    }
}
