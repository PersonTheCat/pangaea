package personthecat.pangaea.mixin.codec;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.util.valueproviders.FloatProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.pangaea.serialization.codec.PatternFloatProviderCodec;
import personthecat.pangaea.serialization.codec.StructuralFloatProviderCodec;

// priority: lower in case another mod still needs original as a MapCodecCodec
@Mixin(value = FloatProvider.class, priority = 1500)
public class FloatProviderMixin {

    @ModifyExpressionValue(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;dispatch(Ljava/util/function/Function;Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"),
        remap = false)
    private static Codec<FloatProvider> modifyCodec(Codec<FloatProvider> original) {
        return Codec.lazyInitialized(() -> injectRangeSyntax(injectStructuralSyntax(original)));
    }

    @Unique
    private static Codec<FloatProvider> injectStructuralSyntax(Codec<FloatProvider> original) {
        return StructuralFloatProviderCodec.wrap(original);
    }

    @Unique
    private static Codec<FloatProvider> injectRangeSyntax(Codec<FloatProvider> original) {
        return PatternFloatProviderCodec.wrap(original);
    }
}
