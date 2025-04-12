package personthecat.pangaea.world.provider;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import personthecat.pangaea.world.density.AutoWrapDensity;

import java.util.function.Function;

public record DensityVerticalAnchor(DensityFunction density) implements ScalableVerticalAnchor {
    public static final Codec<DensityVerticalAnchor> CODEC =
        AutoWrapDensity.HELPER_CODEC.fieldOf("density")
            .xmap(DensityVerticalAnchor::new, DensityVerticalAnchor::density)
            .codec();

    public static Codec<VerticalAnchor> wrapCodec(Codec<VerticalAnchor> codec) {
        return Codec.xor(codec, CODEC).xmap(
            e -> e.map(Function.identity(), Function.identity()),
            a -> a instanceof DensityVerticalAnchor d ? Either.right(d) : Either.left(a)
        );
    }

    @Override
    public int resolveY(WorldGenerationContext gen, FunctionContext fn) {
        return (int) this.density.compute(fn);
    }

    @Override
    public String toString() {
        return this.density.toString();
    }
}
