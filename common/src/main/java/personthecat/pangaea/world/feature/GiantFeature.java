package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import personthecat.catlib.registry.CommonRegistries;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.FeatureCodecs;
import personthecat.pangaea.world.level.PangaeaContext;

public abstract class GiantFeature<FC extends GiantFeatureConfiguration> extends PangaeaFeature<FC> {
    public static final Codec<ConfiguredFeature<GiantFeatureConfiguration, ?>> DIRECT_CODEC =
        FeatureCodecs.FLAT_CONFIG.codec().flatXmap(GiantFeature::asGiantFeature, GiantFeature::asGiantFeature);
    public static final Codec<Holder<ConfiguredFeature<GiantFeatureConfiguration, ?>>> CODEC =
        RegistryFileCodec.create(PgRegistries.Keys.GIANT_FEATURE, DIRECT_CODEC);

    protected GiantFeature(Codec<FC> codec) {
        super(codec);
    }

    @Override
    protected final boolean place(PangaeaContext ctx, FC cfg, BlockPos pos, Border border) {
        final int cX = ctx.chunkX;
        final int cZ = ctx.chunkZ;
        final int r = cfg.chunkRadius;
        final var strict = cfg.strictOrigin;
        final var predicate = strict ? cfg.conditions.buildPredicate() : PositionalBiomePredicate.ALWAYS;

        for (int x = cX - r; x < cX + r; x++) {
            for (int z = cZ - r; z < cZ +r; z++) {
                final var pos2 = new ChunkPos(x, z);
                final int aX = (x << 4) + 8;
                final int aZ = (z << 4) + 8;
                ctx.rand.setLargeFeatureSeed(ctx.seed + ctx.featureIndex.get(), pos2.x, pos2.z);
                ctx.targetPos.at(aX, aZ);
                if (strict) {
                    final var biome = ctx.noise.getApproximateBiome(ctx.biomes, aX, aZ);
                    if (!predicate.test(biome, aX, aZ)) {
                        continue;
                    }
                }
                this.place(ctx, cfg, pos2, border);
            }
        }
        return true;
    }

    protected abstract void place(PangaeaContext ctx, FC cfg, ChunkPos pos, Border border);

    @SuppressWarnings("unchecked")
    private static DataResult<ConfiguredFeature<GiantFeatureConfiguration, ?>> asGiantFeature(ConfiguredFeature<?, ?> cf) {
        return cf.feature() instanceof GiantFeature
            ? DataResult.success((ConfiguredFeature<GiantFeatureConfiguration, ?>) cf)
            : DataResult.error(() -> "Not a giant feature: " + CommonRegistries.FEATURE.getKey(cf.feature()));
    }
}
