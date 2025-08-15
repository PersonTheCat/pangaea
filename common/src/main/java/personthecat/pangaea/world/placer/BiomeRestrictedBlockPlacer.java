package personthecat.pangaea.world.placer;

import com.mojang.serialization.MapCodec;
import personthecat.catlib.data.BiomePredicate;
import personthecat.pangaea.world.level.PangaeaContext;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record BiomeRestrictedBlockPlacer(BiomePredicate biomes, BlockPlacer place) implements BlockPlacer {
    public static final MapCodec<BiomeRestrictedBlockPlacer> CODEC = codecOf(
        field(BiomePredicate.CODEC, "biomes", BiomeRestrictedBlockPlacer::biomes),
        field(BlockPlacer.CODEC, "place", BiomeRestrictedBlockPlacer::place),
        BiomeRestrictedBlockPlacer::new
    );

    @Override
    public boolean place(PangaeaContext ctx, int x, int y, int z, int updates) {
        if (this.biomes.test(ctx.getApproximateBiome(x, z))) {
            return this.place.place(ctx, x, y, z, updates);
        }
        return false;
    }

    @Override
    public MapCodec<BiomeRestrictedBlockPlacer> codec() {
        return CODEC;
    }
}
