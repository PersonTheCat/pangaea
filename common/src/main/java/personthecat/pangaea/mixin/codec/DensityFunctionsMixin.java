package personthecat.pangaea.mixin.codec;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.DensityFunctionBuilder;
import personthecat.pangaea.serialization.codec.FunctionCodec;
import personthecat.pangaea.serialization.codec.StructuralDensityCodec;

// priority: higher in case another mod doesn't replace original with a MapCodecCodec
@Mixin(value = DensityFunctions.class, priority = 500)
public class DensityFunctionsMixin {

    @ModifyExpressionValue(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;dispatch(Ljava/util/function/Function;Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"),
        remap = false)
    private static Codec<DensityFunction> createCodec(Codec<DensityFunction> original) {
        return injectBuilderSyntax(injectStructuralSyntax(injectDensitySyntax(original)));
    }

    @Unique
    private static Codec<DensityFunction> injectDensitySyntax(Codec<DensityFunction> original) {
        return FunctionCodec.wrap(original, PgRegistries.Keys.DENSITY_TEMPLATE);
    }

    @Unique
    private static Codec<DensityFunction> injectStructuralSyntax(Codec<DensityFunction> original) {
        return StructuralDensityCodec.wrap(original);
    }

    @Unique
    private static Codec<DensityFunction> injectBuilderSyntax(Codec<DensityFunction> original) {
        return DensityFunctionBuilder.wrap(original);
    }
}
