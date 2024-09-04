package personthecat.pangaea.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.pangaea.serialization.codec.AltBlockStateCodec;

// priority: lower in case another mod still needs original as a MapCodec
@Mixin(value = BlockState.class, priority = 1500)
public class BlockStateMixin {

    @ModifyExpressionValue(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;stable()Lcom/mojang/serialization/Codec;"),
        remap = false)
    private static Codec<BlockState> createCodec(Codec<BlockState> original) {
        return AltBlockStateCodec.wrap(original);
    }
}
