package personthecat.pangaea.world.injector;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.IdList;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.registry.CommonRegistries;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.serialization.codec.FieldDescriptor;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.mixin.accessor.PlacedFeatureAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.nullable;

public record PlacementModifierInjector(
        IdList<Feature<?>> features,
        IdList<ConfiguredFeature<?, ?>> configuredFeatures,
        IdList<PlacedFeature> placedFeatures,
        At at,
        @Nullable ResourceKey<PlacementModifierType<?>> target,
        List<PlacementModifier> inject,
        List<ResourceKey<PlacementModifierType<?>>> remove) implements Injector {
    
    private static final Set<ResourceKey<PlacementModifierType<?>>> WARNED_TARGETS = 
        Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static final MapCodec<PlacementModifierInjector> CODEC = codecOf(
        idListField(Registries.FEATURE, "features", PlacementModifierInjector::features),
        idListField(Registries.CONFIGURED_FEATURE, "configured_features", PlacementModifierInjector::configuredFeatures),
        idListField(Registries.PLACED_FEATURE, "placed_features", PlacementModifierInjector::placedFeatures),
        defaulted(ofEnum(At.class), "at", At.END, PlacementModifierInjector::at),
        nullable(ResourceKey.codec(Registries.PLACEMENT_MODIFIER_TYPE), "target", PlacementModifierInjector::target),
        defaulted(easyList(PlacementModifier.CODEC), "inject", List.of(), PlacementModifierInjector::inject),
        defaulted(ResourceKey.codec(Registries.PLACEMENT_MODIFIER_TYPE).listOf(), "remove", List.of(), PlacementModifierInjector::remove),
        PlacementModifierInjector::new
    ).validate(PlacementModifierInjector::validate);
    
    private DataResult<PlacementModifierInjector> validate() {
        if ((this.at == At.BEFORE || this.at == At.AFTER) && this.target == null) {
            return DataResult.error(() -> "Must specify a target to inject before / after");
        }
        return DataResult.success(this);
    }

    @Override
    public void inject(ResourceKey<Injector> key, InjectionContext ctx) {
        if (this.inject.isEmpty() && this.remove.isEmpty()) {
            return;
        }
        DynamicRegistries.PLACED_FEATURE.forEachHolder((k, placed) -> {
            if (this.matches(placed)) {
                this.updateFeature(placed.value());
            }
        });
    }

    private boolean matches(Holder<PlacedFeature> placed) {
        if (this.placedFeatures.test(placed)) {
            return true;
        }
        final var configured = placed.value().feature();
        if (this.configuredFeatures.test(configured)) {
            return true;
        }
        final var feature = CommonRegistries.FEATURE.getHolder(configured.value().feature());
        return this.features.test(feature);
    }
    
    private void updateFeature(PlacedFeature placed) {
        this.getModifiedIndex(placed.placement()).ifPresent(i -> {
            final var updated = new ArrayList<>(placed.placement());
            if (!this.inject.isEmpty()) {
                updated.addAll(i, this.inject);
            }
            this.remove.forEach(key -> {
                final var t = CommonRegistries.PLACEMENT_MODIFIER_TYPE.lookup(key);
                if (t != null) {
                    updated.removeIf(m -> m.type() == t);
                }
            });
            ((PlacedFeatureAccessor) (Object) placed).setPlacement(ImmutableList.copyOf(updated));
        });
    }

    private OptionalInt getModifiedIndex(List<PlacementModifier> modifiers) {
        return switch (this.at) {
            case BEGINNING -> OptionalInt.of(0);
            case END -> OptionalInt.of(modifiers.size());
            case BEFORE -> this.getTarget(modifiers);
            case AFTER -> {
                final var t = this.getTarget(modifiers);
                yield t.isPresent() ? OptionalInt.of(t.getAsInt() + 1) : OptionalInt.empty();
            }
        };
    }

    private OptionalInt getTarget(List<PlacementModifier> modifiers) {
        final var target = CommonRegistries.PLACEMENT_MODIFIER_TYPE.lookup(this.target);
        if (target == null) {
            Objects.requireNonNull(this.target, "Target not validated: null");
            if (WARNED_TARGETS.add(this.target)) {
                LibErrorContext.warn(Pangaea.MOD, InjectionWarningException.targetNotFound(this.target));
            }
            return OptionalInt.empty();
        }
        for (int i = 0; i < modifiers.size(); i++) {
            final var modifier = modifiers.get(i);
            if (modifier.type() == target) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }
    
    @Override
    public MapCodec<PlacementModifierInjector> codec() {
        return CODEC;
    }
    
    private static <T> FieldDescriptor<PlacementModifierInjector, IdList<T>, IdList<T>> idListField(
            ResourceKey<Registry<T>> key, String name, Function<PlacementModifierInjector, IdList<T>> getter) {
        return defaulted(IdList.codecOf(key), name, IdList.builder(key).build(), getter);
    }
    
    public enum At {
        BEGINNING,
        END,
        BEFORE,
        AFTER;
    }
}
