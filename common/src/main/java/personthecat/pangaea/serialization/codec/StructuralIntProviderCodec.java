package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviderType;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.world.provider.DensityIntProvider;

import java.util.List;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.defaultType;

public class StructuralIntProviderCodec extends MapCodec<IntProvider> {
    public static final StructuralIntProviderCodec INSTANCE = new StructuralIntProviderCodec();
    public static final IntProviderType<IntProvider> TYPE = () -> INSTANCE;
    private static final List<String> KEYS = List.of("density");

    private StructuralIntProviderCodec() {}

    public static Codec<IntProvider> wrap(Codec<IntProvider> codec) {
        return defaultType(codec, INSTANCE.codec(),
            (p, o) -> Cfg.encodeStructuralIntProviders() && canBeStructural(p));
    }

    private static boolean canBeStructural(IntProvider p) {
        return p instanceof DensityIntProvider;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return KEYS.stream().map(ops::createString);
    }

    @Override
    public <T> DataResult<IntProvider> decode(DynamicOps<T> ops, MapLike<T> input) {
        if (input.get("density") != null) {
            return asParent(DensityIntProvider.CODEC.decode(ops, input));
        }
        return DataResult.error(() -> "no structural fields or type present");
    }

    @SuppressWarnings("unchecked")
    private static <T> DataResult<IntProvider> asParent(DataResult<? extends IntProvider> result) {
        return (DataResult<IntProvider>) result;
    }

    @Override
    public <T> RecordBuilder<T> encode(IntProvider input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        if (input instanceof DensityIntProvider d) {
            return DensityIntProvider.CODEC.encode(d, ops, prefix);
        }
        return prefix.withErrorsFrom(DataResult.error(() -> "not a structural type: " + input));
    }
}
