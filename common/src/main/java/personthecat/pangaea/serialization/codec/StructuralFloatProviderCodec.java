package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviderType;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.world.provider.DensityFloatProvider;

import java.util.List;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.defaultType;

public class StructuralFloatProviderCodec extends MapCodec<FloatProvider> {
    public static final StructuralFloatProviderCodec INSTANCE = new StructuralFloatProviderCodec();
    public static final FloatProviderType<FloatProvider> TYPE = () -> INSTANCE;
    private static final List<String> KEYS = List.of("density");

    private StructuralFloatProviderCodec() {}

    public static Codec<FloatProvider> wrap(Codec<FloatProvider> codec) {
        return defaultType(codec, INSTANCE,
            (o, p) -> Cfg.encodeStructuralFloatProviders() && canBeStructural(p));
    }

    private static boolean canBeStructural(FloatProvider p) {
        return p instanceof DensityFloatProvider;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return KEYS.stream().map(ops::createString);
    }

    @Override
    public <T> DataResult<FloatProvider> decode(DynamicOps<T> ops, MapLike<T> input) {
        if (input.get("density") != null) {
            return asParent(DensityFloatProvider.CODEC.decode(ops, input));
        }
        return DataResult.error(() -> "no structural fields or type present");
    }

    @SuppressWarnings("unchecked")
    private static DataResult<FloatProvider> asParent(DataResult<? extends FloatProvider> result) {
        return (DataResult<FloatProvider>) result;
    }

    @Override
    public <T> RecordBuilder<T> encode(FloatProvider input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        if (input instanceof DensityFloatProvider d) {
            return DensityFloatProvider.CODEC.encode(d, ops, prefix);
        }
        return prefix.withErrorsFrom(DataResult.error(() -> "not a structural type: " + input));
    }
}
