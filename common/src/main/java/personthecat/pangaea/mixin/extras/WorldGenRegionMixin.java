package personthecat.pangaea.mixin.extras;

import net.minecraft.server.level.WorldGenRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import personthecat.pangaea.extras.WorldGenRegionExtras;
import personthecat.pangaea.world.level.PangaeaContext;

@Mixin(WorldGenRegion.class)
public class WorldGenRegionMixin implements WorldGenRegionExtras {

    @Unique
    private PangaeaContext pangaea$pangaeaContext;

    @Override
    public PangaeaContext pangaea$getPangaeaContext() {
        return this.pangaea$pangaeaContext;
    }

    @Override
    public void pangaea$setPangaeaContext(PangaeaContext ctx) {
        this.pangaea$pangaeaContext = ctx;
    }
}
