package personthecat.pangaea.world.level;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.extras.WorldGenRegionExtras;

@SuppressWarnings("preview")
public final class ScopeExtension {
    public static final ScopedValue<MutableFunctionContext> GENERATING_POS = ScopedValue.newInstance();
    public static final ScopedValue<WorldGenRegion> GENERATING_REGION = ScopedValue.newInstance();
    public static final ScopedValue<RandomSource> DENSITY_RAND = ScopedValue.newInstance();

    public static RandomSource lookupDensityRand() {
        if (DENSITY_RAND.isBound()) {
            return DENSITY_RAND.get();
        }
        return WorldGenRegionExtras
            .getGenerationContext(GENERATING_REGION.get())
            .rand;
    }

    private ScopeExtension() {}
}
