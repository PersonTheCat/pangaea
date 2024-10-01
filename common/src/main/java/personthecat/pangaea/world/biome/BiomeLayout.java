package personthecat.pangaea.world.biome;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate.Parameter;

import java.util.List;

public record BiomeLayout(ParameterMatrix<Biome> biomes) {
    private static final List<Parameter> DEFAULT_TEMPERATURE = List.of(
        Parameter.span(-1.0F, -0.45F),
        Parameter.span(-0.45F, -0.15F),
        Parameter.span(-0.15F, 0.2F),
        Parameter.span(0.2F, 0.55F),
        Parameter.span(0.55F, 1.0F)
    );
    private static final List<Parameter> DEFAULT_HUMIDITY = List.of(
        Parameter.span(-1.0F, -0.35F),
        Parameter.span(-0.35F, -0.1F),
        Parameter.span(-0.1F, 0.1F),
        Parameter.span(0.1F, 0.3F),
        Parameter.span(0.3F, 1.0F)
    );
    private static final MapCodec<ParameterMatrix<Biome>> BIOME_MATRIX_CODEC =
        ParameterMatrix.codecBuilder(Biome.CODEC)
            .withKeys("humidity", "temperature", "biomes")
            .withDefaultAxes(DEFAULT_HUMIDITY, DEFAULT_TEMPERATURE)
            .withVariantsNamed("biome")
            .build();

    public static final MapCodec<BiomeLayout> CODEC =
        BIOME_MATRIX_CODEC.xmap(BiomeLayout::new, BiomeLayout::biomes);
}
