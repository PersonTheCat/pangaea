package personthecat.pangaea.serialization.extras;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import personthecat.pangaea.test.McBootstrapExtension;
import personthecat.pangaea.world.placer.BlockPlacer;
import personthecat.pangaea.world.placer.ChanceBlockPlacer;
import personthecat.pangaea.world.placer.TargetedBlockPlacer;
import personthecat.pangaea.world.placer.UnconditionalBlockPlacer;

import static personthecat.pangaea.test.TestUtils.assertError;
import static personthecat.pangaea.test.TestUtils.assertSuccess;
import static personthecat.pangaea.test.TestUtils.parse;

@ExtendWith(McBootstrapExtension.class)
public final class DefaultBuilderFieldsTest {

    @Test
    public void placer_parsesSinglePlace_asUnconditionalPlacer() {
        final var result = parse(BlockPlacer.CODEC, "place: 'stone'");
        assertSuccess(new UnconditionalBlockPlacer(Blocks.STONE.defaultBlockState()), result);
    }

    @Test
    public void placer_withSinglePlace_doesNotParseInvalidArguments() {
        final var result = parse(BlockPlacer.CODEC, "place: 'unknown'");
        assertError(result, "Unknown block type");
    }

    @Test
    public void placer_withAdditionalFields_wrapsPlacer() {
        final var result = parse(BlockPlacer.CODEC, "chance: 0.5, target: '#replaceable', place: 'stone'");
        assertSuccess(
            new TargetedBlockPlacer(new TagMatchTest(BlockTags.REPLACEABLE),
                new ChanceBlockPlacer(0.5,
                    new UnconditionalBlockPlacer(Blocks.STONE.defaultBlockState()))),
            result,
            true // tag match test has no equals impl
        );
    }

    @Test
    public void placer_withExplicitType_canBeParsedNormally() {
        final var result = parse(BlockPlacer.CODEC, "type: 'pangaea:chance', chance: 0.5, place: 'stone'");
        assertSuccess(
            new ChanceBlockPlacer(0.5,
                new UnconditionalBlockPlacer(Blocks.STONE.defaultBlockState())),
            result
        );
    }

    @Test
    public void placer_withExplicitType_doesNotAppendBuilderFields() {
        final var result = parse(BlockPlacer.CODEC, "type: 'pangaea:chance', chance: 0.5, target: '#replaceable', place: 'stone'");
        assertSuccess(
            new ChanceBlockPlacer(0.5,
                new UnconditionalBlockPlacer(Blocks.STONE.defaultBlockState())),
            result // no target (chance codec only)
        );
    }
}
