package personthecat.pangaea.serialization.codec.appender;

import com.mojang.serialization.Codec;
import personthecat.pangaea.serialization.codec.PatternCodec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;

public class PatternAppender implements CodecAppender {
    public static final int PRIORITY = FlagAppender.PRIORITY - 1_000;
    public static final Info<PatternAppender> INFO = PatternAppender::new;
    private final List<PatternCodec.Pattern<?>> patterns = new ArrayList<>();
    private final Condition condition = new Condition();

    public <A> void addPatterns(Collection<? extends PatternCodec.Pattern<? extends A>> patterns) {
        this.patterns.addAll(patterns);
    }

    @Override
    public void addEncodeCondition(BooleanSupplier condition) {
        this.condition.addCondition(condition);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> Codec<A> append(String typeKey, Codec<A> codec) {
        final var patterns = (List<PatternCodec.Pattern<A>>) (Object) this.patterns;
        if (!patterns.isEmpty()) {
            return new PatternCodec<>(patterns, this.condition.get()).wrap(codec);
        }
        return codec;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Info<PatternAppender> info() {
        return INFO;
    }
}
