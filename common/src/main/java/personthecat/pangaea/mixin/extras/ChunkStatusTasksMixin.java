package personthecat.pangaea.mixin.extras;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.pangaea.world.hooks.GeneratorHooks;
import personthecat.pangaea.world.level.PangaeaContext;

@Mixin(ChunkStatusTasks.class)
public class ChunkStatusTasksMixin {

    @WrapOperation(
        method = "generateCarvers",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;applyCarvers(Lnet/minecraft/server/level/WorldGenRegion;JLnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/GenerationStep$Carving;)V"))
    private static void wrapApplyCarvers(
            ChunkGenerator gen,
            WorldGenRegion level,
            long seed,
            RandomState rand,
            BiomeManager biomes,
            StructureManager structures,
            ChunkAccess chunk,
            Carving step,
            Operation<Void> applyCarvers) {
        final var ctx = PangaeaContext.init(level, (ProtoChunk) chunk, gen);
        GeneratorHooks.initRoadSystem(level, chunk);
        ctx.reset();
        applyCarvers.call(gen, level, seed, rand, biomes, structures, chunk, step);
    }

    @WrapOperation(
        method = "generateFeatures",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;applyBiomeDecoration(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/StructureManager;)V"))
    private static void wrapApplyBiomeDecoration(
            ChunkGenerator gen,
            WorldGenLevel level,
            ChunkAccess chunk,
            StructureManager structures,
            Operation<Void> applyBiomeDecorations) {
        final var ctx = PangaeaContext.init(level, (ProtoChunk) chunk, gen);
        GeneratorHooks.applyGiantFeatures(level, gen, ctx);
        ctx.reset();
        applyBiomeDecorations.call(gen, level, chunk, structures);
    }
}
