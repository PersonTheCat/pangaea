package personthecat.pangaea.world.injector;

import com.mojang.serialization.Codec;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.world.injector.configurations.InjectorConfiguration;

public record ConfiguredInjector<C extends InjectorConfiguration, I extends Injector<C>>(I injector, C config) {
    public static final Codec<ConfiguredInjector<?, ?>> DIRECT_CODEC = PgRegistries.INJECTOR.codec()
        .dispatch(ci -> ci.injector, Injector::configuredCodec);

    public void inject(InjectionContext ctx) {
        this.injector.inject(ctx, this.config);
    }
}
