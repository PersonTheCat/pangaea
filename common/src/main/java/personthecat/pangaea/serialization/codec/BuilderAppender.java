package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class BuilderAppender implements CodecAppender {
    public static final int PRIORITY = StructureAppender.PRIORITY + 50;
    public static final Info<BuilderAppender> INFO = BuilderAppender::new;
    private final List<BuilderCodec.BuilderField<?, ?, ?>> fields = new ArrayList<>();
    private final Condition condition = new Condition();

    public <A> void addFields(Collection<? extends BuilderCodec.BuilderField<A, ?, ?>> fields) {
        this.fields.addAll(fields);
    }

    @Override
    public void addEncodeCondition(BooleanSupplier condition) {
        this.condition.addCondition(condition);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> Codec<A> append(Codec<A> codec) {
        final var fields = (List<BuilderCodec.BuilderField<A, ?, ?>>) (Object) this.fields;
        if (!fields.isEmpty()) {
            return new BuilderCodec<>(fields, this.condition.get()).wrap(codec);
        }
        return codec;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Info<BuilderAppender> info() {
        return INFO;
    }
}
