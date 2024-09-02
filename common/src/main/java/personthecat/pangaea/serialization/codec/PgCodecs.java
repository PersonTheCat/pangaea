package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.pangaea.resources.ResourceInfo;

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

    public static <T, C> @Nullable T inferFromPath(
            RegistryHandle<T> handle, ResourceKey<Registry<C>> type, DynamicOps<?> ops) {
        if (!(ops instanceof RegistryOps<?>)) {
            return null;
        }
        final var resource = RegistryOpsExtras.getIncomingResource(ops);
        if (resource == null) {
            return null;
        }
        final var typePath = type.location().getNamespace() + '/' + type.location().getPath();
        for (final var entry : handle.entrySet()) {
            final var entryPath = entry.getKey().location().getPath();
            if (resource.isInDirectory(typePath + '/' + entryPath)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
