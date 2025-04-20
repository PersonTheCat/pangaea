package personthecat.pangaea.world.level;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import personthecat.catlib.data.ForkJoinThreadLocal;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.extras.WorldGenRegionExtras;

public final class ScopeExtension {
    public static final ForkJoinThreadLocal<MutableFunctionContext> GENERATING_POS = ForkJoinThreadLocal.eager(MutableFunctionContext::new);
    public static final ForkJoinThreadLocal<WorldGenRegion> GENERATING_REGION = ForkJoinThreadLocal.create();
    public static final ForkJoinThreadLocal<RandomSource> DENSITY_RAND = ForkJoinThreadLocal.create();

    public static RandomSource lookupDensityRand() {
        final var density = DENSITY_RAND.get();
        if (density != null) {
            return density;
        }
        final var gen = GENERATING_REGION.get();
        if (gen != null) {
            return WorldGenRegionExtras.getGenerationContext(gen).rand;
        }
        throw new IllegalStateException("Neither generating noise nor features");
    }

    public static GenerationContext lookupPgContext(WorldGenerationContext wgc) {
        if (wgc instanceof PlacementContext pc) {
            return WorldGenRegionExtras.getGenerationContext(pc.getLevel());
        } else if (wgc instanceof GenerationContext gc) {
            return gc;
        }
        return getGenerationContext();
    }

    public static GenerationContext getGenerationContext() {
        return WorldGenRegionExtras.getGenerationContext(GENERATING_REGION.get());
    }

    private ScopeExtension() {}
}
