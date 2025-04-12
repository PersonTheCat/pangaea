package personthecat.pangaea.extras;

import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;
import personthecat.pangaea.resources.ResourceInfo;

public interface RegistryOpsExtras {
    @Nullable ResourceInfo pangaea$getIncomingResource();
    void pangaea$setIncomingResource(@Nullable ResourceInfo resource);

    static @Nullable ResourceInfo getIncomingResource(DynamicOps<?> ops) {
        return get(ops).pangaea$getIncomingResource();
    }

    static void setIncomingResource(DynamicOps<?> ops, @Nullable ResourceInfo resource) {
        get(ops).pangaea$setIncomingResource(resource);
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
