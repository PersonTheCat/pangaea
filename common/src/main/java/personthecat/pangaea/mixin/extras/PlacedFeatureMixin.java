package personthecat.pangaea.mixin.extras;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

@Mixin(PlacedFeature.class)
public class PlacedFeatureMixin {

    @Inject(
        method = "placeWithContext",
        at = @At("HEAD"))
    private void getGeneratingPos(
            CallbackInfoReturnable<Boolean> cir,
            @Share("pos") LocalRef<MutableFunctionContext> target) {
        target.set(PangaeaContext.get().targetPos);
    }

    // Wrapping lambdas to avoid having different mixins on each platform
    @ModifyArg(
        method = "placeWithContext",
        at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;flatMap(Ljava/util/function/Function;)Ljava/util/stream/Stream;"))
    private Function<BlockPos, Stream<BlockPos>> updatePosForModifier(
            Function<BlockPos, Stream<BlockPos>> function,
            @Share("pos") LocalRef<MutableFunctionContext> target) {
        return pos -> {
            target.get().set(pos);
            return function.apply(pos);
        };
    }

    @ModifyArg(
        method = "placeWithContext",
        at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"))
    private Consumer<BlockPos> updatePosForPlacement(
            Consumer<BlockPos> action,
            @Share("pos") LocalRef<MutableFunctionContext> target) {
        return pos -> {
            target.get().set(pos);
            action.accept(pos);
        };
    }
}
