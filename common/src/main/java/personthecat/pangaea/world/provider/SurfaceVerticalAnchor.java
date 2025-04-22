package personthecat.pangaea.world.provider;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Function;

public record SurfaceVerticalAnchor(int offset) implements ScalableVerticalAnchor {
    public static final Codec<SurfaceVerticalAnchor> CODEC =
        Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("surface")
            .xmap(SurfaceVerticalAnchor::new, SurfaceVerticalAnchor::offset)
            .codec();

    public static Codec<VerticalAnchor> wrapCodec(Codec<VerticalAnchor> codec) {
        return Codec.xor(codec, CODEC).xmap(
            e -> e.map(Function.identity(), Function.identity()),
            a -> a instanceof SurfaceVerticalAnchor s ? Either.right(s) : Either.left(a)
        );
    }

    @Override
    public int resolveY(WorldGenerationContext gen, FunctionContext ctx) {
        return PangaeaContext.get(gen).getHeight(ctx.blockX(), ctx.blockZ()) + this.offset;
    }

    @Override
    public String toString() {
        if (this.offset == 0) return "surface";
        if (this.offset > 0) return this.offset + " above surface";
        return -this.offset + " below surface";
    }
}
