package personthecat.pangaea.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import personthecat.pangaea.serialization.codec.FeaturesByStageCodec;
import personthecat.pangaea.world.level.ScopeExtension;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings("preview")
@Mixin(value = PlacedFeature.class, priority = 1500)
public class PlacedFeatureMixin {

    // Wrapping lambdas to avoid having different mixins on each platform
    @ModifyArg(
        method = "placeWithContext",
        at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;flatMap(Ljava/util/function/Function;)Ljava/util/stream/Stream;"))
    private Function<BlockPos, Stream<BlockPos>> updatePosForModifier(
            Function<BlockPos, Stream<BlockPos>> function) {
        return pos -> {
            ScopeExtension.GENERATING_POS.get().set(pos);
            return function.apply(pos);
        };
    }

    @ModifyArg(
        method = "placeWithContext",
        at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"))
    private Consumer<BlockPos> updatePosForPlacement(Consumer<BlockPos> action) {
        return pos -> {
            ScopeExtension.GENERATING_POS.get().set(pos);
            action.accept(pos);
        };
    }

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
