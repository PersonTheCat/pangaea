package personthecat.pangaea.mixin.accessor;

import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(PlacedFeature.class)
public interface PlacedFeatureAccessor {
    @Mutable
    @Accessor
    void setPlacement(List<PlacementModifier> placement);
}
