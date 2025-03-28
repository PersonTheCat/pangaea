package personthecat.pangaea.mixin.extras;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Decoder;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryDataLoader.RegistryData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.FunctionCodecs;
import personthecat.pangaea.serialization.codec.RegistryOpsExtras;
import personthecat.pangaea.resources.ResourceInfo;

import java.util.List;
import java.util.Map;

@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {

    // Temporary until we get real functions
    @WrapOperation(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Ljava/util/List;of([Ljava/lang/Object;)Ljava/util/List;", ordinal = 0))
    private static List<Object> insertEarlyRegistry(Object[] list, Operation<List<Object>> original) {
        final var densityTemplate = new RegistryData<>(PgRegistries.Keys.DENSITY_TEMPLATE, FunctionCodecs.DENSITY);
        return original.call(new Object[] { ArrayUtils.insert(0, list, densityTemplate) });
    }

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
