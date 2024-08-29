package personthecat.pangaea.mixin;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Decoder;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.serialization.codec.RegistryOpsExtras;
import personthecat.pangaea.resources.ResourceInfo;

import java.util.Map;

@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {

    @Inject(
        method = "loadContentsFromManager",
        at = @At(value = "INVOKE_ASSIGN", target = "Ljava/util/Map$Entry;getKey()Ljava/lang/Object;"))
    private static <E> void consumeIncomingResource(
            ResourceManager resourceManager, RegistryOps.RegistryInfoLookup registryInfoLookup,
            WritableRegistry<E> writableRegistry, Decoder<E> decoder, Map<ResourceKey<?>, Exception> map,
            CallbackInfo ci, @Local Map.Entry<ResourceLocation, Resource> entry, @Local RegistryOps<JsonElement> ops) {
        RegistryOpsExtras.setIncomingResource(ops, new ResourceInfo(entry.getKey()));
    }

    @Inject(method = "loadContentsFromManager", at = @At("TAIL"))
    private static <E> void disposeIncomingResource(
            ResourceManager resourceManager, RegistryOps.RegistryInfoLookup registryInfoLookup,
            WritableRegistry<E> writableRegistry, Decoder<E> decoder, Map<ResourceKey<?>, Exception> map,
            CallbackInfo ci, @Local RegistryOps<JsonElement> ops) {
        RegistryOpsExtras.setIncomingResource(ops, null);
    }
}
