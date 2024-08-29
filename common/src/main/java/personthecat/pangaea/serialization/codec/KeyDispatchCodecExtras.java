package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.KeyDispatchCodec;
import personthecat.pangaea.mixin.KeyDispatchCodecAccessor;

import java.util.function.Function;

public interface KeyDispatchCodecExtras {
    void pg$setDefaultType(Function<DynamicOps<?>, String> type);

    static void setDefaultType(KeyDispatchCodecAccessor<?, ?> accessor, Function<DynamicOps<?>, String> type) {
        setDefaultType((KeyDispatchCodec<?, ?>) accessor, type);
    }

    static void setDefaultType(KeyDispatchCodec<?, ?> codec, Function<DynamicOps<?>, String> type) {
        if (!(codec instanceof KeyDispatchCodecExtras extras)) {
            throw new IllegalStateException("Key dispatch codec extras mixin not applied");
        }
        extras.pg$setDefaultType(type);
    }
}
