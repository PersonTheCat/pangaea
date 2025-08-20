package personthecat.pangaea.mixin.extras;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import personthecat.pangaea.extras.ChunkAccessExtras;
import personthecat.pangaea.extras.ChunkGeneratorExtras;
import personthecat.pangaea.extras.NoiseChunkExtras;
import personthecat.pangaea.mixin.accessor.StructureManagerAccessor;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Supplier;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin implements ChunkGeneratorExtras {
    @Shadow @Final private Holder<NoiseGeneratorSettings> settings;
    @Shadow @Final private Supplier<Aquifer.FluidPicker> globalFluidPicker;

    @Inject(
        method = "applyCarvers",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getPos()Lnet/minecraft/world/level/ChunkPos;"))
    private void injectPangaeaContext(
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
    private Supplier<ChunkAccess> wrapDoFill(
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

    @Override
    public int pangaea$getOptimizedHeight(ChunkAccess chunk, RandomState rand, int blockX, int blockZ) {
        final var test = ChunkAccessExtras.getApproximateHeight(chunk, blockX & 15, blockZ & 15);
        if (test == 0) {
            this.pangaea$computeHeightValues(chunk, rand);
        }
        return ChunkAccessExtras.getInterpolatedHeight(chunk, blockX & 15, blockZ & 15);
    }

    @Unique
    private void pangaea$computeHeightValues(ChunkAccess chunk, RandomState rand) {
        final var settings = this.settings.value();
        final var noise = settings.noiseSettings().clampToHeightAccessor(chunk);
        final var extras = ChunkAccessExtras.get(chunk);

        final var noiseChunk =
            NoiseChunk.forChunk(chunk, rand, DensityFunctions.BeardifierMarker.INSTANCE, settings, this.globalFluidPicker.get(), Blender.empty());

        final int cellHeight = noise.getCellHeight();
        final int cellWidth = noise.getCellWidth();

        final var pos = chunk.getPos();
        int minBlockX = pos.getMinBlockX();
        int minBlockZ = pos.getMinBlockZ();

        int minY = noise.minY();
        int cellMinY = Math.floorDiv(minY, cellHeight);
        int cellCountY = Math.floorDiv(noise.height(), cellHeight);

        final int seaLevel = settings.seaLevel();
        noiseChunk.initializeForFirstCellX();

        int lastCellX = 0;
        for (int localX = 0; localX <= 16; localX += 4) {
            if (localX == 16) localX = 15; // compute edges for interpolation

            int blockX = minBlockX + localX;
            int cellX = localX / cellWidth;

            // if cell advanced, update slices
            if (lastCellX != cellX) noiseChunk.swapSlices();
            lastCellX = cellX;

            noiseChunk.advanceCellX(cellX);

            for (int localZ = 0; localZ <= 16; localZ += 4) {
                if (localZ == 16) localZ = 15;

                int blockZ = minBlockZ + localZ;
                int cellZ = localZ / cellWidth;

                // Assume neighbor heights will always be in +- 16 blocks
                int guessMin = Integer.MAX_VALUE;
                int guessMax = Integer.MIN_VALUE;
                int margin = 16;

                // check four direct neighbors that may already be sampled
                int[][] offsets = {{-4,0},{4,0},{0,-4},{0,4}};
                for (int[] off : offsets) {
                    int nx = blockX + off[0];
                    int nz = blockZ + off[1];
                    // make sure we're in the same chunk
                    if (nx >> 4 != blockX >> 4 || nz >> 4 != blockZ >> 16) {
                        continue;
                    }
                    int neighbor = extras.pangaea$getApproximateHeight(nx & 15, nz & 15);
                    if (neighbor != 0) { // 0 = unsampled
                        guessMin = Math.min(guessMin, neighbor);
                        guessMax = Math.max(guessMax, neighbor);
                    }
                }

                final int minScanY;
                final int maxScanY;
                if (guessMin != Integer.MAX_VALUE) {
                    // use neighbor band +- margin
                    minScanY = guessMin - margin;
                    maxScanY = guessMax + margin;
                } else {
                    // fallback: full bounds if we didn't find any neighbors
                    minScanY = seaLevel;
                    maxScanY = seaLevel * 2;
                }
                int startCellY = Math.max(cellMinY, Math.floorDiv(minScanY - minY, cellHeight));
                int endCellY = Math.min(cellCountY - 1, Math.floorDiv(maxScanY - minY, cellHeight));

                column: for (int cellY = endCellY; cellY >= startCellY; --cellY) {
                    noiseChunk.selectCellYZ(cellY, cellZ);

                    for (int yInCell = cellHeight - 1; yInCell >= 0; yInCell--) {
                        final int blockY = (cellMinY + cellY) * cellHeight + yInCell;
                        final double fracY = (double) yInCell / (double) cellHeight;

                        noiseChunk.updateForY(blockY, fracY);
                        noiseChunk.updateForX(blockX, 0);
                        noiseChunk.updateForZ(blockZ, 0);

                        if (NoiseChunkExtras.computeDensity(noiseChunk) > 0) {
                            extras.pangaea$setApproximateHeight(blockX & 15, blockZ & 15, blockY + 1);
                            break column;
                        }
                    }
                }
            }
        }
        noiseChunk.stopInterpolation();
    }
}
