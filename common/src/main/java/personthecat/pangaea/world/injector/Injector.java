package personthecat.pangaea.world.injector;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.InferredTypeDispatcher;

import java.util.function.Function;

public interface Injector {
    Codec<Injector> CODEC =
        InferredTypeDispatcher.builder(PgRegistries.INJECTOR_TYPE, PgRegistries.Keys.INJECTOR)
            .build(Injector::codec, Function.identity());

    void inject(InjectionContext ctx);
    MapCodec<? extends Injector> codec();

    default Phase phase() {
        return Phase.WORLD_GEN;
    }

    enum Phase {
        WORLD_GEN,
        DIMENSION,
    }
}
