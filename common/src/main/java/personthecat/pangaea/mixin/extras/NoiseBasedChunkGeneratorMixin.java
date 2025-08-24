package personthecat.pangaea.mixin.extras;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.mixin.accessor.StructureManagerAccessor;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Supplier;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {

    @Inject(
        method = "applyCarvers",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getPos()Lnet/minecraft/world/level/ChunkPos;"))
    private void injectCarverContext(
            WorldGenRegion level,
            long seed,
            RandomState state,
            BiomeManager biomes,
            StructureManager structures,
            ChunkAccess chunk,
            Carving step,
            CallbackInfo ci,
            @Local WorldgenRandom rand,
            @Share("ctx") LocalRef<PangaeaContext> target) {
        target.set(PangaeaContext.init(level, rand, (ProtoChunk) chunk, ((ChunkGenerator) (Object) this)));
    }

    // Ordinarily, carvers only use providers in the origin chunk.
    // This solution is otherwise brittle, but will not affect vanilla or PG features.
    @Inject(
        method = "applyCarvers",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/WorldGenRegion;getChunk(II)Lnet/minecraft/world/level/chunk/ChunkAccess;"))
    private void updateCarverPos(
            CallbackInfo ci,
            @Local(ordinal = 1) ChunkPos currentPos,
            @Share("ctx") LocalRef<PangaeaContext> ctx) {
        ctx.get().targetPos.set(currentPos);
    }

    @ModifyArg(
        method = "fillFromNoise",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;wrapThreadWithTaskName(Ljava/lang/String;Ljava/util/function/Supplier;)Ljava/util/function/Supplier;"),
        index = 1)
    private Supplier<ChunkAccess> injectNoiseContext(
            Supplier<ChunkAccess> task,
            @Local(argsOnly = true) StructureManager structures,
            @Local(argsOnly = true) ChunkAccess chunk) {
        return () -> {
            final var level = ((StructureManagerAccessor) structures).getLevel();
            final var ctx = PangaeaContext.init((WorldGenLevel) level, (ProtoChunk) chunk, ((ChunkGenerator) (Object) this));
            ctx.rand.setLargeFeatureSeed(ctx.seed - 1L, ctx.chunkX, ctx.chunkZ);
            return task.get();
        };
    }

    @Inject(method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;buildSurface(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/WorldGenerationContext;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/core/Registry;Lnet/minecraft/world/level/levelgen/blending/Blender;)V"))
    private void injectSurfaceContext(WorldGenRegion level, StructureManager structures, RandomState rand, ChunkAccess chunk, CallbackInfo ci) {
        final var ctx = PangaeaContext.init(level, (ProtoChunk) chunk, ((ChunkGenerator) (Object) this));
        ctx.rand.setLargeFeatureSeed(ctx.seed - 2L, ctx.chunkX, ctx.chunkZ);
    }
}
