package personthecat.pangaea.world.injector;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.ApiStatus;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.player.CommonPlayerEvent;
import personthecat.catlib.event.registry.DataRegistryEvent;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.world.injector.Injector.Phase;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Log4j2
public final class DataInjectionHook {
    private static final AtomicReference<InjectionContext> INJECTIONS = new AtomicReference<>();
    private static final AtomicBoolean IS_SETUP = new AtomicBoolean(false);

    @ApiStatus.Internal
    public static void setup() {
        if (IS_SETUP.getAndSet(true)) {
            log.warn("Tried to setup injection hook multiple times. Ignoring...");
            return;
        }
        DataRegistryEvent.POST.register(source -> runInjections(source.asRegistryAccess()));
        FeatureModificationEvent.forBiomes(DataInjectionHook::hasChanges).register(DataInjectionHook::applyChanges);
        CommonPlayerEvent.LOGIN.register((player, server) -> cleanup());
    }

    private static void runInjections(RegistryAccess registries) {
        final var phase = getCurrentPhase(registries);
        final var ctx = createContext(registries, phase);
        final var injectors = new HashSet<>(getInjectorsForPhase(phase));

        while (!injectors.isEmpty()) {
            final var failures = new HashMap<ResourceKey<Injector>, Throwable>();
            final var iterator = injectors.iterator();
            var anyUpdated = false;

            while (iterator.hasNext()) {
                final var entry = iterator.next();
                final var key = entry.getKey();
                final var injector = entry.getValue();
                if (!injector.hasUnmetDependencies()) {
                    try {
                        injector.inject(key, ctx);
                    } catch (final Throwable t) {
                        failures.put(key, t);
                    }
                    iterator.remove();
                    anyUpdated = true;
                }
            }
            if (!failures.isEmpty()) {
                LibErrorContext.fatal(Pangaea.MOD, new InjectionFailureException(failures));
                return;
            }
            if (!anyUpdated) {
                LibErrorContext.fatal(Pangaea.MOD, new UnmetDependenciesException(mapDependencies(injectors)));
                return;
            }
        }
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

    private static Collection<Entry<ResourceKey<Injector>, Injector>> getInjectorsForPhase(Phase phase) {
        return PgRegistries.INJECTOR.entrySet().stream()
            .filter(e -> e.getValue().phase() == phase)
            .sorted(Comparator.comparingInt((Entry<?, Injector> e) -> e.getValue().priority())
                .thenComparing((Entry<?, Injector> e) -> PgRegistries.INJECTOR_TYPE.getKey(e.getValue().codec()))
                .thenComparingInt((Entry<?, Injector> e) -> e.getValue().getUnmetDependencyCount()))
            .toList();
    }

    private static Map<ResourceLocation, List<ResourceLocation>> mapDependencies(
            Collection<Entry<ResourceKey<Injector>, Injector>> injectors) {
        return injectors.stream().collect(Collectors.toMap(
            entry -> entry.getKey().location(),
            entry -> entry.getValue().getUnmetDependencyNames()));
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
        INJECTIONS.set(null);
    }
}
