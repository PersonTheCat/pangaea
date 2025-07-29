package personthecat.pangaea.world.provider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.world.density.AutoWrapDensity;

import java.util.Optional;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record DensityOffsetVerticalAnchor(
        VerticalAnchor reference, DensityFunction offset) implements ScalableVerticalAnchor {
    public static final Codec<DensityOffsetVerticalAnchor> CODEC = codecOf(
        field(VerticalAnchor.CODEC, "reference", DensityOffsetVerticalAnchor::reference),
        field(AutoWrapDensity.HELPER_CODEC, "offset", DensityOffsetVerticalAnchor::offset),
        DensityOffsetVerticalAnchor::new
    ).codec();

    private static VerticalAnchor getReference(VerticalAnchor a) {
        return a instanceof DensityOffsetVerticalAnchor o ? o.reference : a;
    }

    private static Optional<DensityFunction> getOffset(VerticalAnchor a) {
        return a instanceof DensityOffsetVerticalAnchor o ? Optional.of(o.offset) : Optional.empty();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static VerticalAnchor applyOffset(VerticalAnchor reference, Optional<DensityFunction> offset) {
        return offset.<VerticalAnchor>map(o -> new DensityOffsetVerticalAnchor(reference, o)).orElse(reference);
    }

    private static boolean encodeAsDensityOffset(VerticalAnchor a, DynamicOps<?> o) {
        return a instanceof DensityOffsetVerticalAnchor && Cfg.encodeVerticalAnchorBuilders();
    }

    @Override
    public int resolveY(WorldGenerationContext gen, DensityFunction.FunctionContext fn) {
        return this.reference.resolveY(gen) + (int) this.offset.compute(fn);
    }

    @Override
    public String toString() {
        return this.reference + " + " + this.offset;
    }
}
