package personthecat.pangaea.mixin.extras;

import com.google.common.base.Suppliers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.extras.ContextExtras;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(Context.class)
public abstract class ContextMixin implements ContextExtras {
    @Shadow private @Final Function<BlockPos, Holder<Biome>> biomeGetter;
    @Shadow public @Final BlockPos.MutableBlockPos pos;
    @Unique private PangaeaContext pangaea$pangaea;
    @Unique private Supplier<Holder<Biome>> pangaea$surfaceBiome;

    @Inject(method = "updateXZ", at = @At("TAIL"))
    private void updateHorizontal(int x, int z, CallbackInfo ci) {
        this.pangaea$surfaceBiome = Suppliers.memoize(() ->
            this.biomeGetter.apply(this.pos.set(x, 1_000_000, z)));
        this.pangaea$pangaea.targetPos.at(x, z);
    }

    @Inject(method = "updateY", at = @At("TAIL"))
    private void updateVertical(
            int stoneDepthAbove, int stoneDepthBelow, int waterHeight, int x, int y, int z, CallbackInfo ci) {
        this.pangaea$pangaea.targetPos.at(x, y, z);
    }

    @Override
    public Supplier<Holder<Biome>> pangaea$surfaceBiome() {
        return this.pangaea$surfaceBiome;
    }

    @Override
    public PangaeaContext pangaea$getPangaea() {
        return this.pangaea$pangaea; // duplicate in case original wgc is replaced
    }

    @Override
    public void pangaea$load(PangaeaContext pg) {
        this.pangaea$pangaea = pg;
    }
}
