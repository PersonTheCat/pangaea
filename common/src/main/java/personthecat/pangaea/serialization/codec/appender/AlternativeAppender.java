package personthecat.pangaea.serialization.codec.appender;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.List;

public final class AlternativeAppender implements CodecAppender {
    public static final int PRIORITY = PatternAppender.PRIORITY - 1_000;
    public static final Info<AlternativeAppender> INFO = AlternativeAppender::new;
    private final List<Alternative<?>> alternatives = new ArrayList<>();

    public <A, B extends A> void addAlternative(Codec<B> alt, Class<B> type) {
        this.alternatives.add(new Alternative<>(alt, type));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> Codec<A> append(String typeKey, Codec<A> codec) {
        for (final var alt : (List<Alternative<A>>) (Object) this.alternatives) {
            codec = this.appendSingle(codec, alt);
        }
        return codec;
    }

    private <A> Codec<A> appendSingle(Codec<A> codec, Alternative<A> alt) {
        return Codec.xor(codec, alt.alt)
            .xmap(Either::unwrap, a -> alt.type.isInstance(a) ? Either.right(a) : Either.left(a));
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Info<AlternativeAppender> info() {
        return INFO;
    }

    private record Alternative<A>(Codec<A> alt, Class<A> type) {}
}
