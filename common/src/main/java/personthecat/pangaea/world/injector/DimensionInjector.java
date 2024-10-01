package personthecat.pangaea.world.injector;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import personthecat.catlib.data.DimensionPredicate;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.mixin.MultiNoiseBiomeSourceAccessor;
import personthecat.pangaea.world.biome.DimensionLayout;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public record DimensionInjector(
        DimensionPredicate dimensions, DimensionLayout slices, boolean isDebug) implements Injector {
    public static final MapCodec<DimensionInjector> CODEC = codecOf(
        field(DimensionPredicate.CODEC, "dimensions", DimensionInjector::dimensions),
        union(DimensionLayout.CODEC, DimensionInjector::slices),
        defaulted(Codec.BOOL, "is_temporary_debug", false, DimensionInjector::isDebug),
        DimensionInjector::new
    );

    @Override
    public void inject(ResourceKey<Injector> key, InjectionContext ctx) {
        if (this.isDebug && !Cfg.enableTemporaryDebugFeatures()) {
            return;
        }
        ctx.registries().registryOrThrow(Registries.LEVEL_STEM).holders().forEach(stem -> {
            if (this.dimensions.test(stem.value())) {
                this.modifyDimension(stem);
            }
        });
    }

    private void modifyDimension(Holder<LevelStem> stem) {
        if (!(stem.value().generator().getBiomeSource() instanceof MultiNoiseBiomeSourceAccessor source)) {
            final var key = stem.unwrapKey().orElseThrow();
            LibErrorContext.warn(Pangaea.MOD, InjectionWarningException.incompatibleBiomeSource(key));
            return;
        }
        source.setParameters(Either.left(this.slices.compileBiomes()));
    }

    @Override
    public int priority() {
        return 500;
    }

    @Override
    public Phase phase() {
        return Phase.DIMENSION;
    }

    @Override
    public MapCodec<DimensionInjector> codec() {
        return CODEC;
    }
}
