package personthecat.pangaea.world.hooks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.world.level.PangaeaContext;

public final class GeneratorHooks {

    private GeneratorHooks() {}

    public static void applyGiantFeatures(WorldGenLevel level, ChunkGenerator gen, PangaeaContext ctx) {
        final var registry = PgRegistries.GIANT_FEATURE;
        if (registry.isEmpty()) {
            return;
        }
        final var centerPos = new BlockPos(ctx.centerX, 0, ctx.centerZ);
        final var rand = RandomSource.create(ctx.seed);
        final var biome = ctx.noise.getApproximateBiome(ctx.biomes, ctx.centerX, ctx.centerZ);
        for (final var feature : registry) {
            final var predicate = feature.config().conditions.buildPredicate();
            if (predicate.test(biome, ctx.centerX, ctx.centerZ)) {
                feature.feature().place(feature.config(), level, gen, rand, centerPos);
            }
        }
    }
}
