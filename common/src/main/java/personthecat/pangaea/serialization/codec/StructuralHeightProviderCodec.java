package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.world.provider.DensityHeightProvider;
import personthecat.pangaea.world.provider.DensityOffsetHeightProvider;

import java.util.List;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.defaultType;

public class StructuralHeightProviderCodec extends MapCodec<HeightProvider> {
    public static final StructuralHeightProviderCodec INSTANCE = new StructuralHeightProviderCodec();
    private static final List<String> KEYS = List.of("density", "reference", "offset");

    private StructuralHeightProviderCodec() {}

    public static Codec<HeightProvider> wrap(Codec<HeightProvider> codec) {
        return defaultType(codec, INSTANCE.codec(),
            (p, o) -> Cfg.encodeStructuralHeight() && canBeStructural(p));
    }

    private static boolean canBeStructural(HeightProvider p) {
        return p instanceof DensityHeightProvider || p instanceof DensityOffsetHeightProvider;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return KEYS.stream().map(ops::createString);
    }

    @Override
    public <T> DataResult<HeightProvider> decode(DynamicOps<T> ops, MapLike<T> input) {
        var r = asParent(DensityOffsetHeightProvider.CODEC.decode(ops, input));
        if (r.isSuccess()) {
            return r;
        }
        r = asParent(DensityHeightProvider.CODEC.decode(ops, input));
        if (r.isSuccess()) {
            return r;
        }
        return DataResult.error(() -> "no structural fields or type present");
    }

    @SuppressWarnings("unchecked")
    private static <T> DataResult<HeightProvider> asParent(DataResult<? extends HeightProvider> r) {
        return (DataResult<HeightProvider>) r;
    }

    @Override
    public <T> RecordBuilder<T> encode(HeightProvider input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        if (input instanceof DensityOffsetHeightProvider o) {
            return DensityOffsetHeightProvider.CODEC.encode(o, ops, prefix);
        } else if (input instanceof DensityHeightProvider d) {
            return DensityHeightProvider.CODEC.encode(d, ops, prefix);
        }
        return prefix.withErrorsFrom(DataResult.error(() -> "not a structural type: " + input));
    }
}
