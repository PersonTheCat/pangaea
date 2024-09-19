package personthecat.pangaea.serialization.codec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderSet;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import personthecat.pangaea.config.Cfg;

import java.util.List;
import java.util.Map;

import static personthecat.catlib.serialization.codec.CodecUtils.simpleEither;

public final class BiomeCodecs {
    private static final MapCodec<Map<Decoration, HolderSet<PlacedFeature>>> FEATURE_MAP =
        Codec.simpleMap(Decoration.CODEC, PlacedFeature.LIST_CODEC, StringRepresentable.keys(Decoration.values()));
    private static final Codec<List<HolderSet<PlacedFeature>>> FEATURES_FROM_MAP = FEATURE_MAP.codec()
        .xmap(BiomeCodecs::sortDecorations, BiomeCodecs::categorize);
    public static final Codec<List<HolderSet<PlacedFeature>>> SIMPLE_FEATURE_LIST =
        simpleEither(FEATURES_FROM_MAP, PlacedFeature.LIST_OF_LISTS_CODEC)
            .withEncoder(list -> Cfg.encodeFeatureCategories() ? FEATURES_FROM_MAP : PlacedFeature.LIST_OF_LISTS_CODEC);

    private BiomeCodecs() {}

    private static List<HolderSet<PlacedFeature>> sortDecorations(Map<Decoration, HolderSet<PlacedFeature>> map) {
        final var builder = ImmutableList.<HolderSet<PlacedFeature>>builder();
        for (final var step : Decoration.values()) {
            builder.add(map.getOrDefault(step, HolderSet.empty()));
        }
        return builder.build();
    }

    private static Map<Decoration, HolderSet<PlacedFeature>> categorize(List<HolderSet<PlacedFeature>> list) {
        final var builder = ImmutableMap.<Decoration, HolderSet<PlacedFeature>>builder();
        for (int i = 0; i < list.size(); i++) {
            builder.put(Decoration.values()[i], list.get(i));
        }
        return builder.build();
    }
}