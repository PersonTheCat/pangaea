package personthecat.pangaea.mixin;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.data.NoiseGraph;
import personthecat.pangaea.world.level.LevelExtras;
import personthecat.pangaea.world.road.RoadMap;

@Mixin(ServerLevel.class)
public class ServerLevelMixin implements LevelExtras {
    @Unique
    private RoadMap pangaea$roadMap;
    @Unique
    private NoiseGraph pangaea$noiseGraph;

    @Inject(at = @At("RETURN"), method = "<init>")
    public void postInit(CallbackInfo ci) {
        this.pangaea$noiseGraph = new NoiseGraph(); // accessed by roadMap
        this.pangaea$roadMap = new RoadMap((ServerLevel) (Object) this);
    }

    @Override
    public RoadMap pangaea$getRoadMap() {
        return this.pangaea$roadMap;
    }

    @Override
    public NoiseGraph pangaea$getNoiseGraph() {
        return this.pangaea$noiseGraph;
    }
}
