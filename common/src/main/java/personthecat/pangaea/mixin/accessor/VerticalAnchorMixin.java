package personthecat.pangaea.mixin.accessor;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.pangaea.world.provider.DensityOffsetVerticalAnchor;
import personthecat.pangaea.world.provider.DensityVerticalAnchor;
import personthecat.pangaea.world.provider.SeaLevelVerticalAnchor;
import personthecat.pangaea.world.provider.SurfaceVerticalAnchor;

@Mixin(VerticalAnchor.class)
public interface VerticalAnchorMixin {

    @ModifyExpressionValue(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;xmap(Ljava/util/function/Function;Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"),
        remap = false)
    private static Codec<VerticalAnchor> modify(Codec<VerticalAnchor> original) {
        return Codec.lazyInitialized(() ->
            addSeaLevelOffset(addSurfaceOffset((addDensityOffsetType(addDensityType(original))))));
    }

    @Unique
    private static Codec<VerticalAnchor> addDensityType(Codec<VerticalAnchor> original) {
        return DensityVerticalAnchor.wrapCodec(original);
    }

    @Unique
    private static Codec<VerticalAnchor> addDensityOffsetType(Codec<VerticalAnchor> original) {
        return DensityOffsetVerticalAnchor.wrapCodec(original);
    }

    @Unique
    private static Codec<VerticalAnchor> addSurfaceOffset(Codec<VerticalAnchor> original) {
        return SurfaceVerticalAnchor.wrapCodec(original);
    }

    @Unique
    private static Codec<VerticalAnchor> addSeaLevelOffset(Codec<VerticalAnchor> original) {
        return SeaLevelVerticalAnchor.wrapCodec(original);
    }
}
