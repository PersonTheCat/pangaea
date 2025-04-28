package personthecat.pangaea.world.feature;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

@FunctionalInterface
public interface PositionalBiomePredicate {
    PositionalBiomePredicate ALWAYS = (b, x, z) -> true;

    boolean test(Holder<Biome> b, int x, int z);

    default boolean isDisabled() {
        return this == ALWAYS;
    }
}
