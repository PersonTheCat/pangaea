package personthecat.pangaea.mixin.extras;

import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import personthecat.pangaea.serialization.codec.RegistryOpsExtras;
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
}
