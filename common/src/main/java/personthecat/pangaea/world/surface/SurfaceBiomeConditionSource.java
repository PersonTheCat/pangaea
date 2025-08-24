package personthecat.pangaea.world.surface;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules.Condition;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.BiomePredicate;
import personthecat.pangaea.extras.ContextExtras;

public record SurfaceBiomeConditionSource(BiomePredicate biomes) implements ConditionSource {
    public static final MapCodec<SurfaceBiomeConditionSource> CODEC =
        BiomePredicate.CODEC.fieldOf("surface_biomes").xmap(SurfaceBiomeConditionSource::new, SurfaceBiomeConditionSource::biomes);

    @Override
    public Condition apply(Context ctx) {
        return new SurfaceBiomeCondition(ctx, this.biomes);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends ConditionSource> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    record SurfaceBiomeCondition(Context ctx, BiomePredicate biomes) implements Condition {
        @Override
        public boolean test() {
            return this.biomes.test(ContextExtras.getSurfaceBiome(this.ctx));
        }
    }
}
