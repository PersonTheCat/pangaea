package personthecat.pangaea.world.injector;

import com.mojang.serialization.Codec;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.PgCodecs;
import personthecat.pangaea.world.injector.configurations.InjectorConfiguration;

public record Injector<C extends InjectorConfiguration, I extends InjectorType<C>>(I injector, C config) {
    public static final Codec<Injector<?, ?>> DIRECT_CODEC =
        PgCodecs.inferredDispatch(PgRegistries.INJECTOR_TYPE, PgRegistries.Keys.INJECTOR)
            .apply(ci -> ci.injector, InjectorType::injectorCodec);

    public void inject(InjectionContext ctx) {
        this.injector.inject(ctx, this.config);
    }
}
