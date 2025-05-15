package personthecat.pangaea.world.provider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviderType;
import org.jetbrains.annotations.NotNull;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public class BiasedToBottomFloat extends FloatProvider {
    public static final MapCodec<BiasedToBottomFloat> CODEC = codecOf(
        field(Codec.FLOAT, "min_inclusive", f -> f.minInclusive),
        field(Codec.FLOAT, "max_exclusive", f -> f.maxExclusive),
        BiasedToBottomFloat::new
    ).validate(f -> f.minInclusive < f.maxExclusive ? DataResult.success(f)
        : DataResult.error(() -> "Max must be at least min, min: " + f.minInclusive + ", max: " + f.maxExclusive));
    public static final FloatProviderType<BiasedToBottomFloat> TYPE = () -> CODEC;

    private final float minInclusive;
    private final float maxExclusive;

    private BiasedToBottomFloat(float min, float max) {
        this.minInclusive = min;
        this.maxExclusive = max;
    }

    public static BiasedToBottomFloat of(float min, float max) {
        return new BiasedToBottomFloat(min, max);
    }

    @Override
    public float getMinValue() {
        return this.minInclusive;
    }

    @Override
    public float getMaxValue() {
        return this.maxExclusive;
    }

    @Override
    public float sample(RandomSource rand) {
        return this.minInclusive + rand.nextFloat() + (rand.nextFloat() * (this.maxExclusive - this.minInclusive - 1F));
    }

    @Override
    public @NotNull FloatProviderType<?> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "[" + this.minInclusive + "-" + this.maxExclusive + ")";
    }
}
