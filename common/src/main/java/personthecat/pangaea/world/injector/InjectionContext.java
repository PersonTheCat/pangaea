package personthecat.pangaea.world.injector;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.pangaea.Pangaea;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class InjectionContext {
    private final List<Modification> removals;
    private final List<Modification> additions;
    private final RegistryAccess registries;
    private final Map<ResourceKey<Injector>, Runnable> cleanup;

    public InjectionContext(RegistryAccess registries) {
        this(registries, new ArrayList<>(), new ArrayList<>(), new HashMap<>());
    }

    private InjectionContext(
            RegistryAccess registries,
            List<Modification> removals,
            List<Modification> additions,
            Map<ResourceKey<Injector>, Runnable> cleanup) {
        this.registries = registries;
        this.removals = removals;
        this.additions = additions;
        this.cleanup = cleanup;
    }

    public RegistryAccess registries() {
        return this.registries;
    }

    public void addRemovals(BiomePredicate biomes, Consumer<FeatureModificationContext> listener) {
        this.removals.add(new Modification(biomes, listener));
    }

    public void addAdditions(BiomePredicate biomes, Consumer<FeatureModificationContext> listener) {
        this.additions.add(new Modification(biomes, listener));
    }

    public void addCleanupTask(ResourceKey<Injector> injector, Runnable task) {
        this.cleanup.put(injector, task);
    }

    public InjectionContext withRegistries(RegistryAccess registries) {
        return new InjectionContext(registries, this.removals, this.additions, this.cleanup);
    }

    public void cleanup() {
        this.cleanup.forEach((k, r) -> {
            try {
                r.run();
            } catch (final Exception e) {
                LibErrorContext.error(Pangaea.MOD, CleanupFailureException.forInjector(k, e));
            }
        });
    }

    public boolean hasChanges(Holder<Biome> biome) {
        return Stream.concat(this.removals.stream(), this.additions.stream())
            .map(Modification::biomes)
            .anyMatch(biomes -> biomes.test(biome));
    }

    public void applyChanges(FeatureModificationContext ctx) {
        this.removals.forEach(m -> m.apply(ctx));
        this.additions.forEach(m -> m.apply(ctx));
    }

    public record Modification(BiomePredicate biomes, Consumer<FeatureModificationContext> listener) {
        void apply(FeatureModificationContext ctx) {
            if (this.biomes.test(ctx.getBiome())) {
                this.listener.accept(ctx);
            }
        }
    }
}
