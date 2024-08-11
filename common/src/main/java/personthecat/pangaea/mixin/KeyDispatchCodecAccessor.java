package personthecat.pangaea.mixin;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.codecs.KeyDispatchCodec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Function;

@Mixin(KeyDispatchCodec.class)
public interface KeyDispatchCodecAccessor<K, V> {

    @Accessor(remap = false)
    Function<V, DataResult<K>> getType();

    @Mutable
    @Accessor(remap = false)
    void setType(Function<V, DataResult<K>> type);

    @Accessor(remap = false)
    Function<K, DataResult<MapDecoder< V>>> getDecoder();

    @Mutable
    @Accessor(remap = false)
    void setDecoder(Function<K, DataResult<MapDecoder< V>>> decoder);

    @Accessor
    Function<V, DataResult<MapEncoder<V>>> getEncoder();

    @Mutable
    @Accessor(remap = false)
    void setEncoder(Function<V, DataResult<MapEncoder<V>>> encoder);
}
