package personthecat.pangaea.mixin.extras;

import net.minecraft.server.level.WorldGenRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import personthecat.pangaea.extras.WorldGenRegionExtras;
import personthecat.pangaea.world.level.GenerationContext;

@Mixin(WorldGenRegion.class)
public class WorldGenRegionMixin implements WorldGenRegionExtras {

    @Unique
    private GenerationContext pangaea$generationContext;

    @Override
    public GenerationContext pangaea$getGenerationContext() {
        return this.pangaea$generationContext;
    }

    @Override
    public void pangaea$setGenerationContext(GenerationContext ctx) {
        this.pangaea$generationContext = ctx;
    }
}
