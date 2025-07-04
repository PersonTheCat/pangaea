package personthecat.pangaea.serialization.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import personthecat.catlib.data.FloatRange;
import personthecat.pangaea.config.Cfg;

import java.util.function.Function;

public final class PatternFloatProviderCodec {
    private PatternFloatProviderCodec() {}

    public static Codec<FloatProvider> wrap(Codec<FloatProvider> codec) {
        return Codec.either(codec, FloatRange.CODEC).xmap(
            either -> either.map(Function.identity(), range -> UniformFloat.of(range.min(), range.max())),
            i -> i instanceof UniformFloat u && Cfg.encodeRangeFloatProvider()
                ? Either.right(FloatRange.of(u.getMinValue(), u.getMaxValue())) : Either.left(i));
    }
}
