package personthecat.pangaea.world.injector;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.ApiStatus;
import personthecat.catlib.event.registry.DataRegistryEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.pangaea.registry.PgRegistries;

import java.util.Objects;
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
        DataRegistryEvent.POST.register(source -> runInjections(source.asRegistryAccess()));
        FeatureModificationEvent.forBiomes(DataInjectionHook::hasChanges).register(DataInjectionHook::applyChanges);
        CommonWorldEvent.LOAD.register(level -> cleanup()); // not ideal due to multiple recurrence
    }

    private static void runInjections(RegistryAccess registries) {
        final var phase = getCurrentPhase(registries);
        final var ctx = createContext(registries, phase);

        PgRegistries.INJECTOR.forEach(injector -> {
            if (injector.phase() == phase) {
                injector.inject(ctx);
            }
        });
    }

    private static Injector.Phase getCurrentPhase(RegistryAccess registries) {
        if (registries.registry(Registries.LEVEL_STEM).isPresent()) {
            return Injector.Phase.DIMENSION;
        }
        return Injector.Phase.WORLD_GEN;
    }

    private static InjectionContext createContext(RegistryAccess registries, Injector.Phase phase) {
        final InjectionContext ctx;
        if (phase == Injector.Phase.WORLD_GEN) {
            ctx = new InjectionContext(registries);
        } else {
            ctx = Objects.requireNonNull(INJECTIONS.get(), "out of sync").withRegistries(registries);
        }
        INJECTIONS.set(ctx);
        return ctx;
    }

    private static boolean hasChanges(Holder<Biome> holder) {
        final InjectionContext injections = INJECTIONS.get();
        return injections != null && injections.hasChanges(holder);
    }

    private static void applyChanges(FeatureModificationContext ctx) {
        final InjectionContext injections = INJECTIONS.get();
        if (injections != null) injections.applyChanges(ctx);
    }

    private static void cleanup() {
        INJECTIONS.remove();
    }
}
