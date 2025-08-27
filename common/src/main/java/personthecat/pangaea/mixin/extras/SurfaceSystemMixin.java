package personthecat.pangaea.mixin.extras;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.extras.ContextExtras;
import personthecat.pangaea.world.level.PangaeaContext;

@Mixin(SurfaceSystem.class)
public class SurfaceSystemMixin {

    @Inject(method = "buildSurface", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/SurfaceRules$RuleSource;apply(Ljava/lang/Object;)Ljava/lang/Object;"))
    private void loadContextExtras(
            CallbackInfo ci, @Local Context ctx, @Local(argsOnly = true) WorldGenerationContext wgc) {
        ContextExtras.load(ctx, PangaeaContext.get(wgc));
    }
}
