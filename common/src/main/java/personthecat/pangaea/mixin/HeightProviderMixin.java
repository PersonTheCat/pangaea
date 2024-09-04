package personthecat.pangaea.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.pangaea.serialization.codec.PatternHeightProviderCodec;

// priority: lower in case another mod still needs original as a MapCodecCodec
@Mixin(value = HeightProvider.class, priority = 1500)
public class HeightProviderMixin {

    @ModifyExpressionValue(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;dispatch(Ljava/util/function/Function;Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"),
        remap = false)
    private static Codec<HeightProvider> createDispatchCodec(Codec<HeightProvider> original) {
        return PatternHeightProviderCodec.wrap(original);
    }
}
