package personthecat.pangaea.world.provider;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.world.density.AutoWrapDensity;

import java.util.Optional;
import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.asMapCodec;
import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.ifMap;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public record DensityOffsetVerticalAnchor(
        VerticalAnchor reference, DensityFunction offset) implements ScalableVerticalAnchor {
    public static final Codec<DensityOffsetVerticalAnchor> CODEC = codecOf(
        field(VerticalAnchor.CODEC, "reference", DensityOffsetVerticalAnchor::reference),
        field(AutoWrapDensity.HELPER_CODEC, "offset", DensityOffsetVerticalAnchor::offset),
        DensityOffsetVerticalAnchor::new
    ).codec();

    public static Codec<VerticalAnchor> wrapCodec(Codec<VerticalAnchor> codec) {
        final var xor = Codec.xor(codec, CODEC).xmap(
            e -> e.map(Function.identity(), Function.identity()),
            a -> a instanceof DensityOffsetVerticalAnchor d ? Either.right(d) : Either.left(a)
        );
        final var nullableOffset = AutoWrapDensity.HELPER_CODEC.optionalFieldOf("offset");
        final var addedField = codecOf(
            union(asMapCodec(codec), DensityOffsetVerticalAnchor::getReference),
            union(nullableOffset, DensityOffsetVerticalAnchor::getOffset),
            DensityOffsetVerticalAnchor::applyOffset
        );
        return ifMap(xor, addedField, DensityOffsetVerticalAnchor::encodeAsDensityOffset);
    }

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
