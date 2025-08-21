package personthecat.pangaea.world.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.FloatRange;
import personthecat.pangaea.extras.LevelExtras;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public class RoadDistanceFilter extends PlacementFilter {
    public static final MapCodec<RoadDistanceFilter> CODEC = codecOf(
        defaulted(Codec.FLOAT, "min", -Float.MAX_VALUE, f -> f.range.min()),
        defaulted(Codec.FLOAT, "max", Float.MAX_VALUE, f -> f.range.max()),
        RoadDistanceFilter::fromMinAndMax
    );
    public static final PlacementModifierType<RoadDistanceFilter> TYPE = () -> CODEC;

    private final FloatRange range;
    private final float minSignificant;

    private RoadDistanceFilter(FloatRange range) {
        this.range = range;
        this.minSignificant = computeMinSignificant(range);
    }

    public static RoadDistanceFilter fromMinAndMax(float min, float max) {
        return new RoadDistanceFilter(FloatRange.of(min, max));
    }

    private static float computeMinSignificant(FloatRange r) {
        float minSignificant = 0;
        if (r.min() != -Float.MAX_VALUE) minSignificant = Math.max(minSignificant, r.min() + 8);
        if (r.max() != Float.MAX_VALUE) minSignificant = Math.max(minSignificant, r.max() - 8);
        return minSignificant;
    }

    @Override
    protected boolean shouldPlace(PlacementContext ctx, RandomSource rand, BlockPos pos) {
        final var graph = LevelExtras.getNoiseGraph(ctx.getLevel().getLevel());
        return this.range.contains(Math.min(1_000_000F, graph.getApproximateRoadDistance(pos.getX(), pos.getZ(), this.minSignificant)));
    }

    @Override
    public @NotNull PlacementModifierType<RoadDistanceFilter> type() {
        return TYPE;
    }
}
