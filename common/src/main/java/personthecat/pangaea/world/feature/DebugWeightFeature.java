package personthecat.pangaea.world.feature;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.jetbrains.annotations.Nullable;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.extras.LevelExtras;
import personthecat.pangaea.world.road.TmpRoadUtils;

public class DebugWeightFeature extends Feature<NoneFeatureConfiguration> {
    public static final DebugWeightFeature INSTANCE = new DebugWeightFeature();
    private static final int PILLAR_HEIGHT = 5;

    private DebugWeightFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        final var sampler = ((ServerChunkCache) ctx.level().getChunkSource()).randomState().sampler();
        final var fCtx = MutableFunctionContext.from(ctx.origin()).at(ctx.chunkGenerator().getSeaLevel());
        final var graph = LevelExtras.getNoiseGraph(ctx.level().getLevel());
        final var weight = TmpRoadUtils.getWeight(graph, sampler, fCtx);
        final var pillar = selectPillar(weight);
        if (pillar == null) {
            return false;
        }
        for (int i = 0; i < PILLAR_HEIGHT; i++) {
            ctx.level().setBlock(ctx.origin().above(i), pillar, 2);
        }
        return true;
    }

    private static @Nullable BlockState selectPillar(double weight) {
        if (weight == TmpRoadUtils.NEVER) return null;
        if (weight < 10) return Blocks.WHITE_WOOL.defaultBlockState();
        if (weight < 20) return Blocks.ORANGE_WOOL.defaultBlockState();
        if (weight < 30) return Blocks.MAGENTA_WOOL.defaultBlockState();
        if (weight < 40) return Blocks.LIGHT_BLUE_WOOL.defaultBlockState();
        if (weight < 50) return Blocks.YELLOW_WOOL.defaultBlockState();
        if (weight < 60) return Blocks.LIME_WOOL.defaultBlockState();
        if (weight < 70) return Blocks.PINK_WOOL.defaultBlockState();
        if (weight < 80) return Blocks.GRAY_WOOL.defaultBlockState();
        if (weight < 90) return Blocks.LIGHT_GRAY_WOOL.defaultBlockState();
        if (weight < 100) return Blocks.CYAN_WOOL.defaultBlockState();
        if (weight < 110) return Blocks.PURPLE_WOOL.defaultBlockState();
        if (weight < 120) return Blocks.BLUE_WOOL.defaultBlockState();
        if (weight < 130) return Blocks.BROWN_WOOL.defaultBlockState();
        if (weight < 140) return Blocks.GREEN_WOOL.defaultBlockState();
        if (weight < 150) return Blocks.RED_WOOL.defaultBlockState();
        return Blocks.BLACK_WOOL.defaultBlockState();
    }
}
