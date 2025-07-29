package personthecat.pangaea.serialization.codec.appender;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public interface CodecAppender {
    <A> Codec<A> append(String typeKey, Codec<A> codec);
    Info<? extends CodecAppender> info();

    default int priority() {
        return 1000;
    }

    default void addEncodeCondition(BooleanSupplier condition) {}

    interface Info<A extends CodecAppender> {
        A create();
    }

    class Condition implements Supplier<BooleanSupplier> {
        private @Nullable BooleanSupplier condition;

        @Override
        public BooleanSupplier get() {
            final var c = this.condition;
            return c != null ? c : () -> true;
        }

        public void addCondition(BooleanSupplier condition) {
            final var c = this.condition;
            this.condition = c == null ? condition : () -> c.getAsBoolean() && condition.getAsBoolean();
        }
    }
}
