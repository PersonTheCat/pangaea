package personthecat.pangaea.world.injector;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import personthecat.catlib.data.DimensionPredicate;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.mixin.accessor.NoiseRouterAccessor;
import personthecat.pangaea.world.density.InjectedDensity;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public record DensityInjector(
        DimensionPredicate dimensions,
        boolean removeEntrances,
        boolean removeUndergroundCaverns,
        boolean removeUndergroundFiller,
        boolean removeGlobalCaverns,
        boolean removeSurface,
        InjectedDensity additions) implements Injector {

    public static final MapCodec<DensityInjector> CODEC = codecOf(
        field(DimensionPredicate.CODEC, "dimensions", DensityInjector::dimensions),
        defaulted(Codec.BOOL, "remove_entrances", false, DensityInjector::removeEntrances),
        defaulted(Codec.BOOL, "remove_underground_caverns", false, DensityInjector::removeUndergroundCaverns),
        defaulted(Codec.BOOL, "remove_underground_filler", false, DensityInjector::removeUndergroundFiller),
        defaulted(Codec.BOOL, "remove_global_caverns", false, DensityInjector::removeGlobalCaverns),
        defaulted(Codec.BOOL, "remove_surface", false, DensityInjector::removeSurface),
        union(InjectedDensity.CODEC, DensityInjector::additions),
        DensityInjector::new
    );

    @Override
    public void inject(ResourceKey<Injector> key, InjectionContext ctx) {
        ctx.registries().registryOrThrow(Registries.LEVEL_STEM).holders().forEach(stem -> {
            if (this.dimensions.test(stem.value())) {
                this.modifyDimension(stem);
                ctx.addCleanupTask(key, () -> this.restorePrefix(stem));
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void modifyDimension(Holder<LevelStem> stem) {
        if (!(stem.value().generator() instanceof NoiseBasedChunkGenerator g)) {
            final var key = stem.unwrapKey().orElseThrow();
            LibErrorContext.warn(Pangaea.MOD, InjectionWarningException.incompatibleGenerator(key));
            return;
        }
        if (!((Object) g.generatorSettings().value().noiseRouter() instanceof NoiseRouterAccessor r)) {
            throw new IllegalStateException("NoiseRouter mixin not applied successfully");
        }
        final var density = g.generatorSettings().value().noiseRouter().finalDensity();
        if (density instanceof InjectedDensity injected) {
            this.applyInjections(injected);
            return;
        }
        r.setFinalDensity(this.additions);
    }

    private void applyInjections(InjectedDensity density) {
        if (this.removeEntrances) {
            density.entrances.clear();
        }
        if (this.removeUndergroundCaverns) {
            density.undergroundCaverns.clear();
        }
        if (this.removeUndergroundFiller) {
            density.undergroundFiller.clear();
        }
        if (this.removeGlobalCaverns) {
            density.globalCaverns.clear();
        }
        if (this.removeSurface) {
            density.surface = null;
        }
        final var additions = this.additions;
        density.entrances.addAll(additions.entrances);
        density.undergroundCaverns.addAll(additions.undergroundCaverns);
        density.undergroundFiller.addAll(additions.undergroundFiller);
        density.globalCaverns.addAll(additions.globalCaverns);
        if (additions.surface != null) {
            density.surface = additions.surface;
        }
        if (additions.upperCutoff != null) {
            density.upperCutoff = additions.upperCutoff;
        }
        if (additions.lowerCutoff != null) {
            density.lowerCutoff = additions.lowerCutoff;
        }
        if (additions.surfaceThreshold != null) {
            density.surfaceThreshold = additions.surfaceThreshold;
        }
        if (additions.fillerThreshold != null) {
            density.fillerThreshold = additions.fillerThreshold;
        }
        if (additions.thickness != null) {
            density.thickness = additions.thickness;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void restorePrefix(Holder<LevelStem> stem) {
        if (!(stem.value().generator() instanceof NoiseBasedChunkGenerator g)) {
            return;
        }
        final var density = g.generatorSettings().value().noiseRouter().finalDensity();
        if (density instanceof InjectedDensity injected) {
            ((NoiseRouterAccessor) (Object) g.generatorSettings().value().noiseRouter()).setFinalDensity(injected.optimize());
        }
    }

    @Override
    public Phase phase() {
        return Phase.DIMENSION;
    }

    public MapCodec<DensityInjector> codec() {
        return CODEC;
    }
}
