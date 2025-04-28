package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.world.filter.ChanceChunkFilter;
import personthecat.pangaea.world.filter.ChunkFilter;
import personthecat.pangaea.world.filter.ClusterChunkFilter;
import personthecat.pangaea.world.filter.DensityChunkFilter;
import personthecat.pangaea.world.filter.FastNoiseChunkFilter;
import personthecat.pangaea.world.filter.IntervalChunkFilter;
import personthecat.pangaea.world.filter.SpawnDistanceChunkFilter;

import java.util.List;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.defaultType;

public class StructuralChunkFilterCodec extends MapCodec<ChunkFilter> {
    public static final StructuralChunkFilterCodec INSTANCE = new StructuralChunkFilterCodec();
    private static final List<String> KEYS =
        List.of("density", "cluster_chance", "noise", "interval", "distance", "chance", "fade", "seed");

    private StructuralChunkFilterCodec() {}

    public static Codec<ChunkFilter> wrap(Codec<ChunkFilter> codec) {
        return defaultType(codec, INSTANCE.codec(),
            (f, o) -> Cfg.encodeStructuralChunkFilters() && canBeStructural(f));
    }

    private static boolean canBeStructural(ChunkFilter f) {
        return f instanceof DensityChunkFilter
            || f instanceof ClusterChunkFilter
            || f instanceof FastNoiseChunkFilter
            || f instanceof IntervalChunkFilter
            || f instanceof SpawnDistanceChunkFilter
            || f instanceof ChanceChunkFilter;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return KEYS.stream().map(ops::createString);
    }

    @Override
    public <T> DataResult<ChunkFilter> decode(DynamicOps<T> ops, MapLike<T> input) {
        if (input.get("density") != null) {
            return asParent(DensityChunkFilter.CODEC.decode(ops, input));
        }
        if (input.get("cluster_chance") != null) {
            return asParent(ClusterChunkFilter.CODEC.decode(ops, input));
        }
        if (input.get("noise") != null) {
            return asParent(FastNoiseChunkFilter.CODEC.decode(ops, input));
        }
        if (input.get("interval") != null) {
            return asParent(IntervalChunkFilter.CODEC.decode(ops, input));
        }
        if (input.get("distance") != null) {
            return asParent(SpawnDistanceChunkFilter.CODEC.decode(ops, input));
        }
        if (input.get("chance") != null) {
            return asParent(ChanceChunkFilter.CODEC.decode(ops, input));
        }
        return DataResult.error(() -> "no structural fields or type present");
    }

    @SuppressWarnings("unchecked")
    private static DataResult<ChunkFilter> asParent(DataResult<? extends ChunkFilter> result) {
        return (DataResult<ChunkFilter>) result;
    }

    @Override
    public <T> RecordBuilder<T> encode(ChunkFilter input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        if (input instanceof DensityChunkFilter d) {
            return DensityChunkFilter.CODEC.encode(d, ops, prefix);
        }
        if (input instanceof ClusterChunkFilter c) {
            return ClusterChunkFilter.CODEC.encode(c, ops, prefix);
        }
        if (input instanceof FastNoiseChunkFilter f) {
            return FastNoiseChunkFilter.CODEC.encode(f, ops, prefix);
        }
        if (input instanceof IntervalChunkFilter i) {
            return IntervalChunkFilter.CODEC.encode(i, ops, prefix);
        }
        if (input instanceof SpawnDistanceChunkFilter s) {
            return SpawnDistanceChunkFilter.CODEC.encode(s, ops, prefix);
        }
        if (input instanceof ChanceChunkFilter c) {
            return ChanceChunkFilter.CODEC.encode(c, ops, prefix);
        }
        return prefix.withErrorsFrom(DataResult.error(() -> "not a structural type: " + input));
    }
}
