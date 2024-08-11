package personthecat.pangaea.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DensityFunctions.class)
public interface DensityFunctionsAccessor {

    @Accessor("CODEC")
    static Codec<DensityFunction> getCodec() {
        throw new AssertionError("Mixin not applied");
    }
}
