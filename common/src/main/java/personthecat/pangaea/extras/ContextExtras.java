package personthecat.pangaea.extras;

import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import personthecat.pangaea.data.NoiseGraph;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.function.Supplier;

public interface ContextExtras {
    PangaeaContext pangaea$getPangaea();
    Supplier<Holder<Biome>> pangaea$surfaceBiome();

    default RandomSource pangaea$getRandomSource() {
        return this.pangaea$getPangaea().rand;
    }

    default WorldGenLevel pangaea$getLevel() {
        return this.pangaea$getPangaea().level;
    }

    default NoiseGraph pangaea$getNoiseGraph() {
        return this.pangaea$getPangaea().noise;
    }

    static RandomSource getRandomSource(Context ctx) {
        return get(ctx).pangaea$getRandomSource();
    }

    static Holder<Biome> getSurfaceBiome(Context ctx) {
        return get(ctx).pangaea$surfaceBiome().get();
    }

    static WorldGenLevel getLevel(Context ctx) {
        return get(ctx).pangaea$getLevel();
    }

    static NoiseGraph getNoiseGraph(Context ctx) {
        return get(ctx).pangaea$getNoiseGraph();
    }

    static PangaeaContext getPangaea(Context ctx) {
        return get(ctx).pangaea$getPangaea();
    }

    @SuppressWarnings("ConstantConditions")
    static ContextExtras get(Context ctx) {
        if ((Object) ctx instanceof ContextExtras extras) {
            return extras;
        }
        throw new IllegalStateException("Surface rules context extras mixin was not applied");
    }
}
