package personthecat.pangaea.serialization.codec.appender;

import com.mojang.serialization.Codec;
import personthecat.pangaea.serialization.codec.BuilderCodec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class FlagAppender implements CodecAppender {
    public static final int PRIORITY = PatternAppender.PRIORITY - 5_000;
    public static final Info<FlagAppender> INFO = FlagAppender::new;
    private final List<BuilderCodec.BuilderField<?, ?>> fields = new ArrayList<>();
    private final Condition condition = new Condition();

    public <A> void addFlags(Collection<? extends BuilderCodec.BuilderField<A, ?>> fields) {
        this.fields.addAll(fields);
    }

    @Override
    public void addEncodeCondition(BooleanSupplier condition) {
        this.condition.addCondition(condition);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> Codec<A> append(Codec<A> codec) {
        final var fields = (List<BuilderCodec.BuilderField<A, ?>>) (Object) this.fields;
        if (!fields.isEmpty()) {
            return new BuilderCodec<>(fields, this.condition.get()).asUnionOf(codec);
        }
        return codec;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Info<FlagAppender> info() {
        return INFO;
    }
}
