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
        return Codec.either(FloatRange.CODEC, codec).xmap(
            either -> either.map(range -> UniformFloat.of(range.min(), range.max()), Function.identity()),
            i -> i instanceof UniformFloat u && Cfg.encodeRangeFloatProvider()
                ? Either.left(FloatRange.of(u.getMinValue(), u.getMaxValue())) : Either.right(i));
    }
}
