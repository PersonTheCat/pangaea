package personthecat.pangaea.mixin.extras;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStatus.GenerationTask;
import net.minecraft.world.level.chunk.status.ChunkStatus.LoadingTask;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.status.ToFullChunk;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.world.feature.GiantFeatureStage;
import personthecat.pangaea.world.hooks.GeneratorHooks;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusMixin {

    @Shadow @Final private static EnumSet<Types> PRE_FEATURES;
    @Shadow @Final public static ChunkStatus EMPTY;
    @Shadow @Final public static ChunkStatus NOISE;
    @Shadow @Final public static ChunkStatus CARVERS;
    @Unique private static ChunkStatus INIT_ROADS;
    @Unique private static ChunkStatus BEFORE_SURFACE;
    @Unique private static ChunkStatus AFTER_SURFACE;

    @Inject(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/status/ChunkStatus;register(Ljava/lang/String;Lnet/minecraft/world/level/chunk/status/ChunkStatus;IZLjava/util/EnumSet;Lnet/minecraft/world/level/chunk/status/ChunkType;Lnet/minecraft/world/level/chunk/status/ChunkStatus$GenerationTask;Lnet/minecraft/world/level/chunk/status/ChunkStatus$LoadingTask;)Lnet/minecraft/world/level/chunk/status/ChunkStatus;", ordinal = 1))
    private static void addRoads(CallbackInfo ci) {
        INIT_ROADS = register("pangaea:init_roads", EMPTY, 0, false, PRE_FEATURES, ChunkType.PROTOCHUNK, ChunkStatusMixin::pangaea$roadsTask, ChunkStatusMixin::pangaea$passThrough);
    }

    @Inject(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/status/ChunkStatus;register(Ljava/lang/String;Lnet/minecraft/world/level/chunk/status/ChunkStatus;IZLjava/util/EnumSet;Lnet/minecraft/world/level/chunk/status/ChunkType;Lnet/minecraft/world/level/chunk/status/ChunkStatus$GenerationTask;Lnet/minecraft/world/level/chunk/status/ChunkStatus$LoadingTask;)Lnet/minecraft/world/level/chunk/status/ChunkStatus;", ordinal = 5))
    private static void addBeforeSurface(CallbackInfo ci) {
        BEFORE_SURFACE = register("pangaea:before_surface", NOISE, 1, true, PRE_FEATURES, ChunkType.PROTOCHUNK, ChunkStatusMixin::pangaea$beforeSurfaceTask, ChunkStatusMixin::pangaea$passThrough);
    }

    @Inject(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/status/ChunkStatus;register(Ljava/lang/String;Lnet/minecraft/world/level/chunk/status/ChunkStatus;IZLjava/util/EnumSet;Lnet/minecraft/world/level/chunk/status/ChunkType;Lnet/minecraft/world/level/chunk/status/ChunkStatus$GenerationTask;Lnet/minecraft/world/level/chunk/status/ChunkStatus$LoadingTask;)Lnet/minecraft/world/level/chunk/status/ChunkStatus;", ordinal = 7))
    private static void addAfterSurface(CallbackInfo ci) { // technically after carvers
        AFTER_SURFACE = register("pangaea:after_surface", CARVERS, 1, true, PRE_FEATURES, ChunkType.PROTOCHUNK, ChunkStatusMixin::pangaea$afterSurfaceTask, ChunkStatusMixin::pangaea$passThrough);
    }

    @WrapOperation(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/status/ChunkStatus;register(Ljava/lang/String;Lnet/minecraft/world/level/chunk/status/ChunkStatus;IZLjava/util/EnumSet;Lnet/minecraft/world/level/chunk/status/ChunkType;Lnet/minecraft/world/level/chunk/status/ChunkStatus$GenerationTask;Lnet/minecraft/world/level/chunk/status/ChunkStatus$LoadingTask;)Lnet/minecraft/world/level/chunk/status/ChunkStatus;", ordinal = 1))
    private static ChunkStatus updateStructureStarts(String id, ChunkStatus parent, int range, boolean dependencies, EnumSet<Types> types, ChunkType type, GenerationTask gen, LoadingTask load, Operation<ChunkStatus> registerSurface) {
        return registerSurface.call(id, INIT_ROADS, range, dependencies, types, type, gen, load);
    }

    @WrapOperation(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/status/ChunkStatus;register(Ljava/lang/String;Lnet/minecraft/world/level/chunk/status/ChunkStatus;IZLjava/util/EnumSet;Lnet/minecraft/world/level/chunk/status/ChunkType;Lnet/minecraft/world/level/chunk/status/ChunkStatus$GenerationTask;Lnet/minecraft/world/level/chunk/status/ChunkStatus$LoadingTask;)Lnet/minecraft/world/level/chunk/status/ChunkStatus;", ordinal = 5))
    private static ChunkStatus updateSurface(String id, ChunkStatus parent, int range, boolean dependencies, EnumSet<Types> types, ChunkType type, GenerationTask gen, LoadingTask load, Operation<ChunkStatus> registerSurface) {
        return registerSurface.call(id, BEFORE_SURFACE, range, dependencies, types, type, gen, load);
    }

    @WrapOperation(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/status/ChunkStatus;register(Ljava/lang/String;Lnet/minecraft/world/level/chunk/status/ChunkStatus;IZLjava/util/EnumSet;Lnet/minecraft/world/level/chunk/status/ChunkType;Lnet/minecraft/world/level/chunk/status/ChunkStatus$GenerationTask;Lnet/minecraft/world/level/chunk/status/ChunkStatus$LoadingTask;)Lnet/minecraft/world/level/chunk/status/ChunkStatus;", ordinal = 7))
    private static ChunkStatus updateFeatures(String id, ChunkStatus parent, int range, boolean dependencies, EnumSet<Types> types, ChunkType type, GenerationTask gen, LoadingTask load, Operation<ChunkStatus> registerSurface) {
        return registerSurface.call(id, AFTER_SURFACE, range, dependencies, types, type, gen, load);
    }

    @Shadow
    private static ChunkStatus register(
            String id, @Nullable ChunkStatus previous, int range, boolean dependencies, EnumSet<Types> types, ChunkType type, GenerationTask gen, LoadingTask load) {
        throw new AssertionError("Oops! this wasn't shadowed correctly");
    }

    @Unique
    private static CompletableFuture<ChunkAccess> pangaea$roadsTask(WorldGenContext wgc, ChunkStatus status, Executor ex, ToFullChunk toFullChunk, List<ChunkAccess> chunks, ChunkAccess chunk) {
        final var level = new WorldGenRegion(wgc.level(), chunks, status, 0);
        return GeneratorHooks.initRoadSystem(level, chunk, wgc.generator(), ex).thenApply(v -> chunk);
    }

    @Unique
    private static CompletableFuture<ChunkAccess> pangaea$beforeSurfaceTask(WorldGenContext wgc, ChunkStatus status, Executor ex, ToFullChunk toFullChunk, List<ChunkAccess> chunks, ChunkAccess chunk) {
        final var level = new WorldGenRegion(wgc.level(), chunks, status, 0);
        return CompletableFuture.supplyAsync(() -> {
            final var ctx = PangaeaContext.init(level, (ProtoChunk) chunk, wgc.generator());
            GeneratorHooks.applyGiantFeatures(level, GiantFeatureStage.BEFORE_SURFACE, wgc.generator(), ctx);
            return chunk;
        }, ex);
    }

    @Unique
    private static CompletableFuture<ChunkAccess> pangaea$afterSurfaceTask(WorldGenContext wgc, ChunkStatus status, Executor ex, ToFullChunk toFullChunk, List<ChunkAccess> chunks, ChunkAccess chunk) {
        final var level = new WorldGenRegion(wgc.level(), chunks, status, 0);
        final var ctx = PangaeaContext.init(level, (ProtoChunk) chunk, wgc.generator());
        GeneratorHooks.applyGiantFeatures(level, GiantFeatureStage.AFTER_SURFACE, wgc.generator(), ctx);
        return CompletableFuture.completedFuture(chunk);
    }

    @Unique
    private static CompletableFuture<ChunkAccess> pangaea$passThrough(WorldGenContext wgc, ChunkStatus status, ToFullChunk toFullChunk, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }
}
