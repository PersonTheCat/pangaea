package personthecat.pangaea.world.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.Mth;
import personthecat.catlib.data.Range;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record SpawnDistanceChunkFilter(Range distance, double chance, int fade) implements ChunkFilter {
    private static final Codec<Range> RANGE_UP_CODEC =
        Range.CODEC.xmap(SpawnDistanceChunkFilter::rangeUp, Function.identity());
    public static final MapCodec<SpawnDistanceChunkFilter> CODEC = codecOf(
        field(RANGE_UP_CODEC, "distance", SpawnDistanceChunkFilter::distance),
        defaulted(Codec.doubleRange(0, 1), "chance", 0.25, SpawnDistanceChunkFilter::chance),
        defaulted(Codec.intRange(0, Integer.MAX_VALUE), "fade", 0, SpawnDistanceChunkFilter::fade),
        SpawnDistanceChunkFilter::new
    );

    private static Range rangeUp(Range r) {
        return r.diff() == 0 ? Range.of(r.min(), Integer.MAX_VALUE) : r;
    }

    @Override
    public boolean test(PangaeaContext ctx, int x, int z) {
        final int d = (int) Math.sqrt((x * x) + (z * z));

        if (this.distance.contains(d + this.fade)) {
            return ctx.rand.nextDouble() <= this.getProbability(d);
        }
        return false;
    }

    private double getProbability(int d) {
        return this.fade == 0 ? this.chance : this.doFade(d);
    }

    private double doFade(int d) {
        final int min = this.distance.min();
        final int max = this.distance.max();
        final int fade = this.fade;

        if (d > max) { // fade out
            return this.chance * Mth.map(d, max, max + fade, 1, 0);
        } else if (d < min) { // fade in
            return this.chance * Mth.map(d, min - fade, min, 0, 1);
        }
        return this.chance;
    }

    @Override
    public MapCodec<SpawnDistanceChunkFilter> codec() {
        return CODEC;
    }
}
