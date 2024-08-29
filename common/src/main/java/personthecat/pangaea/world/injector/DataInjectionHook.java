package personthecat.pangaea.world.injector;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.ApiStatus;
import personthecat.catlib.event.registry.DataRegistryEvent;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.pangaea.registry.PgRegistries;

import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public final class DataInjectionHook {
    private static final ThreadLocal<InjectionContext> INJECTIONS = new ThreadLocal<>();
    private static final AtomicBoolean IS_SETUP = new AtomicBoolean(false);

    @ApiStatus.Internal
    public static void setup() {
        if (IS_SETUP.getAndSet(true)) {
            log.warn("Tried to setup injection hook multiple times. Ignoring...");
            return;
        }
        DataRegistryEvent.POST.register(source -> {
            if (source.getRegistry(Registries.LEVEL_STEM) == null) {
                return; // only run once all registries have loaded
            }
            final var registry = source.getRegistry(PgRegistries.Keys.INJECTOR);
            if (registry != null) {
                final var ctx = new InjectionContext(source.asRegistryAccess());
                INJECTIONS.set(ctx);
                registry.forEach(injector -> injector.inject(ctx));
            }
        });
        FeatureModificationEvent.forBiomes(DataInjectionHook::hasRemovalsForBiome)
            .register(DataInjectionHook::applyRemovals);
    }

    private static boolean hasRemovalsForBiome(Holder<Biome> holder) {
        final InjectionContext injections = INJECTIONS.get();
        return injections != null && injections.hasChanges(holder);
    }

    private static void applyRemovals(FeatureModificationContext ctx) {
        final InjectionContext injections = INJECTIONS.get();
        if (injections != null) {
            injections.applyChanges(ctx);
            INJECTIONS.remove();
        }
    }
}
