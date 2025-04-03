package personthecat.pangaea.world.placer;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import personthecat.pangaea.world.level.GenerationContext;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public record SimpleBlockPlacer(RuleTest target, BlockState state) implements BlockPlacer {
    private static final RuleTest DEFAULT_TARGET = new TagMatchTest(BlockTags.OVERWORLD_CARVER_REPLACEABLES);
    public static final MapCodec<SimpleBlockPlacer> CODEC = codecOf(
        defaulted(RuleTest.CODEC, "target", DEFAULT_TARGET, SimpleBlockPlacer::target),
        field(BlockState.CODEC, "state", SimpleBlockPlacer::state),
        SimpleBlockPlacer::new
    );

    @Override
    public boolean place(WorldGenLevel level, WorldgenRandom rand, BlockPos pos) {
        final var replaced = level.getBlockState(pos);
        if (this.target.test(replaced, rand)) {
            level.setBlock(pos, this.state, 3);
            return true;
        }
        return false;
    }

    @Override
    public boolean placeUnchecked(GenerationContext ctx, int x, int y, int z) {
        final var replaced = ctx.getUnchecked(x, y, z);
        if (this.target.test(replaced, ctx.rand)) {
            ctx.setUnchecked(x, y, z, this.state);
            return true;
        }
        return false;
    }

    @Override
    public MapCodec<SimpleBlockPlacer> codec() {
        return CODEC;
    }
}