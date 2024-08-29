package personthecat.pangaea.world.injector;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.PgCodecs;

import java.util.function.Function;

public interface Injector {
    Codec<Injector> CODEC =
        PgCodecs.inferredDispatch(PgRegistries.INJECTOR_TYPE, PgRegistries.Keys.INJECTOR)
            .apply(Injector::codec, Function.identity());

    void inject(InjectionContext ctx);
    MapCodec<? extends Injector> codec();
}
