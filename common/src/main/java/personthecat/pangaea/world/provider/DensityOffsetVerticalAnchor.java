package personthecat.pangaea.world.provider;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import personthecat.pangaea.world.density.AutoWrapDensity;

import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record DensityOffsetVerticalAnchor(
        VerticalAnchor reference, DensityFunction offset) implements ScalableVerticalAnchor {
    public static final Codec<DensityOffsetVerticalAnchor> CODEC = codecOf(
        field(VerticalAnchor.CODEC, "reference", DensityOffsetVerticalAnchor::reference),
        field(AutoWrapDensity.HELPER_CODEC, "offset", DensityOffsetVerticalAnchor::offset),
        DensityOffsetVerticalAnchor::new
    ).codec();

    public static Codec<VerticalAnchor> wrapCodec(Codec<VerticalAnchor> codec) {
        return Codec.xor(codec, CODEC).xmap(
            e -> e.map(Function.identity(), Function.identity()),
            a -> a instanceof DensityOffsetVerticalAnchor d ? Either.right(d) : Either.left(a)
        );
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
