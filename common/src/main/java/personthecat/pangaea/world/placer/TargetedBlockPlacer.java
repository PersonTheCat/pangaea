package personthecat.pangaea.world.placer;

import com.mojang.serialization.MapCodec;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import personthecat.pangaea.world.level.PangaeaContext;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public record TargetedBlockPlacer(RuleTest target, BlockPlacer place) implements BlockPlacer {
    private static final RuleTest DEFAULT_TARGET = new TagMatchTest(BlockTags.OVERWORLD_CARVER_REPLACEABLES);
    public static final MapCodec<TargetedBlockPlacer> CODEC = codecOf(
        defaulted(RuleTest.CODEC, "target", DEFAULT_TARGET, TargetedBlockPlacer::target),
        field(BlockPlacer.CODEC, "place", TargetedBlockPlacer::place),
        TargetedBlockPlacer::new
    );

    @Override
    public boolean place(PangaeaContext ctx, int x, int y, int z, int updates) {
        final var replaced = ctx.getBlock(x, y, z);
        if (this.target.test(replaced, ctx.rand)) {
            return this.place.place(ctx, x, y, z, updates);
        }
        return false;
    }

    @Override
    public MapCodec<TargetedBlockPlacer> codec() {
        return CODEC;
    }
}