package personthecat.pangaea.mixin.extras;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.data.NoiseGraph;
import personthecat.pangaea.mixin.accessor.MinecraftServerAccessor;
import personthecat.pangaea.extras.LevelExtras;
import personthecat.pangaea.world.road.RoadMap;

import java.nio.file.Path;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements LevelExtras {
    @Shadow
    @Final
    private ServerChunkCache chunkSource;
    @Unique
    private RoadMap pangaea$roadMap;
    @Unique
    private NoiseGraph pangaea$noiseGraph;

    @Inject(at = @At("RETURN"), method = "<init>")
    public void postInit(CallbackInfo ci) {
        this.pangaea$noiseGraph = new NoiseGraph(this.chunkSource.randomState()); // accessed by roadMap
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

    @Override
    public Path pangaea$getDimensionPath() {
        final var level = (ServerLevel) (Object) this;
        return ((MinecraftServerAccessor) level.getServer()).getStorageSource().getDimensionPath(level.dimension());
    }
}
