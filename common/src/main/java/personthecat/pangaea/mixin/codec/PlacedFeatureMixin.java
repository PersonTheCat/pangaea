package personthecat.pangaea.mixin.codec;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.pangaea.serialization.codec.FeaturesByStageCodec;

import java.util.List;

@Mixin(value = PlacedFeature.class, priority = 1500)
public class PlacedFeatureMixin {

    @ModifyExpressionValue(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;listOf()Lcom/mojang/serialization/Codec;"),
        remap = false)
    private static Codec<List<HolderSet<PlacedFeature>>> modifyCodec(Codec<List<HolderSet<PlacedFeature>>> original) {
        return injectMapSyntax(original);
    }

    @Unique
    private static Codec<List<HolderSet<PlacedFeature>>> injectMapSyntax(Codec<List<HolderSet<PlacedFeature>>> original) {
        return FeaturesByStageCodec.wrap(original);
    }
}
