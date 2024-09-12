package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.DensityFunction;

public final class FunctionCodecs {
    private FunctionCodecs() {}

    public static final Codec<FunctionCodec.Template<DensityFunction>> DENSITY =
        FunctionCodec.create(DensityFunction.HOLDER_HELPER_CODEC);
}
