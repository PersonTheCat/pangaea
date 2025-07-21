package personthecat.pangaea.mixin.codec;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.util.valueproviders.IntProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.pangaea.serialization.codec.PangaeaCodec;

// priority: lower in case another mod still needs original as a MapCodecCodec
@Mixin(value = IntProvider.class, priority = 1500)
public class IntProviderMixin {

    @ModifyExpressionValue(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;dispatch(Ljava/util/function/Function;Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"),
        remap = false)
    private static Codec<IntProvider> modifyCodec(Codec<IntProvider> original) {
        return PangaeaCodec.create(original);
    }
}
