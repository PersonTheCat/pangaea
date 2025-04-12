package personthecat.pangaea.world.injector;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.BiomeBuilder;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MobSpawnSettings.MobSpawnCost;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.mixin.accessor.BiomeGenerationSettingsAccessor;
import personthecat.pangaea.mixin.accessor.MobSpawnSettingsAccessor;
import personthecat.pangaea.world.biome.BiomeChanges;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.nullable;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public record BiomeInjector(@Nullable Holder<Biome> parent, BiomeChanges changes) implements Injector {
    private static final RegistrationInfo SYNCHRONIZED_INFO = new RegistrationInfo(
        Optional.of(new KnownPack(Pangaea.ID, "synchronized", Pangaea.RAW_VERSION)), Lifecycle.experimental());
    private static final Biome DEFAULT_PARENT = createDummyBiome();
    private static final Codec<Holder<Biome>> PARENT_CODEC =
        RegistryFileCodec.create(Registries.BIOME, Biome.DIRECT_CODEC, false);
    public static final MapCodec<BiomeInjector> CODEC = codecOf(
        nullable(PARENT_CODEC, "parent", BiomeInjector::parent),
        union(BiomeChanges.CODEC, BiomeInjector::changes),
        BiomeInjector::new
    );

    @Override
    public void inject(ResourceKey<Injector> key, InjectionContext ctx) {
        final var biomes = (WritableRegistry<Biome>) ctx.registries().registryOrThrow(Registries.BIOME);
        final var reference = this.parent != null ? this.parent.value() : DEFAULT_PARENT;
        final var biome = this.createBuilder(reference)
            .specialEffects(this.createEffects(reference))
            .generationSettings(this.createGeneration(reference))
            .mobSpawnSettings(this.createMobs(reference))
            .build();
        final var biomeKey = ResourceKey.create(Registries.BIOME, removePrefix(key.location()));
        biomes.register(biomeKey, biome, SYNCHRONIZED_INFO);
    }

    private BiomeBuilder createBuilder(Biome reference) {
        final var changes = this.changes.climate().changes();
        return new BiomeBuilder()
            .hasPrecipitation(getOrElse(changes.hasPrecipitation(), reference.hasPrecipitation()))
            .temperature(getOrElse(changes.temperature(), reference.getBaseTemperature()))
            .temperatureAdjustment(getOrElse(changes.temperatureModifier(), reference.climateSettings.temperatureModifier()))
            .downfall(getOrElse(changes.downfall(), reference.climateSettings.downfall()));
    }

    private BiomeSpecialEffects createEffects(Biome reference) {
        final var builder = new BiomeSpecialEffects.Builder();
        final var changes = this.changes.effects().changes();
        final var removals = this.changes.effects().removals();
        final var effects = reference.getSpecialEffects();

        builder.fogColor(getColor(changes.fogColor(), effects.getFogColor()));
        builder.waterColor(getColor(changes.waterColor(), effects.getWaterColor()));
        builder.waterFogColor(getColor(changes.waterFogColor(), effects.getWaterFogColor()));
        builder.skyColor(getColor(changes.skyColor(), effects.getSkyColor()));

        getOptionalColor(changes.foliageColor(), effects.getFoliageColorOverride(), removals.removeFoliageColor())
            .ifPresent(builder::foliageColorOverride);
        getOptionalColor(changes.grassColor(), effects.getGrassColorOverride(), removals.removeGrassColor())
            .ifPresent(builder::grassColorOverride);

        builder.grassColorModifier(getOrElse(changes.grassColorModifier(), effects.getGrassColorModifier()));

        getOptional(changes.ambientParticleSettings(), effects.getAmbientParticleSettings(), removals.removeAmbientParticleSettings())
            .ifPresent(builder::ambientParticle);
        getOptional(changes.ambientSoundLoopEvent(), effects.getAmbientLoopSoundEvent(), removals.removeAmbientLoopSoundEvent())
            .ifPresent(builder::ambientLoopSound);
        getOptional(changes.ambientMoodSettings(), effects.getAmbientMoodSettings(), removals.removeAmbientMoodSettings())
            .ifPresent(builder::ambientMoodSound);
        getOptional(changes.ambientAdditionsSettings(), effects.getAmbientAdditionsSettings(), removals.removeAmbientAdditionsSettings())
            .ifPresent(builder::ambientAdditionsSound);
        getOptional(changes.backgroundMusic(), effects.getBackgroundMusic(), removals.removeBackgroundMusic())
            .ifPresent(builder::backgroundMusic);

        return builder.build();
    }

    private BiomeGenerationSettings createGeneration(Biome reference) {
        final var builder = new BiomeGenerationSettings.PlainBuilder();
        this.addCarvers(builder, reference.getGenerationSettings());
        this.addFeatures(builder, reference.getGenerationSettings());
        return builder.build();
    }

    private void addCarvers(
            BiomeGenerationSettings.PlainBuilder builder, BiomeGenerationSettings generation) {
        final var changes = this.changes.generation().changes();
        final var additions = this.changes.generation().additions();
        final var removals = this.changes.generation().removals();

        final Map<Carving, List<Holder<ConfiguredWorldCarver<?>>>> carvers = new HashMap<>();
        final var carverSource = ((BiomeGenerationSettingsAccessor) generation).getCarvers();

        if (changes.carvers() != null) {
            for (final var step : Carving.values()) {
                final var change = changes.carvers().getOrDefault(step, carverSource.get(step));
                carvers.put(step, new ArrayList<>(change.stream().toList()));
            }
        } else {
            carverSource.forEach((step, set) -> carvers.put(step, new ArrayList<>(set.stream().toList())));
            if (removals.removeCarvers() != null) {
                removals.removeCarvers().compile()
                    .forEach(carver -> carvers.forEach((step, list) -> list.remove(carver)));
            }
        }
        carvers.forEach((step, set) -> set.forEach(carver -> builder.addCarver(step, carver)));
        if (additions.addCarvers() != null) {
            additions.addCarvers()
                .forEach((step, set) -> set.forEach(carver -> builder.addCarver(step, carver)));
        }
    }

    private void addFeatures(
            BiomeGenerationSettings.PlainBuilder builder, BiomeGenerationSettings generation) {
        final var changes = this.changes.generation().changes();
        final var additions = this.changes.generation().additions();
        final var removals = this.changes.generation().removals();

        final List<List<Holder<PlacedFeature>>> features = new ArrayList<>();
        if (changes.features() != null) {
            for (final var step : Decoration.values()) {
                HolderSet<PlacedFeature> change = null;
                if (changes.features().size() > step.ordinal()) {
                    change = changes.features().get(step.ordinal());
                }
                if (change == null) {
                    change = generation.features().get(step.ordinal());
                }
                features.add(new ArrayList<>(change.stream().toList()));
            }
        } else {
            generation.features().forEach(set -> features.add(new ArrayList<>(set.stream().toList())));
            if (removals.removeFeatures() != null) {
                removals.removeFeatures().compile()
                    .forEach(feature -> features.forEach(list -> list.remove(feature)));
            }
        }
        for (int i = 0; i < features.size(); i++) {
            for (final var feature : features.get(i)) {
                builder.addFeature(i, feature);
            }
        }
        if (additions.addFeatures() != null) {
            for (int i = 0; i < additions.addFeatures().size(); i++) {
                final var added = additions.addFeatures().get(i);
                if (added != null) {
                    for (final var feature : added) {
                        builder.addFeature(i, feature);
                    }
                }
            }
        }
    }

    private MobSpawnSettings createMobs(Biome reference) {
        final var builder = new MobSpawnSettings.Builder();
        final var changes = this.changes.mobs().changes();
        final var mobs = reference.getMobSettings();

        builder.creatureGenerationProbability(
            getOrElse(changes.creatureSpawnProbability(), mobs.getCreatureProbability()));

        this.addSpawners(builder, mobs);
        this.addSpawnCosts(builder, mobs);

        return builder.build();
    }

    private void addSpawners(MobSpawnSettings.Builder builder, MobSpawnSettings mobs) {
        final var changes = this.changes.mobs().changes();
        final var additions = this.changes.mobs().additions();
        final var removals = this.changes.mobs().removals();

        final Map<MobCategory, List<SpawnerData>> spawners = new HashMap<>();
        if (changes.spawners() != null) {
            for (final var category : MobCategory.values()) {
                final var change = changes.spawners().getOrDefault(category, mobs.getMobs(category));
                spawners.put(category, new ArrayList<>(change.unwrap()));
            }
        } else {
            for (final var category : MobCategory.values()) {
                spawners.put(category, new ArrayList<>(mobs.getMobs(category).unwrap()));
            }
            if (removals.removeSpawnCategories() != null) {
                removals.removeSpawnCategories().forEach(category -> spawners.get(category).clear());
            }
            if (removals.removeSpawners() != null) {
                removals.removeSpawners().compile()
                    .forEach(type -> spawners.forEach((category, list) ->
                        list.removeIf(data -> data.type == type)));
            }
        }
        spawners.forEach((category, list) -> list.forEach(data -> builder.addSpawn(category, data)));
        if (additions.addSpawners() != null) {
            additions.addSpawners().forEach((category, list) ->
                list.unwrap().forEach(data -> builder.addSpawn(category, data)));
        }
    }

    private void addSpawnCosts(MobSpawnSettings.Builder builder, MobSpawnSettings mobs) {
        final var changes = this.changes.mobs().changes();
        final var additions = this.changes.mobs().additions();
        final var removals = this.changes.mobs().removals();

        final Map<EntityType<?>, MobSpawnCost> costs =
            new HashMap<>(((MobSpawnSettingsAccessor) mobs).getMobSpawnCosts());
        if (changes.mobSpawnCosts() != null) {
            costs.putAll(changes.mobSpawnCosts());
        } else if (removals.removeMobSpawnCosts() != null) {
            removals.removeMobSpawnCosts().compile()
                .forEach(type -> costs.remove(type.value()));
        }
        if (additions.addMobSpawnCosts() != null) {
            costs.putAll(additions.addMobSpawnCosts());
        }
        costs.forEach((type, cost) -> builder.addMobCharge(type, cost.energyBudget(), cost.charge()));
    }

    @Override
    public MapCodec<BiomeInjector> codec() {
        return CODEC;
    }

    @Override
    public int priority() {
        return 500;
    }

    @Override
    public Stream<Holder<?>> getDependencies() {
        return Stream.ofNullable(this.parent);
    }

    private static Biome createDummyBiome() {
        return new Biome.BiomeBuilder()
            .temperature(0.7F)
            .downfall(0.8F)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .fogColor(12638463).waterColor(4159204).waterFogColor(329011).skyColor(7972607).build())
            .mobSpawnSettings(new MobSpawnSettings.Builder().build())
            .generationSettings(new BiomeGenerationSettings.PlainBuilder().build())
            .build();
    }

    @Contract("null,null->null;_,!null->!null")
    private static <T> T getOrElse(@Nullable T t, T defaultValue) {
        return t != null ? t : defaultValue;
    }

    private static int getColor(@Nullable Color change, int defaultValue) {
        return Optional.ofNullable(change).map(Color::getRGB).orElse(defaultValue);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Optional<Integer> getOptionalColor(
            @Nullable Color change, Optional<Integer> defaultValue, boolean hasRemoval) {
        if (change != null) {
            return Optional.of(change.getRGB());
        } else if (hasRemoval) {
            return Optional.empty();
        }
        return defaultValue;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static <T> Optional<T> getOptional(
            @Nullable T change, Optional<T> defaultValue, boolean hasRemoval) {
        if (change != null) {
            return Optional.of(change);
        } else if (hasRemoval) {
            return Optional.empty();
        }
        return defaultValue;
    }

    private static ResourceLocation removePrefix(ResourceLocation id) {
        if (id.getPath().startsWith("biome/")) {
            return new ResourceLocation(id.getNamespace(), id.getPath().substring("biome/".length()));
        }
        return id;
    }
}
