package personthecat.pangaea.world.injector;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import personthecat.catlib.data.DimensionPredicate;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.mixin.MultiNoiseBiomeSourceAccessor;
import personthecat.pangaea.world.biome.DimensionLayout;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public record DimensionInjector(DimensionPredicate dimensions, DimensionLayout slices) implements Injector {
    public static final MapCodec<DimensionInjector> CODEC = codecOf(
        field(DimensionPredicate.CODEC, "dimensions", DimensionInjector::dimensions),
        union(DimensionLayout.CODEC, DimensionInjector::slices),
        DimensionInjector::new
    );

    @Override
    public void inject(ResourceKey<Injector> key, InjectionContext ctx) {
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
        final var biomes = DynamicRegistries.BIOME.asRegistry();
        source.setParameters(Either.left(this.slices.compileBiomes(biomes)));
    }

    @Override
    public MapCodec<DimensionInjector> codec() {
        return CODEC;
    }
}
