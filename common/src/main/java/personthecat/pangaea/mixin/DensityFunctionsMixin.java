package personthecat.pangaea.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.pangaea.serialization.codec.DensityFunctionBuilder;
import personthecat.pangaea.serialization.codec.StructuralDensityCodec;

@Mixin(DensityFunctions.class)
public class DensityFunctionsMixin {

    @ModifyExpressionValue(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;dispatch(Ljava/util/function/Function;Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"),
        remap = false)
    private static Codec<DensityFunction> createCodec(Codec<DensityFunction> original) {
        return DensityFunctionBuilder.wrap(StructuralDensityCodec.wrap(original));
    }
}
