package personthecat.pangaea.world.placer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.world.level.PangaeaContext;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public record ChanceBlockPlacer(double chance, BlockPlacer place) implements BlockPlacer {
    public static final MapCodec<ChanceBlockPlacer> CODEC = codecOf(
        defaulted(Codec.doubleRange(0, 1), "chance", 0.5, ChanceBlockPlacer::chance),
        field(BlockPlacer.CODEC, "place", ChanceBlockPlacer::place),
        ChanceBlockPlacer::new
    );

    @Override
    public boolean placeUnchecked(PangaeaContext ctx, int x, int y, int z) {
        if (ctx.rand.nextDouble() <= this.chance) {
            return this.place.placeUnchecked(ctx, x, y, z);
        }
        return false;
    }

    @Override
    public MapCodec<ChanceBlockPlacer> codec() {
        return CODEC;
    }
}