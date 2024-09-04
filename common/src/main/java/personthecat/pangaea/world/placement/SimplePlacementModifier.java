package personthecat.pangaea.world.placement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.asParent;
import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleEither;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public class SimplePlacementModifier extends PlacementModifier {
    public static final MapCodec<SimplePlacementModifier> CODEC = codecOf(
        field(IntProvider.CODEC, "count", c -> c.count),
        field(HeightProvider.CODEC, "height", c -> c.height),
        defaulted(Codec.INT, "bias", 0, c -> c.bias),
        defaulted(Codec.DOUBLE, "chance", 1.0, c -> c.chance),
        SimplePlacementModifier::new
    );
    private static final Codec<PlacementModifier> PLACEMENT_CODEC = asParent(CODEC.codec());
    public static final Codec<PlacementModifier> DEFAULT_FLEXIBLE_CODEC =
        simpleEither(PLACEMENT_CODEC, PlacementModifier.CODEC)
            .withEncoder(p -> p instanceof SimplePlacementModifier ? PLACEMENT_CODEC : PlacementModifier.CODEC);
    public static final Codec<List<PlacementModifier>> DEFAULTED_LIST_CODEC =
        Codec.either(PLACEMENT_CODEC, DEFAULT_FLEXIBLE_CODEC.listOf()).xmap(
            either -> either.map(List::of, Function.identity()),
            list -> list.size() == 1 && list.getFirst() instanceof SimplePlacementModifier f
                ? Either.left(f) : Either.right(list));
    public static final PlacementModifierType<SimplePlacementModifier> TYPE = () -> CODEC;

    public final IntProvider count;
    public final HeightProvider height;
    public final int bias;
    public final double chance;

    public SimplePlacementModifier(final IntProvider count, final HeightProvider height, final int bias, final double chance) {
        this.count = count;
        this.height = height;
        this.bias = bias;
        this.chance = chance;
    }

    @Override
    public @NotNull Stream<BlockPos> getPositions(final PlacementContext ctx, final RandomSource rand, final BlockPos origin) {
        return IntStream.range(0, this.count.sample(rand))
            .filter(i -> this.chance == 1 || rand.nextFloat() <= this.chance)
            .mapToObj(i -> this.genPos(rand, ctx, origin));
    }

    private BlockPos genPos(final RandomSource rand, final PlacementContext ctx, final BlockPos origin) {
        return new BlockPos(
            rand.nextInt(16) + origin.getX(),
            this.genHeight(rand, ctx),
            rand.nextInt(16) + origin.getZ()
        );
    }

    private int genHeight(final RandomSource rand, final PlacementContext ctx) {
        final int offset = -ctx.getMinGenY();
        int y = this.height.sample(rand, ctx) + offset;
        for (int i = 0; y > 0 && i < this.bias; i++) {
            y = rand.nextInt(y + 1);
        }
        return y - offset;
    }

    @Override
    public @NotNull PlacementModifierType<?> type() {
        return TYPE;
    }
}
