package personthecat.pangaea.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.pangaea.world.provider.DensityVerticalAnchor;

@Mixin(VerticalAnchor.class)
public class VerticalAnchorMixin {

    @ModifyExpressionValue(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;xor(Lcom/mojang/serialization/Codec;Lcom/mojang/serialization/Codec;)Lcom/mojang/serialization/Codec;"),
        remap = false)
    private static Codec<VerticalAnchor> createCodec(Codec<VerticalAnchor> original) {
        return Codec.lazyInitialized(() -> DensityVerticalAnchor.wrapCodec(original));
    }
}
