package personthecat.pangaea.world.surface;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.minecraft.world.level.levelgen.SurfaceRules.SequenceRule;
import net.minecraft.world.level.levelgen.SurfaceRules.SurfaceRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.serialization.codec.CodecUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// Experimental -- unused
public record SurfaceController(
        DimensionType dimension,
        List<RuleSource> edge,
        List<RuleSource> surfaceBlocks,
        List<RuleSource> surfaceDecorations,
        List<RuleSource> waterFiller,
        List<RuleSource> underwaterDecorations,
        List<RuleSource> layers) implements RuleSource {

    public SurfaceController(DimensionType dimension) {
        this(dimension, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public boolean hasAny() {
        return !this.edge.isEmpty()
            || !this.surfaceBlocks.isEmpty()
            || !this.surfaceDecorations.isEmpty()
            || !this.waterFiller.isEmpty()
            || !this.layers.isEmpty();
    }

    @Override
    public SurfaceRule apply(Context ctx) {
        return new Rule(
            buildSequence(this.edge.stream(), ctx),
            buildSequence(this.surfaceBlocks.stream(), ctx),
            buildSequence(this.surfaceDecorations.stream(), ctx),
            buildSequence(this.waterFiller.stream(), ctx),
            buildSequence(this.underwaterDecorations.stream(), ctx),
            buildSequence(this.layers.stream(), ctx),
            this.dimension,
            ctx
        );
    }

    private static SurfaceRule buildSequence(Stream<RuleSource> rules, Context ctx) {
        final var built = rules
            .flatMap(InjectedRuleSource::optimizeSource) // debug testing
            .map(source -> source.apply(ctx))
            .collect(ImmutableList.toImmutableList());
        if (built.isEmpty()) return NullSource.NullRule.INSTANCE;
        if (built.size() == 1) return built.getFirst();
        return new SequenceRule(built);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<SurfaceController> codec() {
        return KeyDispatchDataCodec.of(CodecUtils.neverMapCodec());
    }

    private record Rule(
            SurfaceRule edge,
            SurfaceRule surfaceBlocks,
            SurfaceRule surfaceDecorations,
            SurfaceRule waterFiller,
            SurfaceRule underwaterDecorations,
            SurfaceRule layers,
            DimensionType dimension,
            Context ctx) implements SurfaceRule {

        @Override
        public @Nullable BlockState tryApply(int x, int y, int z) {
            var s = this.edge.tryApply(x, y, z);
            if (s != null) {
                return s;
            }
            if (this.isAbovePreliminarySurface()) {
                if (this.isInOrAboveSurface()) {
                    s = this.surfaceDecorations.tryApply(x, y, z);
                    if (s != null) {
                        return s;
                    }
                    if (this.isInWaterSurface()) {
                        s = this.waterFiller.tryApply(x, y, z);
                        if (s != null) {
                            return s;
                        }
                    }
                }
                if (this.isDeepWaterBelow()) {
                    s = this.underwaterDecorations.tryApply(x, y, z);
                    if (s != null) {
                        return s;
                    }
                }
            }
            return this.layers.tryApply(x, y, z);
        }

        private boolean isAbovePreliminarySurface() {
            return this.ctx.blockY >= this.ctx.getMinSurfaceLevel();
        }

        private boolean isInOrAboveSurface() {
            return this.ctx.stoneDepthAbove <= 1;
        }

        private boolean isInWaterSurface() {
            return this.ctx.waterHeight == Integer.MIN_VALUE || this.ctx.blockY >= this.ctx.waterHeight - 1;
        }

        private boolean isDeepWaterBelow() {
            return this.ctx.waterHeight == Integer.MIN_VALUE || this.ctx.blockY + this.ctx.stoneDepthAbove >= this.ctx.waterHeight - 6 - this.ctx.surfaceDepth;
        }
    }
}