package personthecat.pangaea.mixin.extras;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.mixin.accessor.StructureManagerAccessor;
import personthecat.pangaea.world.level.ScopeExtension;

import java.util.function.Supplier;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {

    // Ordinarily, carvers only use providers in the origin chunk.
    // This solution is otherwise brittle, but will not affect vanilla or PG features.
    @Inject(
        method = "applyCarvers",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/WorldGenRegion;getChunk(II)Lnet/minecraft/world/level/chunk/ChunkAccess;"))
    private void updateCarverPos(
            CallbackInfo ci,
            @Local(ordinal = 1) ChunkPos currentPos) {
        ScopeExtension.GENERATING_POS.get().set(currentPos);
    }

    @ModifyArg(
        method = "fillFromNoise",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;wrapThreadWithTaskName(Ljava/lang/String;Ljava/util/function/Supplier;)Ljava/util/function/Supplier;"),
        index = 1)
    private Supplier<ChunkAccess> wrapDoFill(
            Supplier<ChunkAccess> task,
            @Local(argsOnly = true) StructureManager structures,
            @Local(argsOnly = true) ChunkAccess chunk) {
        final var level = ((StructureManagerAccessor) structures).getLevel();
        final var seed = ((WorldGenRegion) level).getSeed();
        final var source = this.generatorSettings().value().getRandomSource().newInstance(seed);
        final var rand = new WorldgenRandom(source);
        rand.setLargeFeatureSeed(seed - 1L, chunk.getPos().x, chunk.getPos().z);
        return () -> ScopeExtension.DENSITY_RAND.getScoped(rand, task);
    }

    @Shadow
    public abstract Holder<NoiseGeneratorSettings> generatorSettings();
}
