package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.pangaea.extras.RegistryOpsExtras;

public final class PgCodecs {

    private PgCodecs() {}

    public static @NotNull String getActiveNamespace(DynamicOps<?> ops) {
        if (ops instanceof RegistryOps<?>) {
            final var resource = RegistryOpsExtras.getIncomingResource(ops);
            if (resource != null) {
                return resource.namespace();
            }
        }
        return "custom";
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
        for (final var entry : handle.entrySet()) {
            final var entryPath = entry.getKey().location().getPath();
            if (resource.isInDirectory(type.location(), entryPath + "/")) {
                return entry.getValue();
            }
        }
        return null;
    }

    // Workaround bizarre behavior of ConstantFloat#getMaxValue being value + 1
    public static Codec<FloatProvider> floatRangeFix(float min, float max) {
        return FloatProvider.CODEC.validate(fp -> validateRange(fp, min, max));
    }

    private static DataResult<FloatProvider> validateRange(FloatProvider fp, float min, float max) {
        final float maxValue = fp instanceof ConstantFloat c ? c.getMinValue() : fp.getMaxValue();
        if (fp.getMinValue() < min) {
            return DataResult.error(() -> "Value provider too low: min of " + min + ", given " + fp);
        } else if (maxValue > max) {
            return DataResult.error(() -> "Value provider too high: max of " + max + ", given " + fp);
        }
        return DataResult.success(fp);
    }
}
