package personthecat.pangaea.world.injector;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.event.world.FeatureModificationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class InjectionContext {
    private final List<Modification> removals = new ArrayList<>();
    private final List<Modification> additions = new ArrayList<>();
    private final RegistryAccess registries;

    public InjectionContext(RegistryAccess registries) {
        this.registries = registries;
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

    public boolean hasChanges(Holder<Biome> biome) {
        return Stream.concat(this.removals.stream(), this.additions.stream())
            .map(Modification::biomes)
            .anyMatch(biomes -> biomes.test(biome));
    }

    public void applyChanges(FeatureModificationContext ctx) {
        this.removals.forEach(m -> m.listener.accept(ctx));
        this.additions.forEach(m -> m.listener.accept(ctx));
    }

    public record Modification(BiomePredicate biomes, Consumer<FeatureModificationContext> listener) {}
}
