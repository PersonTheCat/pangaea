package personthecat.pangaea.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.world.injector.Injector;
import personthecat.pangaea.world.injector.InjectorType;

public final class PgRegistries {
    public static final RegistryHandle<InjectorType<?>> INJECTOR_TYPE =
        RegistryHandle.createAndRegister(Pangaea.MOD, Keys.INJECTOR_TYPE);
    public static final RegistryHandle<Injector<?, ?>> INJECTOR =
        RegistryHandle.createDynamic(Pangaea.MOD, Keys.INJECTOR, Injector.DIRECT_CODEC);

    private PgRegistries() {}

    public static final class Keys {
        public static final ResourceKey<Registry<InjectorType<?>>> INJECTOR_TYPE =
            ResourceKey.createRegistryKey(Pangaea.MOD.id("injector_type"));
        public static final ResourceKey<Registry<Injector<?, ?>>> INJECTOR =
            ResourceKey.createRegistryKey(Pangaea.MOD.id("injector"));

        private Keys() {}
    }
}
