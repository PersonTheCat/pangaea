package personthecat.pangaea.serialization.extras;

import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import personthecat.pangaea.test.McBootstrapExtension;
import personthecat.pangaea.world.provider.DensityOffsetHeightProvider;
import personthecat.pangaea.world.provider.DensityOffsetVerticalAnchor;

import static personthecat.pangaea.test.TestUtils.assertSuccess;
import static personthecat.pangaea.test.TestUtils.parse;

@ExtendWith(McBootstrapExtension.class)
public final class DefaultCodecFlagsTest {

    @Test
    public void density_withNoFlags_canParseNormally() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "type: 'constant', argument: 1");
        assertSuccess(DensityFunctions.constant(1), result);
    }

    @Test
    public void density_withFlags_appendsEachFlag() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "type: 'constant', argument: 1, blend: true, interpolate: true");
        assertSuccess(DensityFunctions.blendDensity(DensityFunctions.interpolated(DensityFunctions.constant(1))), result);
    }

    @Test
    public void density_whenInputIsNotAMap_canParseNormally() {
        final var result = parse(DensityFunctions.DIRECT_CODEC, "1");
        assertSuccess(DensityFunctions.constant(1), result);
    }

    @Test
    public void anchor_withFlags_appendsEachFlag() {
        final var result = parse(VerticalAnchor.CODEC, "top: -1, offset: -2");
        assertSuccess(new DensityOffsetVerticalAnchor(VerticalAnchor.belowTop(1), DensityFunctions.constant(-2)), result);
    }

    @Test
    public void anchor_whenInputIsNotAMap_canParseNormally() {
        final var result = parse(VerticalAnchor.CODEC, "123"); // not technically normal, but relevant
        assertSuccess(VerticalAnchor.absolute(123), result);
    }

    @Test
    public void height_withFlags_appendsEachFlag() {
        final var result = parse(HeightProvider.CODEC, "bottom: [1, 2], offset: 3");
        assertSuccess(
            new DensityOffsetHeightProvider(
                TrapezoidHeight.of(VerticalAnchor.aboveBottom(1), VerticalAnchor.aboveBottom(2)),
                DensityFunctions.constant(3)),
            result,
            true);
    }

    @Test
    public void height_whenInputIsNotAMap_canParseNormally() {
        final var result = parse(HeightProvider.CODEC, "[ 1, 2 ]");
        assertSuccess(TrapezoidHeight.of(VerticalAnchor.absolute(1), VerticalAnchor.absolute(2)), result);
    }
}
