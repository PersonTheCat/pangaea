package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.codecs.KeyDispatchCodec;
import personthecat.pangaea.mixin.KeyDispatchCodecAccessor;

public interface KeyDispatchCodecExtras {
    void pg$setDefaultType(String type);

    static void setDefaultType(KeyDispatchCodecAccessor<?, ?> accessor, String type) {
        setDefaultType((KeyDispatchCodec<?, ?>) accessor, type);
    }

    static void setDefaultType(KeyDispatchCodec<?, ?> codec, String type) {
        if (!(codec instanceof KeyDispatchCodecExtras extras)) {
            throw new IllegalStateException("Key dispatch codec extras mixin not applied");
        }
        extras.pg$setDefaultType(type);
    }
}
