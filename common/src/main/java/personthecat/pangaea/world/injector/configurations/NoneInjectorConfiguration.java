package personthecat.pangaea.world.injector.configurations;

import com.mojang.serialization.MapCodec;

public class NoneInjectorConfiguration implements InjectorConfiguration {
    private static final NoneInjectorConfiguration INSTANCE = new NoneInjectorConfiguration();
    public static final MapCodec<NoneInjectorConfiguration> CODEC = MapCodec.unit(INSTANCE);

    private NoneInjectorConfiguration() {}
}
