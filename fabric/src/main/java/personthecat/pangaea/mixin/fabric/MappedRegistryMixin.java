package personthecat.pangaea.mixin.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.function.BiConsumer;

// Temporary workaround for reference bind order bug on Fabric
@Mixin(MappedRegistry.class)
public class MappedRegistryMixin<T> {

    @WrapOperation(method = "freeze", at = @At(value = "INVOKE", target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"))
    private void skipRebindValues(
            Map<T, Holder.Reference<T>> instance, BiConsumer<T, Holder.Reference<T>> f, Operation<Void> original) {
        // skip operation
    }
}
