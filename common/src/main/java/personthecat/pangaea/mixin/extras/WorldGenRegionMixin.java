package personthecat.pangaea.mixin.extras;

import net.minecraft.server.level.WorldGenRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import personthecat.pangaea.world.level.WorldGenRegionExtras;
import personthecat.pangaea.world.map.GenerationContext;

@Mixin(WorldGenRegion.class)
public class WorldGenRegionMixin implements WorldGenRegionExtras {

    @Unique
    private GenerationContext pangaea$generationContext;

    @Unique
    private int pangaea$generatingFeatureIndex = -1;


    @Override
    public GenerationContext pangaea$getGenerationContext() {
        return this.pangaea$generationContext;
    }

    @Override
    public void pangaea$setGenerationContext(GenerationContext ctx) {
        this.pangaea$generationContext = ctx;
    }

    @Override
    public int pangaea$getGeneratingFeatureIndex() {
        return this.pangaea$generatingFeatureIndex;
    }

    @Override
    public void pangaea$setGeneratingFeatureIndex(int idx) {
        this.pangaea$generatingFeatureIndex = idx;
    }
}
