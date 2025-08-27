package personthecat.pangaea.world.surface;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.minecraft.world.level.levelgen.SurfaceRules.SequenceRuleSource;
import net.minecraft.world.level.levelgen.SurfaceRules.SurfaceRule;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.serialization.codec.CodecUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Stream;

public record InjectedRuleSource(
        Deque<RuleSource> beforeAll,
        MutableObject<RuleSource> original,
        Deque<RuleSource> afterAll) implements RuleSource {

    public static InjectedRuleSource wrap(RuleSource original) {
        return new InjectedRuleSource(new ArrayDeque<>(), new MutableObject<>(original), new ArrayDeque<>());
    }

    @Override
    public SurfaceRule apply(Context ctx) {
        return this.optimize().apply(ctx);
    }

    public RuleSource optimize() {
        final var built = this.generateRuleSequence()
            .flatMap(InjectedRuleSource::unwrapSequence)
            .flatMap(InjectedRuleSource::optimizeSource)
            .collect(ImmutableList.toImmutableList());
        if (built.isEmpty()) return NullSource.INSTANCE;
        if (built.size() == 1) return built.getFirst();
        return new SequenceRuleSource(built);
    }

    private Stream<RuleSource> generateRuleSequence() {
        return Stream.of(
            this.beforeAll.stream(),
            Stream.ofNullable(this.original.getValue()),
            this.afterAll.stream()
        ).flatMap(stream -> stream);
    }

    private static Stream<RuleSource> unwrapSequence(RuleSource source) {
        return source instanceof SequenceRuleSource(var l) ? l.stream() : Stream.of(source);
    }

    @VisibleForDebug
    public static Stream<RuleSource> optimizeSource(RuleSource source) {
        if (source instanceof SurfaceRules.SequenceRuleSource(var l)) {
            final var l2 = l.stream()
                .flatMap(InjectedRuleSource::optimizeSource)
                .collect(ImmutableList.toImmutableList());
            if (l2.size() > 1) {
                return Stream.of(new SurfaceRules.SequenceRuleSource(l2));
            }
            return l2.stream(); // remove empty sequences and unwrap sequences of 1
        } else if (source instanceof SurfaceRules.TestRuleSource(var ifTrue, var thenRun)) {
            final var optimized = optimizeSource(thenRun)
                .collect(ImmutableList.toImmutableList());
            if (optimized.size() == 1) {
                return Stream.of(new SurfaceRules.TestRuleSource(ifTrue, optimized.getFirst()));
            }
            return Stream.empty(); // remove conditions with null or empty branches
        } else if (source == NullSource.INSTANCE) {
            return Stream.empty();
        }
        return Stream.of(source);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends RuleSource> codec() {
        return KeyDispatchDataCodec.of(CodecUtils.neverMapCodec());
    }
}
