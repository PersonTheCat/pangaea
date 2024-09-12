package personthecat.pangaea.mixin.fabric;

import net.fabricmc.fabric.impl.registry.sync.DynamicRegistriesImpl;
import net.minecraft.resources.RegistryDataLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.registry.PgRegistries;

@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {

    // Temporary until we get real functions
    @SuppressWarnings("UnstableApiUsage")
    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void flagEarlyRegistryAsCustom(CallbackInfo ci) {
        DynamicRegistriesImpl.FABRIC_DYNAMIC_REGISTRY_KEYS.add(PgRegistries.Keys.DENSITY_TEMPLATE);
    }
}
