package personthecat.pangaea.world.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.catlib.data.Range;
import personthecat.pangaea.util.Utils;
import personthecat.pangaea.world.level.PangaeaContext;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record SpawnDistanceChunkFilter(Range distance, double chance, int fade) implements ChunkFilter {
    public static final MapCodec<SpawnDistanceChunkFilter> CODEC = codecOf(
        field(Range.RANGE_UP_CODEC, "distance", SpawnDistanceChunkFilter::distance),
        defaulted(Codec.doubleRange(0, 1), "chance", 0.25, SpawnDistanceChunkFilter::chance),
        defaulted(Codec.intRange(0, Integer.MAX_VALUE), "fade", 0, SpawnDistanceChunkFilter::fade),
        SpawnDistanceChunkFilter::new
    );

    @Override
    public boolean test(PangaeaContext ctx, int x, int z) {
        final int d = (int) Math.sqrt((x * x) + (z * z));
        return Utils.checkDistanceWithFade(ctx.rand, this.distance, d, this.chance, this.fade);
    }

    @Override
    public MapCodec<SpawnDistanceChunkFilter> codec() {
        return CODEC;
    }
}
