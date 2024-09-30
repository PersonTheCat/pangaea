package personthecat.pangaea.world.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate.Parameter;
import net.minecraft.world.level.biome.Climate.ParameterList;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import personthecat.pangaea.registry.PgRegistries;

import java.util.List;

public record DimensionLayout(ParameterMatrix<Holder<BiomeSlice>> slices) {
    private static final List<Parameter> DEFAULT_DEPTH = List.of(
        Parameter.span(0.0F, 0.0F),
        Parameter.span(0.0F, 0.2F),
        Parameter.span(0.2F, 0.9F),
        Parameter.span(0.9F, 1.0F),
        Parameter.span(1.0F, 1.0F),
        Parameter.span(1.0F, 1.1F),
        Parameter.span(1.1F, 1.1F)
    );
    private static final List<Parameter> DEFAULT_WEIRDNESS = List.of(
        Parameter.span(-1.0F, -0.933F),
        Parameter.span(-0.933F, -0.767F),
        Parameter.span(-0.767F, -0.567F),
        Parameter.span(-0.567F, -0.4F),
        Parameter.span(-0.4F, -0.267F),
        Parameter.span(-0.267F, -0.05F),
        Parameter.span(-0.05F, 0.05F),
        Parameter.span(0.05F, 0.267F),
        Parameter.span(0.267F, 0.4F),
        Parameter.span(0.4F, 0.567F),
        Parameter.span(0.567F, 0.767F),
        Parameter.span(0.767F, 0.933F),
        Parameter.span(0.933F, 1.0F)
    );
    private static final MapCodec<ParameterMatrix<Holder<BiomeSlice>>> SLICE_MATRIX_CODEC =
        ParameterMatrix.codecBuilder(PgRegistries.BIOME_SLICE.holderCodec())
            .withKeys("weirdness", "depth", "slices")
            .withDefaultAxes(DEFAULT_WEIRDNESS, DEFAULT_DEPTH)
            .build();
    public static final MapCodec<DimensionLayout> CODEC =
        SLICE_MATRIX_CODEC.xmap(DimensionLayout::new, DimensionLayout::slices);

    public ParameterList<Holder<Biome>> compileBiomes(Registry<Biome> biomes) {
        final var list = ImmutableList.<Pair<ParameterPoint, Holder<Biome>>>builder();
        this.slices.forEach((weirdness, depth, slice) ->
            slice.value().layouts().forEach((erosion, continentalness, layout) ->
                layout.value().biomes().forEach((humidity, temperature, choice) -> {
                    final var point = new ParameterPoint(
                        temperature, humidity, continentalness, erosion, depth, weirdness, 0);
                    final var biome = choice.resolve(layout.value(), point);
                    list.add(Pair.of(point, biomes.getHolderOrThrow(biome)));
                })));
        return new ParameterList<>(list.build());
    }
}
