package personthecat.pangaea.world.injector;

import com.mojang.serialization.MapCodec;
import personthecat.pangaea.world.injector.configurations.InjectorConfiguration;

public abstract class InjectorType<C extends InjectorConfiguration> {
    private final MapCodec<Injector<C, InjectorType<C>>> injectorCodec;

    protected InjectorType(MapCodec<C> codec) {
        this.injectorCodec = codec.xmap(c -> new Injector<>(this, c), Injector::config);
    }

    public abstract void inject(InjectionContext ctx, C config);

    public MapCodec<Injector<C, InjectorType<C>>> injectorCodec() {
        return this.injectorCodec;
    }
}
