package personthecat.pangaea.serialization.extras;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import personthecat.catlib.data.BiomePredicate;
import personthecat.pangaea.serialization.codec.BuilderCodec.BuilderField;
import personthecat.pangaea.world.placer.BiomeRestrictedBlockPlacer;
import personthecat.pangaea.world.placer.BlockPlacer;
import personthecat.pangaea.world.placer.ChanceBlockPlacer;
import personthecat.pangaea.world.placer.ColumnRestrictedBlockPlacer;
import personthecat.pangaea.world.placer.TargetedBlockPlacer;
import personthecat.pangaea.world.provider.ColumnProvider;

import java.util.List;
import java.util.function.Function;

public final class DefaultBuilderFields {
    public static final List<BuilderField<BlockPlacer, ?>> PLACER = List.of(
        BuilderField.of(BlockPlacer.class, BlockPlacer.class)
            .parsingRequired(BlockPlacer.CODEC, "place", "Nothing to place (missing 'place')")
            .wrap((place, next) -> place)
            .unwrap(placer -> null, Function.identity()),
        BuilderField.of(BlockPlacer.class, ColumnRestrictedBlockPlacer.class)
            .parsing(ColumnProvider.CODEC, "column")
            .wrap(ColumnRestrictedBlockPlacer::new)
            .unwrap(ColumnRestrictedBlockPlacer::place, ColumnRestrictedBlockPlacer::column),
        BuilderField.of(BlockPlacer.class, ChanceBlockPlacer.class)
            .parsing(Codec.DOUBLE, "chance")
            .wrap(ChanceBlockPlacer::new)
            .unwrap(ChanceBlockPlacer::place, ChanceBlockPlacer::chance),
        BuilderField.of(BlockPlacer.class, TargetedBlockPlacer.class)
            .parsing(RuleTest.CODEC, "target")
            .wrap(TargetedBlockPlacer::new)
            .unwrap(TargetedBlockPlacer::place, TargetedBlockPlacer::target),
        BuilderField.of(BlockPlacer.class, BiomeRestrictedBlockPlacer.class)
            .parsing(BiomePredicate.CODEC, "biomes")
            .wrap(BiomeRestrictedBlockPlacer::new)
            .unwrap(BiomeRestrictedBlockPlacer::place, BiomeRestrictedBlockPlacer::biomes)
    );

    private DefaultBuilderFields() {}
}
