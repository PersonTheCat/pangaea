package personthecat.pangaea.world.injector;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.pangaea.world.injector.BiomeChanges.ClimateChanges;
import personthecat.pangaea.world.injector.BiomeChanges.EffectsChanges;
import personthecat.pangaea.world.injector.BiomeChanges.EffectsRemovals;
import personthecat.pangaea.world.injector.BiomeChanges.GenerationAdditions;
import personthecat.pangaea.world.injector.BiomeChanges.GenerationChanges;
import personthecat.pangaea.world.injector.BiomeChanges.GenerationRemovals;
import personthecat.pangaea.world.injector.BiomeChanges.MobSpawnAdditions;
import personthecat.pangaea.world.injector.BiomeChanges.MobSpawnChanges;
import personthecat.pangaea.world.injector.BiomeChanges.MobSpawnRemovals;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public record BiomeModifierInjector(BiomePredicate biomes, BiomeChanges changes) implements Injector {
    public static final MapCodec<BiomeModifierInjector> CODEC = codecOf(
        field(BiomePredicate.CODEC, "biomes", BiomeModifierInjector::biomes),
        union(BiomeChanges.CODEC, BiomeModifierInjector::changes),
        BiomeModifierInjector::new
    );

    @Override
    public void inject(InjectionContext ctx) {
        if (this.changes.hasRemovals()) {
            ctx.addRemovals(this.biomes, this::doRemovals);
        }
        if (this.changes.hasAdditions() || this.changes.hasChanges()) {
            ctx.addAdditions(this.biomes, this::doAdditions);
        }
    }

    private void doRemovals(FeatureModificationContext ctx) {
        if (this.changes.effects().removals().hasRemovals()) {
            this.doEffectRemovals(ctx, this.changes.effects().removals());
        }
        if (this.changes.generation().removals().hasRemovals()) {
            this.doGenerationRemovals(ctx, this.changes.generation().removals());
        }
        if (this.changes.mobs().removals().hasRemovals()) {
            this.doMobRemovals(ctx, this.changes.mobs().removals());
        }
    }

    private void doEffectRemovals(FeatureModificationContext ctx, EffectsRemovals removals) {
        if (removals.removeFoliageColor()) {
            ctx.setFoliageColorOverride(null);
        }
        if (removals.removeGrassColor()) {
            ctx.setGrassColorOverride(null);
        }
        if (removals.removeAmbientParticleSettings()) {
            ctx.setAmbientParticleSettings(null);
        }
        if (removals.removeAmbientLoopSoundEvent()) {
            ctx.setAmbientLoopSound(null);
        }
        if (removals.removeAmbientMoodSettings()) {
            ctx.setAmbientMoodSound(null);
        }
        if (removals.removeAmbientAdditionsSettings()) {
            ctx.setAmbientAdditionsSound(null);
        }
        if (removals.removeBackgroundMusic()) {
            ctx.setBackgroundMusic(null);
        }
    }

    private void doGenerationRemovals(FeatureModificationContext ctx, GenerationRemovals removals) {
        if (removals.removeCarvers() != null) {
            removals.removeCarvers().compileIds().forEach(ctx::removeCarver);
        }
        if (removals.removeFeatures() != null) {
            removals.removeFeatures().compileIds().forEach(ctx::removeFeature);
        }
    }

    private void doMobRemovals(FeatureModificationContext ctx, MobSpawnRemovals removals) {
        if (removals.removeSpawnCategories() != null) {
            removals.removeSpawnCategories().forEach(ctx::removeSpawns);
        }
        if (removals.removeSpawners() != null) {
            removals.removeSpawners().compile().forEach(holder -> ctx.removeSpawn(holder.value()));
        }
        if (removals.removeMobSpawnCosts() != null) {
            removals.removeMobSpawnCosts().compile().forEach(holder -> ctx.removeSpawnCost(holder.value()));
        }
    }

    private void doAdditions(FeatureModificationContext ctx) {
        if (this.changes.climate().changes().hasChanges()) {
            this.doClimateChanges(ctx, this.changes.climate().changes());
        }
        if (this.changes.effects().changes().hasChanges()) {
            this.doEffectsChanges(ctx, this.changes.effects().changes());
        }
        if (this.changes.generation().changes().hasChanges()) {
            this.doGenerationChanges(ctx, this.changes.generation().changes());
        }
        if (this.changes.mobs().changes().hasChanges()) {
            this.doMobChanges(ctx, this.changes.mobs().changes());
        }
        if (this.changes.generation().additions().hasAdditions()) {
            this.doGenerationAdditions(ctx, this.changes.generation().additions());
        }
        if (this.changes.mobs().additions().hasAdditions()) {
            this.doMobAdditions(ctx, this.changes.mobs().additions());
        }
    }

    private void doClimateChanges(FeatureModificationContext ctx, ClimateChanges changes) {
        if (changes.hasPrecipitation() != null) {
            ctx.setHasPrecipitation(changes.hasPrecipitation());
        }
        if (changes.temperature() != null) {
            ctx.setTemperature(changes.temperature());
        }
        if (changes.temperatureModifier() != null) {
            ctx.setTemperatureModifier(changes.temperatureModifier());
        }
        if (changes.downfall() != null) {
            ctx.setDownfall(changes.downfall());
        }
    }

    private void doEffectsChanges(FeatureModificationContext ctx, EffectsChanges changes) {
        if (changes.fogColor() != null) {
            ctx.setFogColor(changes.fogColor().getRGB());
        }
        if (changes.waterColor() != null) {
            ctx.setWaterColor(changes.waterColor().getRGB());
        }
        if (changes.waterFogColor() != null) {
            ctx.setWaterFogColor(changes.waterFogColor().getRGB());
        }
        if (changes.skyColor() != null) {
            ctx.setSkyColor(changes.skyColor().getRGB());
        }
        if (changes.foliageColor() != null) {
            ctx.setFoliageColorOverride(changes.foliageColor().getRGB());
        }
        if (changes.grassColor() != null) {
            ctx.setGrassColorOverride(changes.grassColor().getRGB());
        }
        if (changes.grassColorModifier() != null) {
            ctx.setGrassColorModifier(changes.grassColorModifier());
        }
        if (changes.ambientParticleSettings() != null) {
            ctx.setAmbientParticleSettings(changes.ambientParticleSettings());
        }
        if (changes.ambientSoundLoopEvent() != null) {
            ctx.setAmbientLoopSound(changes.ambientSoundLoopEvent());
        }
        if (changes.ambientMoodSettings() != null) {
            ctx.setAmbientMoodSound(changes.ambientMoodSettings());
        }
        if (changes.ambientAdditionsSettings() != null) {
            ctx.setAmbientAdditionsSound(changes.ambientAdditionsSettings());
        }
        if (changes.backgroundMusic() != null) {
            ctx.setBackgroundMusic(changes.backgroundMusic());
        }
    }

    private void doGenerationChanges(FeatureModificationContext ctx, GenerationChanges changes) {
        if (changes.carvers() != null) {
            changes.carvers().forEach((step, list) -> {
                ctx.removeCarver(step, carver -> true);
                list.forEach(holder -> ctx.addCarver(step, holder));
            });
        }
        if (changes.features() != null) {
            for (int i = 0; i < changes.features().size(); i++) {
                final var step = Decoration.values()[i];
                ctx.removeFeature(step, feature -> true);
                changes.features().get(i).forEach(feature -> ctx.addFeature(step, feature));
            }
        }
    }

    private void doMobChanges(FeatureModificationContext ctx, MobSpawnChanges changes) {
        if (changes.creatureSpawnProbability() != null) {
            ctx.setCreatureSpawnProbability(changes.creatureSpawnProbability());
        }
        if (changes.spawners() != null) {
            changes.spawners().forEach((category, list) -> {
                ctx.removeSpawns(category);
                list.unwrap().forEach(data -> ctx.addSpawn(category, data));
            });
        }
        if (changes.mobSpawnCosts() != null) {
            changes.mobSpawnCosts().forEach((type, cost) ->
                ctx.setSpawnCost(type, cost.energyBudget(), cost.charge()));
        }
    }

    private void doGenerationAdditions(FeatureModificationContext ctx, GenerationAdditions additions) {
        if (additions.addCarvers() != null) {
            additions.addCarvers().forEach((step, carvers) ->
                carvers.forEach(carver -> ctx.addCarver(step, carver)));
        }
        if (additions.addFeatures() != null) {
            for (int i = 0; i < additions.addFeatures().size(); i++) {
                final var step = Decoration.values()[i];
                additions.addFeatures().get(i).forEach(feature -> ctx.addFeature(step, feature));
            }
        }
    }

    private void doMobAdditions(FeatureModificationContext ctx, MobSpawnAdditions additions) {
        if (additions.addSpawners() != null) {
            additions.addSpawners().forEach((category, list) ->
                list.unwrap().forEach(data -> ctx.addSpawn(category, data)));
        }
        if (additions.addMobSpawnCosts() != null) {
            additions.addMobSpawnCosts().forEach((type, cost) ->
                ctx.setSpawnCost(type, cost.energyBudget(), cost.charge()));
        }
    }

    @Override
    public MapCodec<BiomeModifierInjector> codec() {
        return CODEC;
    }
}
