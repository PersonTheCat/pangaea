package personthecat.pangaea.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import personthecat.catlib.data.ResettableLazy;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.catlib.serialization.codec.capture.CaptureCategory;
import personthecat.catlib.serialization.codec.capture.Captor;
import personthecat.catlib.serialization.codec.capture.Key;
import personthecat.pangaea.serialization.codec.appender.BuilderAppender;
import personthecat.pangaea.serialization.codec.appender.CaptureAppender;
import personthecat.pangaea.serialization.codec.appender.CodecAppender;
import personthecat.pangaea.serialization.codec.appender.CodecAppenders;
import personthecat.pangaea.serialization.codec.appender.FlagAppender;
import personthecat.pangaea.serialization.codec.appender.PatternAppender;
import personthecat.pangaea.serialization.codec.appender.PresetAppender;
import personthecat.pangaea.serialization.codec.appender.StructureAppender;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PangaeaCodec<A> implements Codec<A> {
    private static final Map<Key<?>, PangaeaCodec<?>> CODECS = new ConcurrentHashMap<>();
    private final ResettableLazy<Codec<A>> base;
    private final CodecAppenders appenders;

    private PangaeaCodec(Codec<A> base) {
        this.base = ResettableLazy.of(() -> this.generateCodec(base));
        this.appenders = new CodecAppenders();
    }

    public static <A> PangaeaCodec<A> get(Class<A> type) {
        return get(Key.of(type.getSimpleName(), type));
    }

    @SuppressWarnings("unchecked")
    public static <A> PangaeaCodec<A> get(Key<A> key) {
        return (PangaeaCodec<A>) Objects.requireNonNull(CODECS.get(key), "No codec for key: " + key);
    }

    @SafeVarargs
    public static <A> MapCodec<A> buildMap(Function<CaptureCategory<A>, MapCodec<A>> builder, A... implicitType) {
        return build(builder, implicitType).mapCodec();
    }

    @SafeVarargs
    public static <A> MapCodec<A> buildMap(String name, Function<CaptureCategory<A>, MapCodec<A>> builder, A... implicitType) {
        return build(Key.of(name, implicitType), builder).mapCodec();
    }

    @SafeVarargs
    public static <A> PangaeaCodec<A> build(Function<CaptureCategory<A>, MapCodec<A>> builder, A... implicitType) {
        return build(inferKey(implicitType), builder);
    }

    public static <A> PangaeaCodec<A> build(Key<A> key, Function<CaptureCategory<A>, MapCodec<A>> builder) {
        return create(key, builder.apply(CaptureCategory.get(key.name(), key.type())).codec());
    }

    @SafeVarargs
    public static <A> PangaeaCodec<A> forRegistry(
            RegistryHandle<MapCodec<? extends A>> registry, Function<? super A, ? extends MapCodec<? extends A>> getter, A... implicitType) {
        return forRegistry(inferKey(implicitType), registry, getter);
    }

    public static <A> PangaeaCodec<A> forRegistry(
            Key<A> key, RegistryHandle<MapCodec<? extends A>> registry, Function<? super A, ? extends MapCodec<? extends A>> getter) {
        return create(key, registry.codec().dispatch(getter, Function.identity()));
    }

    @SafeVarargs
    public static <A> PangaeaCodec<A> create(Codec<A> base, A... implicitType) {
        return create(inferKey(implicitType), base);
    }

    public static <A> PangaeaCodec<A> create(Key<A> key, Codec<A> base) {
        final var codec = new PangaeaCodec<>(base);
        CODECS.put(key, codec);
        return codec;
    }

    @SafeVarargs
    private static <A> Key<A> inferKey(A... implicitType) {
        final var type = Key.inferType(implicitType);
        return Key.of(type.getSimpleName(), type);
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        return this.base.get().decode(ops, input);
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        return this.base.get().encode(input, ops, prefix);
    }

    public PangaeaCodec<A> addPreset(String name, List<Captor<?>> preset) {
        return this.configure(PresetAppender.class, c -> c.addPreset(name, preset));
    }

    public PangaeaCodec<A> addPresetNameFunction(Function<A, DataResult<String>> getter) {
        return this.configure(PresetAppender.class, c -> c.setNameFunction(getter));
    }

    public PangaeaCodec<A> addCaptures(Captor<?>... captors) {
        return this.addCaptures(List.of(captors));
    }

    public PangaeaCodec<A> addCaptures(Collection<? extends Captor<?>> captors) {
        return this.configure(CaptureAppender.class, c -> c.addCaptors(captors));
    }

    @SafeVarargs
    public final PangaeaCodec<A> addStructures(StructuralCodec.Structure<? extends A>... structures) {
        return this.addStructures(List.of(structures));
    }

    public PangaeaCodec<A> addStructures(Collection<? extends StructuralCodec.Structure<? extends A>> structures) {
        return this.configure(StructureAppender.class, c -> c.addStructures(structures));
    }

    public PangaeaCodec<A> addStructureCondition(BooleanSupplier condition) {
        return this.configure(StructureAppender.class, c -> c.addEncodeCondition(condition));
    }

    @SafeVarargs
    public final PangaeaCodec<A> addBuilderFields(BuilderCodec.BuilderField<A, ?>... fields) {
        return this.addBuilderFields(List.of(fields));
    }

    public PangaeaCodec<A> addBuilderFields(Collection<? extends BuilderCodec.BuilderField<A, ?>> fields) {
        return this.configure(BuilderAppender.class, c -> c.addFields(fields));
    }

    public PangaeaCodec<A> addBuilderCondition(BooleanSupplier condition) {
        return this.configure(BuilderAppender.class, c -> c.addEncodeCondition(condition));
    }

    @SafeVarargs
    public final PangaeaCodec<A> addFlags(BuilderCodec.BuilderField<A, ?>... flags) {
        return this.addFlags(List.of(flags));
    }

    public PangaeaCodec<A> addFlags(Collection<? extends BuilderCodec.BuilderField<A, ?>> flags) {
        return this.configure(FlagAppender.class, c -> c.addFlags(flags));
    }

    public PangaeaCodec<A> addFlagCondition(BooleanSupplier condition) {
        return this.configure(FlagAppender.class, c -> c.addEncodeCondition(condition));
    }

    @SafeVarargs
    public final PangaeaCodec<A> addPatterns(PatternCodec.Pattern<? extends A>... patterns) {
        return this.addPatterns(List.of(patterns));
    }

    public PangaeaCodec<A> addPatterns(Collection<? extends PatternCodec.Pattern<? extends A>> patterns) {
        return this.configure(PatternAppender.class, c -> c.addPatterns(patterns));
    }

    public PangaeaCodec<A> addPatternCondition(BooleanSupplier condition) {
        return this.configure(PatternAppender.class, c -> c.addEncodeCondition(condition));
    }

    public PangaeaCodec<A> addGlobalCondition(BooleanSupplier condition) {
        this.appenders.addGlobalCondition(condition);
        return this.reset();
    }

    public <C extends CodecAppender> PangaeaCodec<A> configure(Class<C> type, Consumer<C> action) {
        action.accept(this.appenders.get(type));
        return this.reset();
    }

    private PangaeaCodec<A> reset() {
        this.base.reset();
        return this;
    }

    public MapCodec<A> mapCodec() {
        return MapCodec.assumeMapUnsafe(this);
    }

    private Codec<A> generateCodec(Codec<A> base) {
        return this.appenders.generate(base);
    }
}
