package personthecat.pangaea.mixin.extras;

import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.pangaea.extras.RegistryOpsExtras;
import personthecat.pangaea.resources.ResourceInfo;

@Mixin(RegistryOps.class)
public class RegistryOpsMixin implements RegistryOpsExtras {
    @Unique
    private @Nullable ResourceInfo pangaea$incomingResource;

    @Override
    public @Nullable ResourceInfo pangaea$getIncomingResource() {
        return this.pangaea$incomingResource;
    }

    @Override
    public void pangaea$setIncomingResource(@Nullable ResourceInfo resource) {
        this.pangaea$incomingResource = resource;
    }

    @Inject(method = "withParent", at = @At("RETURN"))
    private <U> void copyIncomingResource(DynamicOps<U> uOps, CallbackInfoReturnable<RegistryOps<U>> cir) {
        RegistryOpsExtras.setIncomingResource(cir.getReturnValue(), this.pangaea$incomingResource);
    }
}
