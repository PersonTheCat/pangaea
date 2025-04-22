package personthecat.pangaea.mixin.extras;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.world.level.GenerationContext;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {

    @Inject(
        method = "applyBiomeDecoration",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;setDecorationSeed(JII)J"))
    private void injectGenerationContext(
            WorldGenLevel level,
            ChunkAccess chunk,
            StructureManager structures,
            CallbackInfo ci,
            @Local WorldgenRandom rand,
            @Share("ctx") LocalRef<GenerationContext> ctx) {
        ctx.set(GenerationContext.init(level, rand, (ProtoChunk) chunk, ((ChunkGenerator) (Object) this)));
    }

    @Inject(
        method = "applyBiomeDecoration",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;setFeatureSeed(JII)V"))
    private void captureFeatureIndex(
            WorldGenLevel level,
            ChunkAccess chunk,
            StructureManager structures,
            CallbackInfo ci,
            @Share("ctx") LocalRef<GenerationContext> ctx) {
        ctx.get().featureIndex.increment();
    }
}
