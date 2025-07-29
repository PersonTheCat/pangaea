package personthecat.pangaea.serialization.extras;

import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import personthecat.catlib.data.FloatRange;
import personthecat.catlib.data.Range;
import personthecat.fastnoise.data.NoiseType;
import personthecat.pangaea.serialization.codec.StructuralCodec.Structure;
import personthecat.pangaea.test.McBootstrapExtension;
import personthecat.pangaea.world.density.DensityList;
import personthecat.pangaea.world.density.FastNoiseDensity;
import personthecat.pangaea.world.filter.ChanceChunkFilter;
import personthecat.pangaea.world.filter.ChunkFilter;
import personthecat.pangaea.world.filter.ClusterChunkFilter;
import personthecat.pangaea.world.filter.DensityChunkFilter;
import personthecat.pangaea.world.filter.FastNoiseChunkFilter;
import personthecat.pangaea.world.filter.IntervalChunkFilter;
import personthecat.pangaea.world.filter.PredictableChunkFilter;
import personthecat.pangaea.world.filter.SpawnDistanceChunkFilter;
import personthecat.pangaea.world.provider.DensityFloatProvider;
import personthecat.pangaea.world.provider.DensityHeightProvider;
import personthecat.pangaea.world.provider.DensityIntProvider;
import personthecat.pangaea.world.provider.DensityVerticalAnchor;
import personthecat.pangaea.world.weight.ConstantWeight;
import personthecat.pangaea.world.weight.CutoffWeight;
import personthecat.pangaea.world.weight.DensityWeight;
import personthecat.pangaea.world.weight.MultipleWeight;
import personthecat.pangaea.world.weight.SumWeight;
import personthecat.pangaea.world.weight.WeightFunction;

import java.util.List;

import static personthecat.pangaea.test.TestUtils.assertError;
import static personthecat.pangaea.test.TestUtils.assertSuccess;
import static personthecat.pangaea.test.TestUtils.parse;

@ExtendWith(McBootstrapExtension.class)
public final class DefaultCodecStructuresTest {

    @Test
    public void density_parsesMulTimes_asMultiplication() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "mul: 1, times: 2");
        assertSuccess(DensityFunctions.mul(DensityFunctions.constant(1), DensityFunctions.constant(2)), result);
    }

    @Test
    public void density_withMulTimesStructure_doesNotParseInvalidArguments() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "mul: 1, times: 2000000");
        assertError(result, "outside of range");
    }

    @Test
    public void density_parsesAddPlus_asAddition() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "add: 1, plus: 2");
        assertSuccess(DensityFunctions.add(DensityFunctions.constant(1), DensityFunctions.constant(2)), result);
    }

    @Test
    public void density_withAddPlusStructure_doesNotParseInvalidArguments() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "add: 1, plus: 2000000");
        assertError(result, "outside of range");
    }

    @Test
    public void density_parsesMin_asMinList() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "min: [ 1, 2, 3 ]");
        assertSuccess(
            new DensityList.Min(
                List.of(DensityFunctions.constant(1), DensityFunctions.constant(2), DensityFunctions.constant(3)),
                1_000_000.0),
            result);
    }

    @Test
    public void density_parsesMin_withOptimizedDecoder() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "min: [ 1 ]");
        assertSuccess(DensityFunctions.constant(1), result);
    }

    @Test
    public void density_withMinStructure_doesNotParseInvalidArgument() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "min: [ 2000000 ]");
        assertError(result, "outside of range");
    }

    @Test
    public void density_parsesMax_asMaxList() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "max: [ 1, 2, 3 ]");
        assertSuccess(
            new DensityList.Max(
                List.of(DensityFunctions.constant(1), DensityFunctions.constant(2), DensityFunctions.constant(3)),
                -1_000_000.0),
            result);
    }

    @Test
    public void density_parsesMax_withOptimizedDecoder() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "max: [ 1 ]");
        assertSuccess(DensityFunctions.constant(1), result);
    }

    @Test
    public void density_withMaxStructure_doesNotParseInvalidArgument() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "min: [ 2000000 ]");
        assertError(result, "outside of range");
    }

    @Test
    public void density_parsesSum_asSumList() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "sum: [ 1, 2, 3 ]");
        assertSuccess(
            new DensityList.Sum(
                List.of(DensityFunctions.constant(1), DensityFunctions.constant(2), DensityFunctions.constant(3)),
                500_000.0),
            result);
    }

    @Test
    public void density_parsesSum_withOptimizedDecoder() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "sum: [ 1 ]");
        assertSuccess(DensityFunctions.constant(1), result);
    }

    @Test
    public void density_withSumStructure_doesNotParseInvalidArgument() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "sum: [ 2000000 ]");
        assertError(result, "outside of range");
    }

    @Test
    public void density_parsesAbs_asMappedAbs() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "abs: -1");
        assertSuccess(DensityFunctions.constant(-1).abs(), result);
    }

    @Test
    public void density_withAbsStructure_doesNotParseInvalidArguments() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "abs: 2000000");
        assertError(result, "outside of range");
    }

    @Test
    public void density_parsesSquare_asMappedSquare() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "square: -1");
        assertSuccess(DensityFunctions.constant(-1).square(), result);
    }

    @Test
    public void density_withSquareStructure_doesNotParseInvalidArguments() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "square: 2000000");
        assertError(result, "outside of range");
    }

    @Test
    public void density_parsesCube_asMappedCube() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "cube: -1");
        assertSuccess(DensityFunctions.constant(-1).cube(), result);
    }

    @Test
    public void density_withCubeStructure_doesNotParseInvalidArguments() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "cube: 2000000");
        assertError(result, "outside of range");
    }

    @Test
    public void density_parsesHalfNegative_asMappedHalfNegative() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "half_negative: -1");
        assertSuccess(DensityFunctions.constant(-1).halfNegative(), result);
    }

    @Test
    public void density_withHalfNegativeStructure_doesNotParseInvalidArguments() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "half_negative: 2000000");
        assertError(result, "outside of range");
    }

    @Test
    public void density_parsesQuarterNegative_asMappedQuarterNegative() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "quarter_negative: -1");
        assertSuccess(DensityFunctions.constant(-1).quarterNegative(), result);
    }

    @Test
    public void density_withQuarterNegativeStructure_doesNotParseInvalidArguments() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "quarter_negative: 2000000");
        assertError(result, "outside of range");
    }

    @Test
    public void density_parsesSqueeze_asMappedSqueeze() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "squeeze: -1");
        assertSuccess(DensityFunctions.constant(-1).squeeze(), result);
    }

    @Test
    public void density_withSqueezeStructure_doesNotParseInvalidArguments() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "squeeze: 2000000");
        assertError(result, "outside of range");
    }

    @Test
    public void density_withNoiseStructure_andFastNoiseType_parsesFastNoiseDensity() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "noise: 'simplex'");
        assertSuccess(NoiseType.SIMPLEX, result.map(Structure.get((FastNoiseDensity d) -> d.noise().toBuilder().type())));
    }

    @Test
    public void density_withNoiseStructure_andFastNoiseType_doesNotParseInvalidProperties() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "noise: 'simplex', frequency: [ 1, 2, 3, 4 ]");
        assertError(result, "not a list of 3 elements");
    }

    @Test
    public void density_parsesShiftedNoiseStructure_asShiftedNoise() {
        final var result = parse(
            DensityFunctions.DIRECT_CODEC, "noise: { amplitudes: [], firstOctave: 1 }, shift_x: 1, shift_y: 2, shift_z: 3, xz_scale: 4, y_scale: 5");
        assertSuccess(result); // too complicated, not doing that
    }

    @Test
    public void density_parsesNoiseStructure_withoutFastNoiseType_asNoiseDensity() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "noise: { amplitudes: [], firstOctave: 1 }, xz_scale: 1, y_scale: 2");
        assertSuccess(result); // too complicated, not doing that
    }

    @Test
    public void density_withNoStructures_canParseNormally() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "type: 'constant', argument: 1");
        assertSuccess(DensityFunctions.constant(1), result);
    }

    @Test
    public void float_parsesDensity_asDensityFloat() {
        final var result = parse(FloatProvider.CODEC, "density: 1");
        assertSuccess(new DensityFloatProvider(DensityFunctions.constant(1)), result);
    }

    @Test
    public void float_withDensityStructure_doesNotParseInvalidArgument() {
        final var result = parse(FloatProvider.CODEC, "density: 2000000");
        assertError(result, "outside of range");
    }

    @Test
    public void float_withNoStructures_canParseNormally() {
        final var result = parse(FloatProvider.CODEC, "type: 'uniform', min_inclusive: 1, max_exclusive: 2");
        assertSuccess(UniformFloat.of(1, 2), result);
    }

    @Test
    public void int_parsesDensity_asDensityInt() {
        final var result = parse(IntProvider.CODEC, "density: 1");
        assertSuccess(new DensityIntProvider(DensityFunctions.constant(1)), result);
    }

    @Test
    public void int_withDensityStructure_doesNotParseInvalidArgument() {
        final var result = parse(IntProvider.CODEC, "density: 2000000");
        assertError(result, "outside of range");
    }

    @Test
    public void int_withNoStructures_canParseNormally() {
        final var result = parse(IntProvider.CODEC, "type: 'uniform', min_inclusive: 1, max_inclusive: 2");
        assertSuccess(UniformInt.of(1, 2), result);
    }

    @Test
    public void weight_parsesMulTimes_asMultiplication() {
        final var result = parse(WeightFunction.CODEC, "mul: 1, times: 2");
        assertSuccess(MultipleWeight.create(new ConstantWeight(1), new ConstantWeight(2)), result);
    }

    @Test
    public void weight_withMulTimesStructure_doesNotParseInvalidArguments() {
        final var result = parse(WeightFunction.CODEC, "mul: 1, times: { type: 'unknown' }");
        assertError(result, "Unknown registry key");
    }

    @Test
    public void weight_parsesAddPlus_asAddition() {
        final var result = parse(WeightFunction.CODEC, "add: 1, plus: 2");
        assertSuccess(SumWeight.create(new ConstantWeight(1), new ConstantWeight(2)), result);
    }

    @Test
    public void weight_withAddPlusStructure_doesNotParseInvalidArguments() {
        final var result = parse(WeightFunction.CODEC, "add: 1, plus: { type: 'unknown' }");
        assertError(result, "Unknown registry key");
    }

    @Test
    public void weight_parsesDensity_asDensityWeight() {
        final var result = parse(WeightFunction.CODEC, "density: 1");
        assertSuccess(new DensityWeight(DensityFunctions.constant(1)), result);
    }

    @Test
    public void weight_withDensityStructure_doesNotParseInvalidArguments() {
        final var result = parse(WeightFunction.CODEC, "density: 2000000");
        assertError(result, "outside of range");
    }

    @Test
    public void weight_parsesCutoffStructure_asCutoffWeight() {
        final var result = parse(WeightFunction.CODEC, "input: 1, lower: [ 2, 3 ]"); // upper and/or lower
        assertSuccess(new CutoffWeight(new ConstantWeight(1), FloatRange.of(2, 3), FloatRange.of(99999)), result);
    }

    @Test
    public void weight_withCutoffStructure_doesNotParseInvalidArguments() {
        final var result = parse(WeightFunction.CODEC, "input: { type: 'unknown' }, upper: [ 1, 2 ]");
        assertError(result, "Unknown registry key");
    }

    @Test
    public void weight_withNoStructures_canParseNormally() {
        final var result = parse(WeightFunction.CODEC, "type: 'pangaea:constant', weight: 1");
        assertSuccess(new ConstantWeight(1), result);
    }

    @Test
    public void filter_parsesDensity_asDensityFilter() {
        final var result = parse(ChunkFilter.CODEC, "density: 1");
        assertSuccess(new DensityChunkFilter(DensityFunctions.constant(1)), result);
    }

    @Test
    public void filter_withDensityStructure_doesNotParseInvalidArguments() {
        final var result = parse(ChunkFilter.CODEC, "density: 2000000");
        assertError(result, "outside of range");
    }

    @Test
    public void filter_withClusterChance_parsesClusterChunkFilter() {
        final var result = parse(ChunkFilter.CODEC, "cluster_chance: 0.5");
        assertSuccess(ClusterChunkFilter.fromProbability(0.5, 0), result);
    }

    @Test
    public void filter_withClusterChance_doesNotParseInvalidArguments() {
        final var result = parse(ChunkFilter.CODEC, "cluster_chance: 2");
        assertError(result, "outside of range");
    }

    @Test
    public void filter_parsesNoise_asFastNoiseFilter() {
        final var result = parse(ChunkFilter.CODEC, "noise: 'simplex'");
        assertSuccess(NoiseType.SIMPLEX, result.map(Structure.get((FastNoiseChunkFilter f) -> f.noise().toBuilder().type())));
    }

    @Test
    public void filter_withNoiseStructure_doesNotParseInvalidArguments() {
        final var result = parse(ChunkFilter.CODEC, "noise: 'simplex', frequency: [ 1, 2, 3, 4 ]");
        assertError(result, "not a list of 3 elements");
    }

    @Test
    public void filter_parsesInterval_asIntervalFilter() {
        final var result = parse(ChunkFilter.CODEC, "interval: 2");
        assertSuccess(new IntervalChunkFilter(2), result);
    }

    @Test
    public void filter_withIntervalStructure_doesNotParseInvalidArguments() {
        final var result = parse(ChunkFilter.CODEC, "interval: -1");
        assertError(result, "outside of range");
    }

    @Test
    public void filter_parsesDistance_asSpawnDistanceFilter() {
        final var result = parse(ChunkFilter.CODEC, "distance: [ 5, 10 ]");
        assertSuccess(new SpawnDistanceChunkFilter(Range.of(5, 10), 0.25, 0), result);
    }

    @Test
    public void filter_withDistanceStructure_doesNotParseInvalidArguments() {
        final var result = parse(ChunkFilter.CODEC, "distance: 5, chance: 2");
        assertError(result, "outside of range");
    }

    @Test
    public void filter_parsesChance_asChanceFilter() {
        final var result = parse(ChunkFilter.CODEC, "chance: 0.5");
        assertSuccess(new ChanceChunkFilter(0.5), result);
    }

    @Test
    public void filter_withChanceStructure_doesNotParseInvalidArguments() {
        final var result = parse(ChunkFilter.CODEC, "chance: 1.1");
        assertError(result, "outside of range");
    }

    @Test
    public void filter_withNoStructures_canParseNormally() {
        final var result = parse(ChunkFilter.CODEC, "type: 'pangaea:predictable'");
        assertSuccess(PredictableChunkFilter.fromProbability(0.75, 0), result);
    }

    @Test // not technically correct, but functionally the same
    public void height_parsesDensityStructure_asDensityOffsetHeight() {
        final var result = parse(HeightProvider.CODEC, "density: 2");
        assertSuccess(ConstantHeight.of(new DensityVerticalAnchor(DensityFunctions.constant(2))), result, true);
    }

    @Test
    public void height_withDensityStructure_doesNotParseInvalidArguments() {
        final var result = parse(HeightProvider.CODEC, "density: 2000000");
        assertError(result, "outside of range");
    }

    @Test
    public void height_withNoStructures_canParseNormally() {
        final var result = parse(HeightProvider.CODEC, "type: 'constant', value: { absolute: 1 }");
        assertSuccess(ConstantHeight.of(VerticalAnchor.absolute(1)), result);
    }
}
