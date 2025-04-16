package personthecat.pangaea.mixin.extras;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import personthecat.pangaea.world.level.ScopeExtension;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

@Mixin(PlacedFeature.class)
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
}
