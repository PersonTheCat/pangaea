package personthecat.pangaea.serialization.extras;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockStateMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import personthecat.catlib.data.IdList;
import personthecat.catlib.data.IdMatcher;
import personthecat.catlib.data.Range;
import personthecat.pangaea.data.ColumnBounds;
import personthecat.pangaea.test.McBootstrapExtension;
import personthecat.pangaea.world.filter.ChanceChunkFilter;
import personthecat.pangaea.world.filter.ChunkFilter;
import personthecat.pangaea.world.filter.ClusterChunkFilter;
import personthecat.pangaea.world.filter.UnionChunkFilter;
import personthecat.pangaea.world.placer.BlockPlacer;
import personthecat.pangaea.world.placer.BlockPlacerList;
import personthecat.pangaea.world.placer.ChanceBlockPlacer;
import personthecat.pangaea.world.placer.UnconditionalBlockPlacer;
import personthecat.pangaea.world.provider.ColumnProvider;
import personthecat.pangaea.world.provider.ConstantColumnProvider;
import personthecat.pangaea.world.ruletest.HeterogeneousListRuleTest;
import personthecat.pangaea.world.weight.ApproximateWeight;
import personthecat.pangaea.world.weight.ConstantWeight;
import personthecat.pangaea.world.weight.WeightFunction;
import personthecat.pangaea.world.weight.WeightList;

import java.util.List;

import static personthecat.pangaea.test.TestUtils.assertError;
import static personthecat.pangaea.test.TestUtils.assertSuccess;
import static personthecat.pangaea.test.TestUtils.parse;
import static personthecat.pangaea.world.density.DensityCutoff.DEFAULT_HARSHNESS;

@ExtendWith(McBootstrapExtension.class)
public final class DefaultCodecPatternsTest {

    @Test
    public void placer_parsesState_asUnconditional() {
        final var result = parse(BlockPlacer.CODEC, "'minecraft:coal_ore'");
        assertSuccess(new UnconditionalBlockPlacer(Blocks.COAL_ORE.defaultBlockState()), result);
    }

    @Test
    public void placer_withStatePattern_doesNotParseInvalidState() {
        final var result = parse(BlockPlacer.CODEC, "'minecraft:invalid'");
        assertError(result, "Unknown block type");
    }

    @Test
    public void placer_parsesList_asPlacerList() {
        final var result = parse(BlockPlacer.CODEC, "[{chance:0.5,place:'redstone_ore'}, 'coal_ore']");
        assertSuccess(new BlockPlacerList(List.of(
            new ChanceBlockPlacer(0.5, new UnconditionalBlockPlacer(Blocks.REDSTONE_ORE.defaultBlockState())),
            new UnconditionalBlockPlacer(Blocks.COAL_ORE.defaultBlockState())
        )), result);
    }

    @Test
    public void placer_withListPattern_doesNotParseInvalidList() {
        final var result = parse(BlockPlacer.CODEC, "['minecraft:invalid']");
        assertError(result, "Unknown block type");
    }

    @Test
    public void placer_withNoPatterns_canParseNormally() {
        final var result = parse(BlockPlacer.CODEC, "{ type: 'pangaea:unconditional', place: 'coal_ore' }");
        assertSuccess(new UnconditionalBlockPlacer(Blocks.COAL_ORE.defaultBlockState()), result);
    }

    @Test
    public void float_parsesNumberList_asUniformRange() {
        final var result = parse(FloatProvider.CODEC, "[ 1, 2 ]");
        assertSuccess(UniformFloat.of(1, 2), result);
    }

    @Test
    public void float_withNoPatterns_canParseNormally() {
        final var result = parse(FloatProvider.CODEC, "{ type: 'uniform', min_inclusive: 3, max_exclusive: 4 }");
        assertSuccess(UniformFloat.of(3, 4), result);
    }

    @Test
    public void int_parsesNumberList_asUniformRange() {
        final var result = parse(IntProvider.CODEC, "[ 1, 2 ]");
        assertSuccess(UniformInt.of(1, 2), result);
    }

    @Test
    public void int_withNoPatterns_canParseNormally() {
        final var result = parse(IntProvider.CODEC, "{ type: 'uniform', min_inclusive: 3, max_inclusive: 4 }");
        assertSuccess(UniformInt.of(3, 4), result);
    }

    @Test
    public void ruleTest_parsesHashString_asTagTest() {
        final var result = parse(RuleTest.CODEC, "'#coal_ores'");
        assertSuccess(new TagMatchTest(BlockTags.COAL_ORES), result);
    }

    @Test
    public void ruleTest_withHashStringPattern_doesNotParseInvalidTag() {
        final var result = parse(RuleTest.CODEC, "'#invalid$id'");
        assertError(result, "Not a valid resource location");
    }

    @Test
    public void ruleTest_parsesId_asBlockTest() {
        final var result = parse(RuleTest.CODEC, "'coal_ore'");
        assertSuccess(new BlockMatchTest(Blocks.COAL_ORE), result);
    }

    @Test
    public void ruleTest_parsesStateString_asStateTest() {
        final var result = parse(RuleTest.CODEC, "'redstone_ore[lit=true]'");
        assertSuccess(new BlockStateMatchTest(
            Blocks.REDSTONE_ORE.defaultBlockState().setValue(RedStoneOreBlock.LIT, true)
        ), result);
    }

    @Test
    public void ruleTest_withStateStringPattern_doesNotParseInvalidState() {
        final var result = parse(RuleTest.CODEC, "'redstone_ore[lit]'");
        assertError(result, "Expected value for property");
    }

    @Test
    public void ruleTest_parsesStateObject_asStateTest() {
        final var result = parse(RuleTest.CODEC, "{ Name: 'redstone_ore', Properties: { lit: 'true' }}");
        assertSuccess(new BlockStateMatchTest(
            Blocks.REDSTONE_ORE.defaultBlockState().setValue(RedStoneOreBlock.LIT, true)
        ), result);
    }

    @Test
    public void ruleTest_withStateObjectPattern_doesNotParseInvalidSate() {
        final var result = parse(RuleTest.CODEC, "{ Name: 'no_such_block' }");
        assertError(result, "Unknown registry key");
    }

    @Test
    public void ruleTest_parsesList_asHeterogeneousTest() {
        final var result = parse(RuleTest.CODEC, "[ 'diamond_ore', '#coal_ores' ]");
        assertSuccess(new HeterogeneousListRuleTest(
            IdList.builder(Registries.BLOCK)
                .addEntry(IdMatcher.id(false, ResourceKey.create(Registries.BLOCK, new ResourceLocation("diamond_ore"))))
                .addEntry(IdMatcher.tag(false, BlockTags.COAL_ORES))
                .format(IdList.Format.LIST)
                .build()
        ), result);
    }

    @Test
    public void ruleTest_withListPattern_doesNotParseInvalidList() {
        final var result = parse(RuleTest.CODEC, "[ '?invalid_prefix' ]");
        assertError(result, "character in path of location");
    }

    @Test
    public void ruleTest_withNoPatterns_canParseNormally() {
        final var result = parse(RuleTest.CODEC, "{ predicate_type: 'block_match', block: 'coal_ore' }");
        assertSuccess(new BlockMatchTest(Blocks.COAL_ORE), result);
    }

    @Test
    public void filter_parsesNumber_asChanceFilter() {
        final var result = parse(ChunkFilter.CODEC, "0.5");
        assertSuccess(new ChanceChunkFilter(0.5), result);
    }

    @Test
    public void filter_parsesList_asUnionFilter() {
        final var result = parse(ChunkFilter.CODEC, "[ 0.5, { type: 'pangaea:cluster' }]");
        assertSuccess(new UnionChunkFilter(List.of(
            new ChanceChunkFilter(0.5),
            ClusterChunkFilter.fromProbability(0.75, 0)
        )), result);
    }

    @Test
    public void filter_withListPattern_doesNotParseInvalidList() {
        final var result = parse(ChunkFilter.CODEC, "[{ type: 'unknown' }]");
        assertError(result, "Unknown registry key");
    }

    @Test
    public void filter_withNoPatterns_canParseNormally() {
        final var result = parse(ChunkFilter.CODEC, "{ type: 'pangaea:chance', chance: 0.5 }");
        assertSuccess(new ChanceChunkFilter(0.5), result);
    }

    @Test
    public void weight_parsesNumber_asConstantWeight() {
        final var result = parse(WeightFunction.CODEC, "123");
        assertSuccess(new ConstantWeight(123), result);
    }

    @Test
    public void weight_parsesList_asWeightList() {
        final var result = parse(WeightFunction.CODEC, "[1,2]");
        assertSuccess(new WeightList(List.of(
            new ConstantWeight(1),
            new ConstantWeight(2)
        )), result);
    }

    @Test
    public void weight_withListPattern_doesNotParseInvalidList() {
        final var result = parse(WeightFunction.CODEC, "[{ type: 'unknown' }]");
        assertError(result, "Unknown registry key");
    }

    @Test
    public void weight_parsesId_asTypeWithEmptyMap() {
        final var result = parse(WeightFunction.CODEC, "'pangaea:approximate_depth'");
        assertSuccess(ApproximateWeight.DEPTH, result);
    }

    @Test
    public void weight_withIdPattern_doesNotParseInvalidId() {
        final var result = parse(WeightFunction.CODEC, "'unknown'");
        assertError(result, "No such weight type");
    }

    @Test
    public void anchor_parsesNumber_asAbsolute() {
        final var result = parse(VerticalAnchor.CODEC, "123");
        assertSuccess(VerticalAnchor.absolute(123), result);
    }

    @Test
    public void anchor_withNumberPattern_doesNotParseInvalidNumber() {
        final var result = parse(VerticalAnchor.CODEC, "2048");
        assertError(result, "outside of range");
    }

    @Test
    public void anchor_parsesString_asZeroOffsetType() {
        final var result = parse(VerticalAnchor.CODEC, "'bottom'");
        assertSuccess(VerticalAnchor.bottom(), result);
    }

    @Test
    public void anchor_withStringPattern_doesNotParseInvalidString() {
        final var result = parse(VerticalAnchor.CODEC, "'unknown'");
        assertError(result, "Unknown key: unknown");
    }

    @Test
    public void anchor_withNoPatterns_canParseNormally() {
        final var result = parse(VerticalAnchor.CODEC, "above_bottom: 123");
        assertSuccess(VerticalAnchor.aboveBottom(123), result);
    }

    @Test // pattern was too complicated to split up yet, gets its own codec (HeightPattern.java)
    public void height_parsesHeightPattern_successfully() {
        final var result = parse(HeightProvider.CODEC, "[1, 2]");
        assertSuccess(TrapezoidHeight.of(VerticalAnchor.absolute(1), VerticalAnchor.absolute(2)), result);
    }

    @Test
    public void column_parsesHeightPattern_successfully() {
        final var result = parse(ColumnProvider.CODEC, "[[1, 2], [3, 4]]");
        assertSuccess(new ConstantColumnProvider(ColumnBounds.create(Range.of(1, 2), Range.of(3, 4), DEFAULT_HARSHNESS)), result);
    }
}
