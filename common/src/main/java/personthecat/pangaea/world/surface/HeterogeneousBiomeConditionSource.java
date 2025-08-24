package personthecat.pangaea.world.surface;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules.Condition;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.BiomePredicate;

public record HeterogeneousBiomeConditionSource(BiomePredicate biomes) implements ConditionSource {
    public static final MapCodec<HeterogeneousBiomeConditionSource> CODEC =
        BiomePredicate.CODEC.fieldOf("biomes").xmap(HeterogeneousBiomeConditionSource::new, HeterogeneousBiomeConditionSource::biomes);

    @Override
    public Condition apply(Context ctx) {
        return new HeterogeneousBiomeCondition(ctx, this.biomes);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends ConditionSource> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    record HeterogeneousBiomeCondition(Context ctx, BiomePredicate biomes) implements Condition {
        @Override
        public boolean test() {
            return this.biomes.test(this.ctx.biome.get());
        }
    }
}
