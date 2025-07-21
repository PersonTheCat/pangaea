package personthecat.pangaea.serialization.codec.appender;

import com.mojang.serialization.Codec;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

public final class CodecAppenders {
    private static final Map<Class<?>, CodecAppender.Info<?>> REGISTRY = new ConcurrentHashMap<>();
    private final Map<Class<?>, CodecAppender> appenders = new HashMap<>();

    public static void bootstrap() {
        register(PresetAppender.class, PresetAppender.INFO);
        register(CaptureAppender.class, CaptureAppender.INFO);
        register(StructureAppender.class, StructureAppender.INFO);
        register(BuilderAppender.class, BuilderAppender.INFO);
        register(FlagAppender.class, FlagAppender.INFO);
    }

    public static <A extends CodecAppender> void register(Class<A> key, CodecAppender.Info<A> info) {
        REGISTRY.put(key, info);
    }

    @SuppressWarnings("unchecked")
    private static <A extends CodecAppender> A create(Class<A> key) {
        return Objects.requireNonNull(((CodecAppender.Info<A>) REGISTRY.get(key)), "unregistered appender: " + key).create();
    }

    @SuppressWarnings("unchecked")
    public <A extends CodecAppender> A get(Class<A> key) {
        return (A) this.appenders.computeIfAbsent(key, k -> create((Class<A>) k));
    }

    public void addGlobalCondition(BooleanSupplier condition) {
        this.appenders.values().forEach(a -> a.addEncodeCondition(condition));
    }

    public <A> Codec<A> generate(Codec<A> base) {
        for (final var appender : this.sortAppenders()) {
            base = appender.append(base);
        }
        return base;
    }

    private Collection<CodecAppender> sortAppenders() {
        return this.appenders.values().stream()
            .sorted(Comparator.comparingInt(CodecAppender::priority)
                .thenComparing(a -> a.getClass().getSimpleName()))
            .toList();
    }
}
