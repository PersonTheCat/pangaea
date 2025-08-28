package personthecat.pangaea.mixin.extras;

import net.minecraft.world.level.levelgen.feature.treedecorators.BeehiveDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeehiveDecorator.class)
public class BeehiveDecoratorMixin {

    // DELETE ME: Patch to fix beehive placement crash on 1.20.6
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void checkLogs(TreeDecorator.Context ctx, CallbackInfo ci) {
        if (ctx.logs().isEmpty()) {
            ci.cancel();
        }
    }
}
