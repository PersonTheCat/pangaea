package personthecat.pangaea.world.level;

import net.minecraft.world.level.Level;
import personthecat.pangaea.data.NoiseGraph;
import personthecat.pangaea.world.road.RoadMap;

public interface LevelExtras {
    RoadMap pangaea$getRoadMap();
    NoiseGraph pangaea$getNoiseGraph();

    static RoadMap getRoadMap(Level level) {
        return get(level).pangaea$getRoadMap();
    }

    static NoiseGraph getNoiseGraph(Level level) {
        return get(level).pangaea$getNoiseGraph();
    }

    static LevelExtras get(Level level) {
        if (level.isClientSide()) {
            throw new IllegalArgumentException("Tried to get extras from client level");
        }
        if (level instanceof LevelExtras extras) {
            return extras;
        }
        throw new IllegalStateException("Level extras mixin was not applied");
    }
}
