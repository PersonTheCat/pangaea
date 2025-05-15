package personthecat.pangaea.world.provider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviderType;
import org.jetbrains.annotations.NotNull;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public class VeryBiasedToBottomInt extends IntProvider {
    public static final MapCodec<VeryBiasedToBottomInt> CODEC = codecOf(
        field(Codec.INT, "min_inclusive", f -> f.minInclusive),
        field(Codec.INT, "max_inclusive", f -> f.maxInclusive),
        VeryBiasedToBottomInt::new
    ).validate(f -> f.minInclusive < f.maxInclusive ? DataResult.success(f)
        : DataResult.error(() -> "Max must be at least min, min: " + f.minInclusive + ", max: " + f.maxInclusive));
    public static final IntProviderType<VeryBiasedToBottomInt> TYPE = () -> CODEC;
    private final int minInclusive;
    private final int maxInclusive;

    private VeryBiasedToBottomInt(int min, int max) {
        this.minInclusive = min;
        this.maxInclusive = max;
    }

    public static VeryBiasedToBottomInt of(int min, int max) {
        return new VeryBiasedToBottomInt(min, max);
    }

    @Override
    public int sample(RandomSource rand) {
        return this.minInclusive + rand.nextInt(rand.nextInt(rand.nextInt(this.maxInclusive - this.minInclusive + 1) + 1) + 1);
    }

    @Override
    public int getMinValue() {
        return this.minInclusive;
    }

    @Override
    public int getMaxValue() {
        return this.maxInclusive;
    }

    @Override
    public @NotNull IntProviderType<?> getType() {
        return IntProviderType.BIASED_TO_BOTTOM;
    }

    @Override
    public String toString() {
        return "[" + this.minInclusive + "-" + this.maxInclusive + "]";
    }
}
