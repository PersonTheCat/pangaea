package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.KeyDispatchCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.pangaea.resources.ResourceInfo;

import java.util.function.Function;

public final class PgCodecs {
    private static final ResourceInfo DEFAULT_INFO = new ResourceInfo(new ResourceLocation("custom", ""));

    private PgCodecs() {}

    public static @NotNull String getActiveNamespace(DynamicOps<?> ops) {
        if (ops instanceof RegistryOps<?>) {
            final var resource = RegistryOpsExtras.getIncomingResource(ops);
            if (resource != null) {
                return resource.namespace();
            }
        }
        return DEFAULT_INFO.namespace();
    }

    public static <A, E> DispatchBuilder<A, E> inferredDispatch(RegistryHandle<A> handle, ResourceKey<Registry<E>> type) {
        return (typeGetter, codecGetter) -> {
            final var result = handle.codec().dispatch(typeGetter, codecGetter);
            KeyDispatchCodecExtras.setDefaultType(unwrapDispatch(result), ops -> inferFromPath(handle, type, ops));
            return result;
        };
    }

    public static <K, V> KeyDispatchCodec<K, V> unwrapDispatch(Codec<V> codec) {
        if (codec instanceof MapCodec.MapCodecCodec<V> mapCodecCodec) {
            if (mapCodecCodec.codec() instanceof KeyDispatchCodec<K, V> keyDispatchCodec) {
                return keyDispatchCodec;
            }
        }
        throw new IllegalArgumentException("Not a KeyDispatchCodec: " + codec);
    }

    public static <T, C> @Nullable String inferFromPath(
            RegistryHandle<T> handle, ResourceKey<Registry<C>> type, DynamicOps<?> ops) {
        if (!(ops instanceof RegistryOps<?>)) {
            return null;
        }
        final var resource = RegistryOpsExtras.getIncomingResource(ops);
        if (resource == null) {
            return null;
        }
        final var typePath = type.location().getPath();
        for (final var entry : handle.entrySet()) {
            final var entryPath = entry.getKey().location().getPath();
            if (resource.isInDirectory(typePath + '/' + entryPath)) {
                return entry.getKey().location().toString();
            }
        }
        return null;
    }

    public interface DispatchBuilder<A, E> {
        Codec<E> apply(Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec);
    }
}
