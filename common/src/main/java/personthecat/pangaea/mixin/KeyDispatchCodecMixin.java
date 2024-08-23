package personthecat.pangaea.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.KeyDispatchCodec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.catlib.command.annotations.Nullable;
import personthecat.pangaea.serialization.codec.KeyDispatchCodecExtras;

import java.util.Objects;

@Mixin(KeyDispatchCodec.class)
public class KeyDispatchCodecMixin<K, V> implements KeyDispatchCodecExtras {
    @Unique
    private String pg$defaultType;

    @Final
    @Shadow(remap = false)
    private Codec<K> keyCodec;

    @Final
    @Shadow(remap = false)
    private String typeKey;

    // This wrap is inefficient, but tolerates additional injections from other mods
    @WrapOperation(
        method = "decode",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/MapLike;get(Ljava/lang/String;)Ljava/lang/Object;"),
        remap = false)
    private <T> @Nullable T getOrDefault(
            MapLike<T> instance, String arg, Operation<T> original, @Local(argsOnly = true) DynamicOps<T> ops) {
        final T t = original.call(instance, arg);
        if (t == null && this.pg$defaultType != null) {
            return ops.createString(this.pg$defaultType);
        }
        return t;
    }

    @WrapOperation(
        method = "encode",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/RecordBuilder;add(Ljava/lang/String;Lcom/mojang/serialization/DataResult;)Lcom/mojang/serialization/RecordBuilder;"),
        remap = false)
    private <T> RecordBuilder<T> wrapAddType(
            RecordBuilder<T> instance, String typeArg, DataResult<T> valueArg, Operation<RecordBuilder<T>> original,
            @Local(argsOnly = true) DynamicOps<T> ops) {
        if (this.pg$defaultType != null && typeArg.equals(this.typeKey) && valueArg.isSuccess()) {
            final T value = valueArg.getOrThrow();
            if (Objects.equals(value, ops.createString(this.pg$defaultType))) {
                return instance;
            }
        }
        return original.call(instance, typeArg, valueArg);
    }

    @Override
    public void pg$setDefaultType(String type) {
        this.pg$defaultType = type;
    }
}
