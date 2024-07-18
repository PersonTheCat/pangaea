package personthecat.pangaea.world.level;

import net.minecraft.world.level.Level;
import personthecat.pangaea.data.NoiseGraph;
import personthecat.pangaea.world.road.RoadMap;

import java.nio.file.Path;

public interface LevelExtras {
    RoadMap pangaea$getRoadMap();
    NoiseGraph pangaea$getNoiseGraph();
    Path pangaea$getDimensionPath();

    static RoadMap getRoadMap(Level level) {
        return get(level).pangaea$getRoadMap();
    }

    static NoiseGraph getNoiseGraph(Level level) {
        return get(level).pangaea$getNoiseGraph();
    }

    static Path getDimensionPath(Level level) {
        return get(level).pangaea$getDimensionPath();
    }

    static LevelExtras get(Level level) {
        if (level instanceof LevelExtras extras) {
            return extras;
        }
        if (level.isClientSide()) {
            throw new IllegalArgumentException("Tried to get extras from client level");
        }
        throw new IllegalStateException("Level extras mixin was not applied");
    }
}
