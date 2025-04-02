package personthecat.pangaea.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import personthecat.catlib.data.IdList;
import personthecat.catlib.data.IdMatcher;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.idList;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public class MapFeatureConfiguration extends PangaeaFeatureConfiguration {
    private static final HeightProvider DEFAULT_HEIGHT =
        UniformHeight.of(VerticalAnchor.aboveBottom(8), VerticalAnchor.absolute(180));
    private static final IdList<Block> DEFAULT_REPLACEABLE =
        IdList.builder(Registries.BLOCK)
            .addEntries(IdMatcher.tag(false, BlockTags.OVERWORLD_CARVER_REPLACEABLES.location()))
            .build();

    public static final MapCodec<MapFeatureConfiguration> CODEC = codecOf(
        union(PangaeaFeatureConfiguration.CODEC, c -> c),
        defaulted(Codec.intRange(1, 32), "chunk_radius", 8, c -> c.chunkRadius),
        defaulted(HeightProvider.CODEC, "height", DEFAULT_HEIGHT, c -> c.height),
        defaulted(idList(Registries.BLOCK), "replaceable", DEFAULT_REPLACEABLE, c -> c.replaceable),
        MapFeatureConfiguration::new
    );

    public final int chunkRadius;
    public final HeightProvider height;
    public final IdList<Block> replaceable;

    public MapFeatureConfiguration(
            PangaeaFeatureConfiguration parent,
            int chunkRadius,
            HeightProvider height,
            IdList<Block> replaceable) {
        super(parent);
        this.chunkRadius = chunkRadius;
        this.height = height;
        this.replaceable = replaceable;
    }

    protected MapFeatureConfiguration(MapFeatureConfiguration source) {
        this(source, source.chunkRadius, source.height, source.replaceable);
    }
}
