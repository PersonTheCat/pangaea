package personthecat.pangaea.extras;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

public interface CarverExtras<C extends CarverConfiguration> {
    Codec<C> pangaea$getCodec();

    static <C extends CarverConfiguration> Codec<C> getCodec(WorldCarver<C> carver) {
        return get(carver).pangaea$getCodec();
    }

    @SuppressWarnings("unchecked")
    static <C extends CarverConfiguration> CarverExtras<C> get(WorldCarver<C> carver) {
        if (carver instanceof CarverExtras<?> extras) {
            return (CarverExtras<C>) extras;
        }
        throw new IllegalStateException("WorldCarver extras mixin was not applied");
    }
}
