package personthecat.pangaea.world.biome;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Climate.Parameter;
import personthecat.pangaea.registry.PgRegistries;

import java.util.List;

public record BiomeSlice(ParameterMatrix<Holder<BiomeLayout>> layouts) {
    private static final List<Parameter> DEFAULT_EROSION = List.of(
        Parameter.span(-1.0F, -0.78F),
        Parameter.span(-0.78F, -0.375F),
        Parameter.span(-0.375F, -0.2225F),
        Parameter.span(-0.2225F, 0.05F),
        Parameter.span(0.05F, 0.45F),
        Parameter.span(0.45F, 0.55F),
        Parameter.span(0.55F, 1.0F)
    );
    private static final List<Parameter> DEFAULT_CONTINENTALNESS = List.of(
        Parameter.span(-1.2F, -1.05F),
        Parameter.span(-1.05F, -0.455F),
        Parameter.span(-0.455F, -0.19F),
        Parameter.span(-0.19F, -0.11F),
        Parameter.span(-0.11F, 0.03F),
        Parameter.span(0.03F, 0.3F),
        Parameter.span(0.3F, 0.8F),
        Parameter.span(0.8F, 1.0F)
    );
    private static final MapCodec<ParameterMatrix<Holder<BiomeLayout>>> LAYOUT_MATRIX_CODEC =
        ParameterMatrix.codecBuilder(PgRegistries.BIOME_LAYOUT.holderCodec())
            .withKeys("erosion", "continentalness", "layouts")
            .withDefaultAxes(DEFAULT_EROSION, DEFAULT_CONTINENTALNESS)
            .build();
    public static final MapCodec<BiomeSlice> CODEC =
        LAYOUT_MATRIX_CODEC.xmap(BiomeSlice::new, BiomeSlice::layouts);
}
