package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.pangaea.resources.ResourceInfo;

public interface RegistryOpsExtras {
    ResourceInfo DEFAULT_INFO = new ResourceInfo(new ResourceLocation("custom", ""));

    @Nullable ResourceInfo pangaea$getIncomingResource();
    void pangaea$setIncomingResource(@Nullable ResourceInfo resource);

    static @Nullable ResourceInfo getIncomingResource(DynamicOps<?> ops) {
        return get(ops).pangaea$getIncomingResource();
    }

    static void setIncomingResource(DynamicOps<?> ops, @Nullable ResourceInfo resource) {
        get(ops).pangaea$setIncomingResource(resource);
    }

    static @NotNull String getActiveNamespace(DynamicOps<?> ops) {
        if (ops instanceof RegistryOps<?>) {
            final var resource = getIncomingResource(ops);
            if (resource != null) {
                return resource.namespace();
            }
        }
        return DEFAULT_INFO.namespace();
    }

    static RegistryOpsExtras get(DynamicOps<?> ops) {
        if (ops instanceof RegistryOpsExtras extras) {
            return extras;
        }
        if (!(ops instanceof RegistryOps<?>)) {
            throw new IllegalArgumentException("Tried to get extras from non-registry ops");
        }
        throw new IllegalStateException("Registry ops extras mixin was not applied");
    }
}
