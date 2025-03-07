package personthecat.pangaea.world.provider;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import personthecat.catlib.serialization.codec.CodecUtils;
import personthecat.pangaea.world.density.AutoWrapDensity;

import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.asParent;

public sealed interface DensityVerticalAnchor extends ScalableVerticalAnchor {
    Codec<DensityVerticalAnchor> CODEC =
        Codec.xor(Absolute.CODEC, Codec.xor(AboveBottom.CODEC, BelowTop.CODEC))
            .xmap(DensityVerticalAnchor::merge, DensityVerticalAnchor::split);

    static Codec<VerticalAnchor> wrapCodec(Codec<VerticalAnchor> codec) {
        return CodecUtils.simpleEither(codec, asParent(CODEC))
            .withEncoder(a -> a instanceof DensityVerticalAnchor ? CODEC : codec);
    }

    private static DensityVerticalAnchor merge(Either<Absolute, Either<AboveBottom, BelowTop>> either) {
        return either.map(Function.identity(), Either::unwrap);
    }

    private static Either<Absolute, Either<AboveBottom, BelowTop>> split(DensityVerticalAnchor anchor) {
        if (anchor instanceof Absolute a) return Either.left(a);
        if (anchor instanceof AboveBottom a) return Either.right(Either.left(a));
        return Either.right(Either.right((BelowTop) anchor));
    }

    record Absolute(DensityFunction y) implements DensityVerticalAnchor {
        public static final Codec<Absolute> CODEC =
            AutoWrapDensity.HELPER_CODEC.fieldOf("absolute")
                .xmap(Absolute::new, Absolute::y)
                .codec();

        @Override
        public int resolveY(WorldGenerationContext gen, FunctionContext fn) {
            return (int) this.y.compute(fn);
        }

        @Override
        public String toString() {
            return this.y + " absolute";
        }
    }

    record AboveBottom(DensityFunction offset) implements DensityVerticalAnchor {
        public static final Codec<AboveBottom> CODEC =
            AutoWrapDensity.HELPER_CODEC.fieldOf("above_bottom")
                .xmap(AboveBottom::new, AboveBottom::offset)
                .codec();

        @Override
        public int resolveY(WorldGenerationContext gen, FunctionContext fn) {
            return gen.getMinGenY() + (int) this.offset.compute(fn);
        }

        @Override
        public String toString() {
            return this.offset + " above bottom";
        }
    }

    record BelowTop(DensityFunction offset) implements DensityVerticalAnchor {
        public static final Codec<BelowTop> CODEC =
            AutoWrapDensity.HELPER_CODEC.fieldOf("below_top")
                .xmap(BelowTop::new, BelowTop::offset)
                .codec();


        @Override
        public int resolveY(WorldGenerationContext gen, FunctionContext fn) {
            return gen.getGenDepth() - 1 + gen.getMinGenY() - (int) this.offset.compute(fn);
        }

        @Override
        public String toString() {
            return this.offset + " below top";
        }
    }
}
