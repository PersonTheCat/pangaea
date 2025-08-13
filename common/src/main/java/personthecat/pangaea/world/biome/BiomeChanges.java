package personthecat.pangaea.world.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome.TemperatureModifier;
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier;
import net.minecraft.world.level.biome.MobSpawnSettings.MobSpawnCost;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.IdList;
import personthecat.pangaea.serialization.codec.ColorCodecs;

import java.awt.Color;
import java.util.List;
import java.util.Map;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.idList;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleEither;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.nullable;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public record BiomeChanges(
    ClimateCategory climate,
    EffectsCategory effects,
    GenerationCategory generation,
    MobSpawnCategory mobs) {

    private static final Keyable CARVING_KEYS = StringRepresentable.keys(Carving.values());
    private static final MapCodec<Map<Carving, HolderSet<ConfiguredWorldCarver<?>>>> CARVER_CODEC =
        Codec.simpleMap(Carving.CODEC, ConfiguredWorldCarver.LIST_CODEC, CARVING_KEYS);
    private static final Codec<IdList<ConfiguredWorldCarver<?>>> CARVER_LIST_CODEC = idList(Registries.CONFIGURED_CARVER);
    private static final Codec<IdList<PlacedFeature>> FEATURE_LIST_CODEC = idList(Registries.PLACED_FEATURE);
    private static final Keyable CATEGORY_KEYS = StringRepresentable.keys(MobCategory.values());
    private static final Codec<WeightedRandomList<SpawnerData>> SPAWNER_LIST_CODEC =
        WeightedRandomList.codec(SpawnerData.CODEC);
    private static final MapCodec<Map<MobCategory, WeightedRandomList<SpawnerData>>> SPAWNER_CODEC =
        Codec.simpleMap(MobCategory.CODEC, SPAWNER_LIST_CODEC, CATEGORY_KEYS);
    private static final MapCodec<Map<EntityType<?>, MobSpawnCost>> SPAWN_COST_CODEC =
        Codec.simpleMap(BuiltInRegistries.ENTITY_TYPE.byNameCodec(), MobSpawnCost.CODEC, BuiltInRegistries.ENTITY_TYPE);
    private static final Codec<IdList<EntityType<?>>> ENTITY_TYPE_LIST = idList(Registries.ENTITY_TYPE);
    private static final Codec<List<MobCategory>> ALL_CATEGORIES_CODEC =
        Codec.BOOL.fieldOf("all").codec()
            .xmap(b -> b ? List.of(MobCategory.values()) : List.of(), l -> !l.isEmpty());
    private static final Codec<List<MobCategory>> DIRECT_CATEGORY_LIST = MobCategory.CODEC.listOf();
    private static final Codec<List<MobCategory>> CATEGORY_LIST_CODEC =
        simpleEither(DIRECT_CATEGORY_LIST, ALL_CATEGORIES_CODEC)
            .withEncoder(l -> l.size() == MobCategory.values().length ? ALL_CATEGORIES_CODEC : DIRECT_CATEGORY_LIST);

    public static final MapCodec<BiomeChanges> CODEC = codecOf(
        union(ClimateCategory.CODEC, BiomeChanges::climate),
        defaulted(EffectsCategory.CODEC.codec(), "effects", EffectsCategory.NONE, BiomeChanges::effects),
        union(GenerationCategory.CODEC, BiomeChanges::generation),
        union(MobSpawnCategory.CODEC, BiomeChanges::mobs),
        BiomeChanges::new
    );

    public boolean hasRemovals() {
        return this.effects.removals.hasRemovals() 
            || this.generation.removals.hasRemovals() 
            || this.mobs.removals.hasRemovals();
    }
    
    public boolean hasAdditions() {
        return this.generation.additions.hasAdditions() 
            || this.mobs.additions.hasAdditions();
    }
    
    public boolean hasChanges() {
        return this.climate.changes.hasChanges() 
            || this.effects.changes.hasChanges() 
            || this.generation.changes.hasChanges()
            || this.mobs.changes.hasChanges();
    }
    
    public record ClimateCategory(ClimateChanges changes) {
        public static final MapCodec<ClimateCategory> CODEC =
            ClimateChanges.CODEC.xmap(ClimateCategory::new, ClimateCategory::changes);
    }

    public record ClimateChanges(
        @Nullable Boolean hasPrecipitation,
        @Nullable Float temperature,
        @Nullable TemperatureModifier temperatureModifier,
        @Nullable Float downfall) {

        public static final MapCodec<ClimateChanges> CODEC = codecOf(
            nullable(Codec.BOOL, "has_precipitation", ClimateChanges::hasPrecipitation),
            nullable(Codec.FLOAT, "temperature", ClimateChanges::temperature),
            nullable(TemperatureModifier.CODEC, "temperature_modifier", ClimateChanges::temperatureModifier),
            nullable(Codec.FLOAT, "downfall", ClimateChanges::downfall),
            ClimateChanges::new
        );

        public boolean hasChanges() {
            return this.hasPrecipitation != null
                || this.temperature != null
                || this.temperatureModifier != null
                || this.downfall != null;
        }
    }

    public record EffectsCategory(
        EffectsChanges changes,
        EffectsRemovals removals) {

        private static final EffectsCategory NONE =
            new EffectsCategory(EffectsChanges.NONE, EffectsRemovals.NONE);
        public static final MapCodec<EffectsCategory> CODEC = codecOf(
            union(EffectsChanges.CODEC, EffectsCategory::changes),
            union(EffectsRemovals.CODEC, EffectsCategory::removals),
            EffectsCategory::new
        );
    }

    public record EffectsChanges(
        @Nullable Color fogColor,
        @Nullable Color waterColor,
        @Nullable Color waterFogColor,
        @Nullable Color skyColor,
        @Nullable Color foliageColor,
        @Nullable Color grassColor,
        @Nullable GrassColorModifier grassColorModifier,
        @Nullable AmbientParticleSettings ambientParticleSettings,
        @Nullable Holder<SoundEvent> ambientSoundLoopEvent,
        @Nullable AmbientMoodSettings ambientMoodSettings,
        @Nullable AmbientAdditionsSettings ambientAdditionsSettings,
        @Nullable Music backgroundMusic) {

        private static final EffectsChanges NONE =
            new EffectsChanges(null, null, null, null, null, null, null, null, null, null, null, null);
        public static final MapCodec<EffectsChanges> CODEC = codecOf(
            nullable(ColorCodecs.COLOR, "fog_color", EffectsChanges::fogColor),
            nullable(ColorCodecs.COLOR, "water_color", EffectsChanges::waterColor),
            nullable(ColorCodecs.COLOR, "water_fog_color", EffectsChanges::waterFogColor),
            nullable(ColorCodecs.COLOR, "sky_color", EffectsChanges::skyColor),
            nullable(ColorCodecs.COLOR, "foliage_color", EffectsChanges::foliageColor),
            nullable(ColorCodecs.COLOR, "grass_color", EffectsChanges::grassColor),
            nullable(GrassColorModifier.CODEC, "grass_color_modifier", EffectsChanges::grassColorModifier),
            nullable(AmbientParticleSettings.CODEC, "particle", EffectsChanges::ambientParticleSettings),
            nullable(SoundEvent.CODEC, "ambient_sound", EffectsChanges::ambientSoundLoopEvent),
            nullable(AmbientMoodSettings.CODEC, "ambient_mood", EffectsChanges::ambientMoodSettings),
            nullable(AmbientAdditionsSettings.CODEC, "additions_sound", EffectsChanges::ambientAdditionsSettings),
            nullable(Music.CODEC, "music", EffectsChanges::backgroundMusic),
            EffectsChanges::new
        );

        public boolean hasChanges() {
            return this.fogColor != null
                || this.waterColor != null
                || this.waterFogColor != null
                || this.skyColor != null
                || this.foliageColor != null
                || this.grassColor != null
                || this.grassColorModifier != null
                || this.ambientParticleSettings != null
                || this.ambientSoundLoopEvent != null
                || this.ambientMoodSettings != null
                || this.ambientAdditionsSettings != null
                || this.backgroundMusic != null;
        }
    }

    public record EffectsRemovals(
        boolean removeFoliageColor,
        boolean removeGrassColor,
        boolean removeAmbientParticleSettings,
        boolean removeAmbientLoopSoundEvent,
        boolean removeAmbientMoodSettings,
        boolean removeAmbientAdditionsSettings,
        boolean removeBackgroundMusic) {

        private static final EffectsRemovals NONE =
            new EffectsRemovals(false, false, false, false, false, false, false);
        public static final MapCodec<EffectsRemovals> CODEC = codecOf(
            defaulted(Codec.BOOL, "remove_foliage_color", false, EffectsRemovals::removeFoliageColor),
            defaulted(Codec.BOOL, "remove_grass_color", false, EffectsRemovals::removeGrassColor),
            defaulted(Codec.BOOL, "remove_particle", false, EffectsRemovals::removeAmbientParticleSettings),
            defaulted(Codec.BOOL, "remove_ambient_sound", false, EffectsRemovals::removeAmbientLoopSoundEvent),
            defaulted(Codec.BOOL, "remove_mood_sound", false, EffectsRemovals::removeAmbientMoodSettings),
            defaulted(Codec.BOOL, "remove_additions_sound", false, EffectsRemovals::removeAmbientAdditionsSettings),
            defaulted(Codec.BOOL, "remove_music", false, EffectsRemovals::removeBackgroundMusic),
            EffectsRemovals::new
        );

        public boolean hasRemovals() {
            return this.removeFoliageColor
                || this.removeGrassColor
                || this.removeAmbientParticleSettings
                || this.removeAmbientLoopSoundEvent
                || this.removeAmbientMoodSettings
                || this.removeAmbientAdditionsSettings
                || this.removeBackgroundMusic;
        }
    }

    public record GenerationCategory(
        GenerationChanges changes,
        GenerationAdditions additions,
        GenerationRemovals removals) {

        public static final MapCodec<GenerationCategory> CODEC = codecOf(
            union(GenerationChanges.CODEC, GenerationCategory::changes),
            union(GenerationAdditions.CODEC, GenerationCategory::additions),
            union(GenerationRemovals.CODEC, GenerationCategory::removals),
            GenerationCategory::new
        );
    }

    public record GenerationChanges(
        @Nullable Map<Carving, HolderSet<ConfiguredWorldCarver<?>>> carvers,
        @Nullable List<HolderSet<PlacedFeature>> features) {

        public static final MapCodec<GenerationChanges> CODEC = codecOf(
            nullable(CARVER_CODEC.codec(), "carvers", GenerationChanges::carvers),
            nullable(PlacedFeature.LIST_OF_LISTS_CODEC, "features", GenerationChanges::features),
            GenerationChanges::new
        );

        public boolean hasChanges() {
            return this.carvers != null || this.features != null;
        }
    }

    public record GenerationAdditions(
        @Nullable Map<Carving, HolderSet<ConfiguredWorldCarver<?>>> addCarvers,
        @Nullable List<HolderSet<PlacedFeature>> addFeatures) {

        public static final MapCodec<GenerationAdditions> CODEC = codecOf(
            nullable(CARVER_CODEC.codec(), "add_carvers", GenerationAdditions::addCarvers),
            nullable(PlacedFeature.LIST_OF_LISTS_CODEC, "add_features", GenerationAdditions::addFeatures),
            GenerationAdditions::new
        );

        public boolean hasAdditions() {
            return this.addCarvers != null || this.addFeatures != null;
        }
    }

    public record GenerationRemovals(
        @Nullable IdList<ConfiguredWorldCarver<?>> removeCarvers,
        @Nullable IdList<PlacedFeature> removeFeatures) {

        public static final MapCodec<GenerationRemovals> CODEC = codecOf(
            nullable(CARVER_LIST_CODEC, "remove_carvers", GenerationRemovals::removeCarvers),
            nullable(FEATURE_LIST_CODEC, "remove_features", GenerationRemovals::removeFeatures),
            GenerationRemovals::new
        );

        public boolean hasRemovals() {
            return this.removeCarvers != null || this.removeFeatures != null;
        }
    }

    public record MobSpawnCategory(
        MobSpawnChanges changes,
        MobSpawnAdditions additions,
        MobSpawnRemovals removals) {

        public static MapCodec<MobSpawnCategory> CODEC = codecOf(
            union(MobSpawnChanges.CODEC, MobSpawnCategory::changes),
            union(MobSpawnAdditions.CODEC, MobSpawnCategory::additions),
            union(MobSpawnRemovals.CODEC, MobSpawnCategory::removals),
            MobSpawnCategory::new
        );
    }

    public record MobSpawnChanges(
        @Nullable Float creatureSpawnProbability,
        @Nullable Map<MobCategory, WeightedRandomList<SpawnerData>> spawners,
        @Nullable Map<EntityType<?>, MobSpawnCost> mobSpawnCosts) {

        public static final MapCodec<MobSpawnChanges> CODEC = codecOf(
            nullable(Codec.FLOAT, "creature_spawn_probability", MobSpawnChanges::creatureSpawnProbability),
            nullable(SPAWNER_CODEC.codec(), "spawners", MobSpawnChanges::spawners),
            nullable(SPAWN_COST_CODEC.codec(), "spawn_costs", MobSpawnChanges::mobSpawnCosts),
            MobSpawnChanges::new
        );

        public boolean hasChanges() {
            return this.creatureSpawnProbability != null
                || this.spawners != null
                || this.mobSpawnCosts != null;
        }
    }

    public record MobSpawnAdditions(
        @Nullable Map<MobCategory, WeightedRandomList<SpawnerData>> addSpawners,
        @Nullable Map<EntityType<?>, MobSpawnCost> addMobSpawnCosts) {

        public static final MapCodec<MobSpawnAdditions> CODEC = codecOf(
            nullable(SPAWNER_CODEC.codec(), "add_spawners", MobSpawnAdditions::addSpawners),
            nullable(SPAWN_COST_CODEC.codec(), "add_spawn_costs", MobSpawnAdditions::addMobSpawnCosts),
            MobSpawnAdditions::new
        );

        public boolean hasAdditions() {
            return this.addSpawners != null || this.addMobSpawnCosts != null;
        }
    }

    public record MobSpawnRemovals(
        @Nullable List<MobCategory> removeSpawnCategories,
        @Nullable IdList<EntityType<?>> removeSpawners,
        @Nullable IdList<EntityType<?>> removeMobSpawnCosts) {

        public static final MapCodec<MobSpawnRemovals> CODEC = codecOf(
            nullable(CATEGORY_LIST_CODEC, "remove_spawn_categories", MobSpawnRemovals::removeSpawnCategories),
            nullable(ENTITY_TYPE_LIST, "remove_spawners", MobSpawnRemovals::removeSpawners),
            nullable(ENTITY_TYPE_LIST, "remove_spawn_costs", MobSpawnRemovals::removeMobSpawnCosts),
            MobSpawnRemovals::new
        );

        public boolean hasRemovals() {
            return this.removeSpawnCategories != null
                || this.removeSpawners != null
                || this.removeMobSpawnCosts != null;
        }
    }
}
