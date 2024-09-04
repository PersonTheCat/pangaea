package personthecat.pangaea.world.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class IntervalPlacementModifier extends PlacementModifier {
    public static final MapCodec<IntervalPlacementModifier> CODEC =
        RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("interval").forGetter(m -> m.interval)
        ).apply(i, IntervalPlacementModifier::new));

    public static final PlacementModifierType<IntervalPlacementModifier> TYPE = () -> CODEC;
    private final int interval;

    public IntervalPlacementModifier(final int interval) {
        this.interval = interval;
    }

    @Override
    public @NotNull Stream<BlockPos> getPositions(PlacementContext ctx, RandomSource rand, BlockPos pos) {
        final Stream.Builder<BlockPos> builder = Stream.builder();
        int oX = pos.getX() & ~15;
        int oY = pos.getY();
        int oZ = pos.getZ() & ~15;

        for (int x = 0; x < 16; x++) {
            if (x % this.interval != 0) {
                continue;
            }
            for (int z = 0; z < 16; z++) {
                if (z % this.interval != 0) {
                    continue;
                }
                builder.add(new BlockPos(oX + x, oY, oZ + z));
            }
        }
        return builder.build();
    }

    @Override
    public @NotNull PlacementModifierType<?> type() {
        return TYPE;
    }
}
