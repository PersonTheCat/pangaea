package personthecat.pangaea.serialization.codec.appender;

import com.mojang.serialization.Codec;
import personthecat.catlib.serialization.codec.capture.Captor;
import personthecat.catlib.serialization.codec.capture.CapturingCodec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class CaptureAppender implements CodecAppender {
    public static final int PRIORITY = PresetAppender.PRIORITY + 50;
    public static final Info<CaptureAppender> INFO = CaptureAppender::new;
    private final List<Captor<?>> captors = new ArrayList<>();

    public void addCaptors(Collection<? extends Captor<?>> captors) {
        this.captors.addAll(captors);
    }

    @Override
    public <A> Codec<A> append(Codec<A> codec) {
        if (!this.captors.isEmpty()) {
            return CapturingCodec.builder().capturing(this.captors).build(codec);
        }
        return codec;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Info<CaptureAppender> info() {
        return INFO;
    }
}
