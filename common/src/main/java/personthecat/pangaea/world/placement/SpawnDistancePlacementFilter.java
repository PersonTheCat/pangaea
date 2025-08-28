package personthecat.pangaea.world.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.Range;
import personthecat.pangaea.util.Utils;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public class SpawnDistancePlacementFilter extends PlacementFilter {
    public static final MapCodec<SpawnDistancePlacementFilter> CODEC = codecOf(
        field(Range.RANGE_UP_CODEC, "distance", f -> f.distance),
        defaulted(Codec.doubleRange(0, 1), "chance", 0.25, f -> f.chance),
        defaulted(Codec.intRange(0, Integer.MAX_VALUE), "fade", 0, f -> f.fade),
        SpawnDistancePlacementFilter::new
    );
    public static final PlacementModifierType<SpawnDistancePlacementFilter> TYPE = () -> CODEC;

    private final Range distance;
    private final double chance;
    private final int fade;

    public SpawnDistancePlacementFilter(Range distance, double chance, int fade) {
        this.distance = distance;
        this.chance = chance;
        this.fade = fade;
    }

    @Override
    protected boolean shouldPlace(PlacementContext ctx, RandomSource rand, BlockPos pos) {
        final int d = (int) Math.sqrt((pos.getX() * pos.getX()) + (pos.getZ() * pos.getZ()));
        return Utils.checkDistanceWithFade(rand, this.distance, d, this.chance, this.fade);
    }

    @Override
    public @NotNull PlacementModifierType<SpawnDistancePlacementFilter> type() {
        return TYPE;
    }
}
