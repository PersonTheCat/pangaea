package personthecat.pangaea.world.biome;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.biome.Climate.ParameterPoint;

public record CartesianParameterList(List<List<ParameterMap>> matrix) {
    public static final Codec<CartesianParameterList> CODEC =
        ParameterMap.CODEC.codec().listOf(1, Integer.MAX_VALUE).listOf(1, Integer.MAX_VALUE)
            .xmap(CartesianParameterList::new, CartesianParameterList::matrix);

    public <T> List<Pair<ParameterPoint, T>> createPairs(T t) {
        return Lists.cartesianProduct(this.matrix).stream()
            .map(list -> Pair.of(ParameterMap.flatten(list).toPoint(), t))
            .toList();
    }
}
