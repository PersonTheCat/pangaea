package personthecat.pangaea.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.world.injector.ConfiguredInjector;
import personthecat.pangaea.world.injector.Injector;

public final class PgRegistries {
    public static final RegistryHandle<Injector<?>> INJECTOR =
        RegistryHandle.createAndRegister(Pangaea.MOD, Keys.INJECTOR);
    public static final RegistryHandle<ConfiguredInjector<?, ?>> CONFIGURED_INJECTOR =
        RegistryHandle.createDynamic(Pangaea.MOD, Keys.CONFIGURED_INJECTOR, ConfiguredInjector.DIRECT_CODEC);

    private PgRegistries() {}

    public static final class Keys {
        public static final ResourceKey<Registry<Injector<?>>> INJECTOR =
            ResourceKey.createRegistryKey(Pangaea.MOD.id("injector"));
        public static final ResourceKey<Registry<ConfiguredInjector<?, ?>>> CONFIGURED_INJECTOR =
            ResourceKey.createRegistryKey(Pangaea.MOD.id("configured_injector"));

        private Keys() {}
    }
}
