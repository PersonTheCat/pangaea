package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Direct;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions.HolderHolder;

public final class DensityHelper {
    // Optimization: the result is either a reference or raw density, never holder$direct
    public static final Codec<DensityFunction> CODEC = Codec.lazyInitialized(() ->
        DensityFunction.HOLDER_HELPER_CODEC.xmap(
            f -> f.mapAll(f2 -> f2 instanceof HolderHolder(Direct<DensityFunction> d) ? d.value() : f2),
            f -> f.mapAll(f2 -> f2 instanceof HolderHolder ? f2 : new HolderHolder(Holder.direct(f2)))
        )
    );

    private DensityHelper() {}
}
