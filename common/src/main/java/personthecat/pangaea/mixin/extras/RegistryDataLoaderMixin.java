package personthecat.pangaea.mixin.extras;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryDataLoader.RegistryData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.input.NullReader;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.mixin.accessor.ResourceAccessor;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.resources.GeneratedDataException;
import personthecat.pangaea.resources.ImposterResource;
import personthecat.pangaea.serialization.codec.FunctionCodecs;
import personthecat.pangaea.extras.RegistryOpsExtras;
import personthecat.pangaea.resources.ResourceInfo;

import java.io.BufferedReader;
import java.io.Reader;
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
        at = @At(value = "INVOKE_ASSIGN", target = "Ljava/util/Map$Entry;getValue()Ljava/lang/Object;"))
    private static <E> void consumeIncomingResource(
            ResourceManager resourceManager, RegistryOps.RegistryInfoLookup registryInfoLookup,
            WritableRegistry<E> writableRegistry, Decoder<E> decoder, Map<ResourceKey<?>, Exception> map,
            CallbackInfo ci, @Local ResourceKey<?> key, @Local RegistryOps<JsonElement> ops) {
        RegistryOpsExtras.setIncomingResource(ops, new ResourceInfo(key));
    }

    @Inject(method = "loadContentsFromManager", at = @At("TAIL"))
    private static <E> void disposeIncomingResource(
            ResourceManager resourceManager, RegistryOps.RegistryInfoLookup registryInfoLookup,
            WritableRegistry<E> writableRegistry, Decoder<E> decoder, Map<ResourceKey<?>, Exception> map,
            CallbackInfo ci, @Local RegistryOps<JsonElement> ops) {
        RegistryOpsExtras.setIncomingResource(ops, null);
    }

    // If applicable, this injection allows dynamic resources to be loaded directly.
    // If this injection is skipped, JSON conversion will happen on the fly.
    @Inject(method = "loadElementFromResource", at = @At("HEAD"), cancellable = true)
    private static <E> void loadImposter(
            WritableRegistry<E> registry,
            Decoder<E> decoder,
            RegistryOps<JsonElement> ops,
            ResourceKey<E> key,
            Resource resource,
            RegistrationInfo registration,
            CallbackInfo ci,
            @Share("converted") LocalRef<JsonElement> converted) {
        final var stream = ((ResourceAccessor) resource).getStreamSupplier();
        if (stream instanceof ImposterResource) {
            @SuppressWarnings("all") // This can only be of type E
            final ImposterResource<E> r = ((ImposterResource<E>) stream);
            final Either<DataResult<E>, JsonElement> result = r.apply(ops, decoder);
            result
                .ifRight(converted::set) // user chose to convert the data, so it would load normally
                .ifLeft(data -> {        // data was literal or could not be converted
                    try {
                        registry.register(key, data.getOrThrow(), registration);
                    } catch (final Exception e) {
                        final var msg = data.error().map(DataResult.Error::message).orElse(e.getMessage());
                        LibErrorContext.error(Pangaea.MOD, new GeneratedDataException(key, r.unwrap(), msg, e));
                    }
                    ci.cancel();
                });
        }
    }

    @WrapOperation(method = "loadElementFromResource", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/Resource;openAsReader()Ljava/io/BufferedReader;", remap = false))
    private static <E> BufferedReader skipReader(
            Resource target,
            Operation<BufferedReader> operation,
            @Local(argsOnly = true) Decoder<E> decoder,
            @Share("converted") LocalRef<JsonElement> converted) {
        return converted.get() != null ? new BufferedReader(NullReader.INSTANCE, 1) : operation.call(target);
    }

    // If the user requested it, allow the data to be parsed as regular JSON.
    // This supports any callers who may assume that the data will be JSON-only.
    @WrapOperation(method = "loadElementFromResource", at = @At(value = "INVOKE", target = "Lcom/google/gson/JsonParser;parseReader(Ljava/io/Reader;)Lcom/google/gson/JsonElement;", remap = false))
    private static JsonElement swapJson(
            Reader target,
            Operation<JsonElement> operation,
            @Share("converted") LocalRef<JsonElement> converted) {
        return converted.get() != null ? converted.get() : operation.call(target);
    }
}
