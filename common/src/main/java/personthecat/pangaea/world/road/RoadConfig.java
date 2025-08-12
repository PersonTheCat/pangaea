package personthecat.pangaea.world.road;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import personthecat.catlib.serialization.codec.CodecUtils;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.PangaeaCodec;

public interface RoadConfig {
    Codec<RoadConfig> CODEC =
        PangaeaCodec.forRegistry(PgRegistries.ROAD_TYPE, RoadConfig::codec);

    RoadGenerator<? extends RoadConfig> createGenerator(ServerLevel level, RoadMap map);
    MapCodec<? extends RoadConfig> codec();

    default DestinationStrategy destinationStrategy() {
        return DestinationStrategy.DEFAULT;
    }

    enum DestinationStrategy {
        DISTANCE_AWAY,
        PATH_BETWEEN;

        public static final DestinationStrategy DEFAULT = DISTANCE_AWAY;
        public static final Codec<DestinationStrategy> CODEC =
            CodecUtils.ofEnum(DestinationStrategy.class);
    }
}
