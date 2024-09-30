package personthecat.pangaea.world.biome;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate.ParameterPoint;

import java.util.List;

@FunctionalInterface
public interface ParameterListModifierListener {
    void modify(List<Pair<ParameterPoint, Holder<Biome>>> list);
}
