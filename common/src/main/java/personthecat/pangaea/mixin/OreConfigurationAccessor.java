package personthecat.pangaea.mixin;

import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.TargetBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(OreConfiguration.class)
public interface OreConfigurationAccessor {

    @Mutable
    @Accessor
    void setTargetStates(List<TargetBlockState> targetStates);

    @Mutable
    @Accessor
    void setSize(int size);

    @Mutable
    @Accessor
    void setDiscardChanceOnAirExposure(float discardChanceOnAirExposure);
}
