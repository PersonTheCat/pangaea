package personthecat.pangaea.world.hooks;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import personthecat.pangaea.extras.LevelExtras;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.world.feature.GiantFeatureStage;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.road.RoadRegion;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class GeneratorHooks {

    private GeneratorHooks() {}

    public static CompletableFuture<Void> initRoadSystem(
            WorldGenLevel level, ChunkAccess chunk, ChunkGenerator gen, Executor ex) {
        if (PgRegistries.ROAD.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        final var map = LevelExtras.getRoadMap(level.getLevel());
        final short x = RoadRegion.chunkToRegion(chunk.getPos().x);
        final short z = RoadRegion.chunkToRegion(chunk.getPos().z);

        // Avoid the overhead from async generation if region already exists
        if (map.hasRegion(x, z)) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_roads", () -> {
            PangaeaContext.init(level, (ProtoChunk) chunk, gen);
            map.loadOrGenerateRegion(x, z);
            return null;
        }), ex);
    }

    public static void applyGiantFeatures(
            WorldGenLevel level, GiantFeatureStage stage, ChunkGenerator gen, PangaeaContext ctx) {
        final var registry = PgRegistries.GIANT_FEATURE;
        if (registry.isEmpty()) {
            return;
        }
        final var centerPos = new BlockPos(ctx.centerX, 0, ctx.centerZ);
        final var rand = RandomSource.create(ctx.seed);
        final var biome = ctx.noise.getApproximateBiome(ctx.biomes, ctx.centerX, ctx.centerZ);
        for (final var feature : registry) {
            if (feature.config().stage != stage) {
                continue;
            }
            if (!feature.config().strictOrigin) {
                final var predicate = feature.config().conditions.buildPredicate();
                if (!predicate.test(biome, ctx.centerX, ctx.centerZ)) {
                    continue;
                }
            }
            ctx.featureIndex.increment();
            feature.feature().place(feature.config(), level, gen, rand, centerPos);
        }
    }
}
