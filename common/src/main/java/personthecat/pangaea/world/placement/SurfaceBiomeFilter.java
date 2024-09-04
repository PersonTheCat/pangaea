package personthecat.pangaea.world.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import org.jetbrains.annotations.NotNull;

public class SurfaceBiomeFilter extends PlacementFilter {
    public static final SurfaceBiomeFilter INSTANCE = new SurfaceBiomeFilter();
    public static final MapCodec<SurfaceBiomeFilter> CODEC = MapCodec.unit(INSTANCE);
    public static final PlacementModifierType<SurfaceBiomeFilter> TYPE = () -> CODEC;

    private SurfaceBiomeFilter() {}

    @Override
    protected boolean shouldPlace(PlacementContext ctx, RandomSource rand, BlockPos pos) {
        final var feature = ctx.topFeature().orElseThrow(() -> new IllegalStateException("Tried to biome check an unregistered feature, or a feature that should not restrict the biome"));
        final var height = ctx.getLevel().getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ());
        final var biome = ctx.getLevel().getBiome(pos.atY(height)).value();
        return biome.getGenerationSettings().hasFeature(feature);
    }

    @Override
    public @NotNull PlacementModifierType<SurfaceBiomeFilter> type() {
        return TYPE;
    }
}
