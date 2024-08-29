package personthecat.pangaea.world.injector;

import com.mojang.serialization.MapCodec;
import personthecat.pangaea.world.injector.configurations.InjectorConfiguration;

public abstract class Injector<C extends InjectorConfiguration> {
    private final MapCodec<ConfiguredInjector<C, Injector<C>>> configuredCodec;

    protected Injector(MapCodec<C> codec) {
        this.configuredCodec = codec.xmap(c -> new ConfiguredInjector<>(this, c), ConfiguredInjector::config);
    }

    public abstract void inject(InjectionContext ctx, C config);

    public MapCodec<ConfiguredInjector<C, Injector<C>>> configuredCodec() {
        return this.configuredCodec;
    }
}
