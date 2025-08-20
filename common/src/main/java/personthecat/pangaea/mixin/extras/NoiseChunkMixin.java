package personthecat.pangaea.mixin.extras;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.pangaea.extras.NoiseChunkExtras;

@Mixin(NoiseChunk.class)
public abstract class NoiseChunkMixin implements NoiseChunkExtras {

    @Unique private DensityFunction pangaea$capturedDensity;

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/DensityFunction;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/DensityFunction;"))
    private DensityFunction captureDensity(DensityFunction marker, DensityFunction.Visitor visitor, Operation<DensityFunction> mapAll) {
        return this.pangaea$capturedDensity = mapAll.call(marker, visitor);
    }

    @Override
    public double pangaea$computeDensity() {
        return this.pangaea$capturedDensity.compute((NoiseChunk) (Object) this);
    }
}
