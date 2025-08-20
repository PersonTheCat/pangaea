package personthecat.pangaea.mixin.extras;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.extras.ChunkGeneratorExtras;
import personthecat.pangaea.world.level.PangaeaContext;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin implements ChunkGeneratorExtras {

    @Inject(
        method = "applyBiomeDecoration",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;setDecorationSeed(JII)J"))
    private void injectPangaeaContext(
            WorldGenLevel level,
            ChunkAccess chunk,
            StructureManager structures,
            CallbackInfo ci,
            @Local WorldgenRandom rand,
            @Share("ctx") LocalRef<PangaeaContext> ctx) {
        ctx.set(PangaeaContext.init(level, rand, (ProtoChunk) chunk, ((ChunkGenerator) (Object) this)));
    }

    @Inject(
        method = "applyBiomeDecoration",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;setFeatureSeed(JII)V"))
    private void captureFeatureIndex(
            WorldGenLevel level,
            ChunkAccess chunk,
            StructureManager structures,
            CallbackInfo ci,
            @Share("ctx") LocalRef<PangaeaContext> ctx) {
        ctx.get().featureIndex.increment();
    }

    @Override
    public int pangaea$getOptimizedHeight(ChunkAccess chunk, RandomState rand, int blockX, int blockZ) {
        return this.getBaseHeight(blockX, blockZ, Heightmap.Types.OCEAN_FLOOR_WG, chunk, rand);
    }

    @Shadow
    public abstract int getBaseHeight(int blockX, int blockZ, Heightmap.Types type, LevelHeightAccessor level, RandomState rand);
}
