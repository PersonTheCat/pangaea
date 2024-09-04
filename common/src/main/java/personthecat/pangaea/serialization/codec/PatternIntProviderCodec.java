package personthecat.pangaea.serialization.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import personthecat.catlib.data.Range;
import personthecat.pangaea.config.Cfg;

import java.util.function.Function;

public final class PatternIntProviderCodec {
    private PatternIntProviderCodec() {}

    public static Codec<IntProvider> wrap(Codec<IntProvider> codec) {
        return Codec.either(Range.CODEC, codec).xmap(
            either -> either.map(range -> UniformInt.of(range.min, range.max), Function.identity()),
            i -> i instanceof UniformInt u && Cfg.encodeRangeIntProvider()
                ? Either.left(Range.of(u.getMinValue(), u.getMaxValue())) : Either.right(i));
    }
}
