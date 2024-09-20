package personthecat.pangaea.world.injector;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.InferredTypeDispatcher;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface Injector {
    Codec<Injector> CODEC =
        InferredTypeDispatcher.builder(PgRegistries.INJECTOR_TYPE, PgRegistries.Keys.INJECTOR)
            .build(Injector::codec, Function.identity());

    void inject(ResourceKey<Injector> key, InjectionContext ctx);
    MapCodec<? extends Injector> codec();

    default Phase phase() {
        return Phase.WORLD_GEN;
    }

    default int priority() {
        return 1000;
    }

    default Stream<Holder<?>> getDependencies() {
        return Stream.empty();
    }

    default Stream<Holder<?>> getUnmetDependencies() {
        return this.getDependencies().filter(Predicate.not(Holder::isBound));
    }

    default int getUnmetDependencyCount() {
        return this.hasUnmetDependencies() ? 0 : (int) this.getUnmetDependencies().count();
    }

    default boolean hasUnmetDependencies() {
        return this.getUnmetDependencies().findAny().isPresent();
    }

    default List<ResourceLocation> getUnmetDependencyNames() {
        return this.getUnmetDependencies().map(holder -> holder.unwrapKey().orElseThrow().location()).toList();
    }

    enum Phase {
        WORLD_GEN,
        DIMENSION,
    }
}
