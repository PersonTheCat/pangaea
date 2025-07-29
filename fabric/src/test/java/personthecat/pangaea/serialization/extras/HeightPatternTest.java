package personthecat.pangaea.serialization.extras;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import personthecat.pangaea.test.McBootstrapExtension;
import personthecat.pangaea.world.provider.ColumnProvider;

import static personthecat.pangaea.test.TestUtils.assertEquals;
import static personthecat.pangaea.test.TestUtils.assertSuccess;
import static personthecat.pangaea.test.TestUtils.parse;

@ExtendWith(McBootstrapExtension.class)
public final class HeightPatternTest {

    @Test
    public void height_withParamsOnly_ofNumberOnly_returnsConstantHeight_ofAbsoluteHeight() {
        assertHeight("1", """
            {
              type: 'constant'
              value: { absolute: 1 }
            }
            """);
    }

    @Test
    public void height_withParamsOnly_ofNumberList_returnsTrapezoidHeight_ofAbsolutes() {
        assertHeight("[ 1, 2 ]", """
            {
              type: 'trapezoid'
              min_inclusive: { absolute: 1 }
              max_inclusive: { absolute: 2 }
            }
            """);
    }

    @Test
    public void height_withOffsetType_ofNumberOnly_returnsConstantHeight_ofGivenType() {
        assertHeight("bottom: 1", """
            {
              type: 'constant'
              value: { above_bottom: 1 }
            }
            """);
    }

    @Test
    public void height_withTopType_ofNumberOnly_returnsConstantHeight_ofNegatedValue() {
        assertHeight("top: -1", """
            {
              type: 'constant'
              value: { below_top: 1 }
            }
            """);
    }

    @Test
    public void height_withOffsetType_ofNumberList_returnsTrapezoidHeight_ofGivenType() {
        assertHeight("bottom: [ 1, 2 ]", """
            {
              type: 'trapezoid'
              min_inclusive: { above_bottom: 1 }
              max_inclusive: { above_bottom: 2 }
            }
            """);
    }

    @Test
    public void height_withListOfOffsetMaps_returnsTrapezoidHeight_ofEachGivenType() {
        assertHeight("[{ bottom: 1 }, { top: -1 }]", """
            {
              type: 'trapezoid'
              min_inclusive: { above_bottom: 1 }
              max_inclusive: { below_top: 1 }
            }
            """);
    }

    @Test
    public void height_withRangeField_ofNumberList_andDistribution_returnsHeightOfDistributionType() {
        assertHeight("range: [ 1, 2 ], distribution: 'uniform'", """
            {
              type: 'uniform'
              min_inclusive: { absolute: 1 }
              max_inclusive: { absolute: 2 }
            }
            """);
    }

    @Test
    public void height_withRangeField_ofOffsetMapList_andDistribution_returnsHeightOfDistributionType() {
        assertHeight("range: [{ bottom: 1 }, { top: -1 }], distribution: 'uniform'", """
            {
              type: 'uniform'
              min_inclusive: { above_bottom: 1 }
              max_inclusive: { below_top: 1 }
            }
            """);
    }

    @Test
    public void height_withRangeField_andOffsetMap_ofNumberList_andDistribution_returnsHeightOfDistributionType() {
        assertHeight("range: { bottom: [ 1, 2 ] }, distribution: 'uniform'", """
            {
              type: 'uniform'
              min_inclusive: { above_bottom: 1 }
              max_inclusive: { above_bottom: 2 }
            }
            """);
    }

    @Test
    public void height_withRangePattern_doesNotRequireDistribution() {
        assertHeight("range: { bottom: [ 1, 2 ] }", """
            {
              type: 'trapezoid'
              min_inclusive: { above_bottom: 1 }
              max_inclusive: { above_bottom: 2 }
            }
            """);
    }

    @Test
    public void height_withTypeOnly_returnsTypeOffset() {
        assertHeight("'sea_level'", """
            {
              sea_level: 0
            }
            """);
    }

    @Test
    public void height_withTypeList_returnsRangeOfZeroOffsets() {
        assertHeight("['bottom', 'top']", """
            {
              type: 'trapezoid'
              min_inclusive: { above_bottom: 0 }
              max_inclusive: { below_top: 0 }
            }
            """);
    }

    @Test
    public void height_withMixedPatternList_returnsRangeOfAbsolutes_andZeroOffsets() {
        assertHeight("['bottom', 32]", """
            {
              type: 'trapezoid'
              min_inclusive: { above_bottom: 0 }
              max_inclusive: { absolute: 32 }
            }
            """);
    }

    @Test
    public void height_withComplexList_returnsRangeOfAnchors() {
        assertHeight("['bottom', { top: -1 }]", """
            {
              type: 'trapezoid',
              min_inclusive: { above_bottom: 0 }
              max_inclusive: { below_top: 1 }
            }
            """);
    }

    @Test
    public void column_withParamsOnly_ofNumberOnly_returnsExactHeight_ofAbsoluteHeight() {
        assertColumn("1", """
            {
              type: 'pangaea:exact'
              absolute: 1
            }
            """);
    }

    @Test
    public void column_withParamsOnly_ofNumberList_returnsConstantColumn_ofAutomaticRanges() {
        assertColumn("[ 0, 30 ]", """
            {
              type: 'pangaea:constant'
              lower: [ 0, 3 ] // range because harshness is defaulted
              upper: [ 27, 30 ]
            }
            """);
    }

    @Test
    public void column_withParamsOnly_ofNumberMatrix_returnsConstantColumn_ofExactRanges() {
        assertColumn("[[ 0, 5 ], [ 25, 30 ]]", """
            {
              type: 'pangaea:constant'
              lower: [ 0, 5 ]
              upper: [ 25, 30 ]
            }
            """);
    }

    @Test
    public void column_withOffsetType_ofNumberOnly_returnsExactColumn_ofGivenType() {
        assertColumn("bottom: 1", """
            {
              type: 'pangaea:exact'
              above_bottom: 1
            }
            """);
    }

    @Test
    public void column_withTopType_ofNumberOnly_returnsExactColumn_ofNegatedValue() {
        assertColumn("top: -1", """
            {
              type: 'pangaea:exact'
              below_top: 1
            }
            """);
    }

    @Test
    public void column_withOffsetType_ofNumberList_returnsDynamicColumn() {
        assertColumn("bottom: [ 0, 40 ]", """
            {
              type: 'pangaea:dynamic'
              min: { above_bottom: 0 }
              max: { above_bottom: 40 }
            }
            """);
    }

    @Test
    public void column_withAbsoluteType_ofNumberList_returnsConstantColumn_ofAutomaticRanges() {
        assertColumn("absolute: [ 0, 40 ]", """
            {
              type: 'pangaea:constant'
              lower: [ 0, 4 ] // range when type is absolute and harshness is defaulted
              upper: [ 36, 40 ]
            }
            """);
    }

    @Test
    public void column_withOffsetType_ofNumberMatrix_returnsAnchorRangeColumn_ofGivenType() {
        assertColumn("bottom: [[ 0, 5 ], [ 35, 40 ]]", """
            {
              type: 'pangaea:anchor_range'
              lower: {
                min: { above_bottom: 0 }
                max: { above_bottom: 5 }
              }
              upper: {
                min: { above_bottom: 35 }
                max: { above_bottom: 40 }
              }
            }
            """);
    }

    @Test
    public void column_withListOfOffsetMaps_ofNumberOnly_returnsDynamicColumn_ofEachGivenType() {
        assertColumn("[{ bottom: 1 }, { top: -1 }]", """
            {
              type: 'pangaea:dynamic'
              min: { above_bottom: 1 }
              max: { below_top: 1 }
            }
            """);
    }

    @Test
    public void column_withHeterogeneousMatrix_returnsAnchorRange_ofEachGivenType() {
        assertColumn("[[{ bottom: 10 }, 32], [64, { top: -10 }]]", """
            {
              type: 'pangaea:anchor_range'
              lower: {
                min: { above_bottom: 10 }
                max: { absolute: 32 }
              }
              upper: {
                min: { absolute: 64 }
                max: { below_top: 10 }
              }
            }
            """);
    }

    private static void assertHeight(String pattern, String equivalent) {
        assertPatternMatches(HeightProvider.CODEC, pattern, equivalent);
    }

    private static void assertColumn(String pattern, String equivalent) {
        assertPatternMatches(ColumnProvider.CODEC, pattern, equivalent);
    }

    private static <T> void assertPatternMatches(Codec<T> type, String pattern, String equivalent) {
        final var r1 = parse(type, pattern);
        assertSuccess(r1);

        final var r2 = parse(type, equivalent);
        assertSuccess(r2);

        assertEquals(r2.getOrThrow(), r1.getOrThrow());
    }
}
