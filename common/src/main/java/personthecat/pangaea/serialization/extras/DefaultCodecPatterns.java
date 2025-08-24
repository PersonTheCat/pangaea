package personthecat.pangaea.serialization.extras;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.SurfaceRules.BlockRuleSource;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.minecraft.world.level.levelgen.SurfaceRules.SequenceRuleSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockStateMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import personthecat.catlib.data.FloatRange;
import personthecat.catlib.data.IdList;
import personthecat.catlib.data.Range;
import personthecat.catlib.registry.RegistryUtils;
import personthecat.pangaea.mixin.accessor.BlockMatchTestAccessor;
import personthecat.pangaea.mixin.accessor.BlockStateMatchTestAccessor;
import personthecat.pangaea.mixin.accessor.TagMatchTestAccessor;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.serialization.codec.PatternCodec.Pattern;
import personthecat.pangaea.serialization.codec.TestPattern;
import personthecat.pangaea.world.filter.ChanceChunkFilter;
import personthecat.pangaea.world.filter.ChunkFilter;
import personthecat.pangaea.world.filter.UnionChunkFilter;
import personthecat.pangaea.world.placer.BlockPlacer;
import personthecat.pangaea.world.placer.BlockPlacerList;
import personthecat.pangaea.world.placer.UnconditionalBlockPlacer;
import personthecat.pangaea.world.provider.ColumnProvider;
import personthecat.pangaea.world.ruletest.HeterogeneousListRuleTest;
import personthecat.pangaea.world.surface.AllConditionSource;
import personthecat.pangaea.world.weight.ConstantWeight;
import personthecat.pangaea.world.weight.WeightFunction;
import personthecat.pangaea.world.weight.WeightList;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class DefaultCodecPatterns {
    private static final Codec<UnconditionalBlockPlacer> STATE_PLACER =
        BlockState.CODEC.xmap(UnconditionalBlockPlacer::new, UnconditionalBlockPlacer::place);
    private static final Codec<BlockPlacerList> LIST_PLACER =
        BlockPlacer.CODEC.listOf().xmap(BlockPlacerList::new, BlockPlacerList::place);

    private static final Codec<UniformFloat> RANGE_FLOAT =
        FloatRange.CODEC.xmap(r -> UniformFloat.of(r.min(), r.max()), f -> FloatRange.of(f.getMinValue(), f.getMaxValue()));

    private static final Codec<UniformInt> RANGE_INT =
        Range.CODEC.xmap(r -> UniformInt.of(r.min(), r.max()), i -> Range.of(i.getMinValue(), i.getMaxValue()));

    private static final Codec<TagMatchTest> TAG_TEST =
        TagKey.hashedCodec(Registries.BLOCK).xmap(TagMatchTest::new, Pattern.get(TagMatchTestAccessor::getTag));
    private static final Codec<BlockMatchTest> BLOCK_TEST =
        BuiltInRegistries.BLOCK.byNameCodec().xmap(BlockMatchTest::new, Pattern.get(BlockMatchTestAccessor::getBlock));
    private static final Codec<BlockStateMatchTest> STATE_TEST =
        BlockState.CODEC.xmap(BlockStateMatchTest::new, Pattern.get(BlockStateMatchTestAccessor::getBlockState));
    private static final Codec<HeterogeneousListRuleTest> LIST_TEST =
        IdList.listCodec(Registries.BLOCK).xmap(HeterogeneousListRuleTest::new, Pattern.get((HeterogeneousListRuleTest t) -> t.list));

    private static final Codec<ChanceChunkFilter> NUMBER_FILTER =
        Codec.doubleRange(0, 1).xmap(ChanceChunkFilter::new, ChanceChunkFilter::chance);
    private static final Codec<UnionChunkFilter> LIST_FILTER =
        ChunkFilter.CODEC.listOf().xmap(UnionChunkFilter::new, UnionChunkFilter::filters);

    private static final Codec<ConstantWeight> NUMBER_WEIGHT =
        Codec.DOUBLE.xmap(ConstantWeight::new, ConstantWeight::weight);
    private static final Codec<WeightList> LIST_WEIGHT =
        WeightFunction.CODEC.listOf().xmap(WeightList::new, WeightList::weights);
    private static final Codec<WeightFunction> RESOURCE_KEY_WEIGHT =
        ResourceKey.codec(PgRegistries.Keys.WEIGHT_TYPE).flatXmap(DefaultCodecPatterns::parseWithoutArgs, DefaultCodecPatterns::encodeAsKey);

    private static final Codec<VerticalAnchor.Absolute> NUMBER_ANCHOR =
        Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).xmap(VerticalAnchor.Absolute::new, VerticalAnchor.Absolute::y);
    private static final Codec<VerticalAnchor> TYPE_ANCHOR =
        AnchorType.CODEC.flatComapMap(t -> t.at(0), AnchorType::zeroOffsetType);

    private static final TestPattern MAP_WITH_RANGE_TEST =
        TestPattern.forMap(m -> m.has("range"));
    private static final TestPattern OFFSET_PARAMS_TEST =
        TestPattern.forMap(m -> Stream.of(AnchorType.values()).anyMatch(t -> m.has(t.fieldName(), TestPattern.LIST)));
    private static final TestPattern PARAMS_ONLY_TEST =
        TestPattern.LIST.or(TestPattern.STRING).or(TestPattern.NUMBER);

    private static final Codec<ConditionSource> LIST_CONDITION =
        ConditionSource.CODEC.listOf(1, 256).xmap(DefaultCodecPatterns::conditionList, Pattern.get(AllConditionSource::list));
    private static final Codec<ConditionSource> RESOURCE_KEY_CONDITION =
        ResourceKey.codec(Registries.MATERIAL_CONDITION).flatXmap(DefaultCodecPatterns::parseWithoutArgs, DefaultCodecPatterns::encodeAsKey);

    private static final Codec<RuleSource> LIST_RULE =
        RuleSource.CODEC.listOf(1, 256).xmap(DefaultCodecPatterns::ruleList, Pattern.get(SequenceRuleSource::sequence));
    private static final Codec<BlockRuleSource> STATE_RULE =
        BlockState.CODEC.xmap(BlockRuleSource::new, BlockRuleSource::resultState);

    public static final List<Pattern<? extends BlockPlacer>> PLACER = List.of(
        Pattern.of(STATE_PLACER, UnconditionalBlockPlacer.class).testing(TestPattern.ID.or(TestPattern.STATE)),
        Pattern.of(LIST_PLACER, BlockPlacerList.class).testing(TestPattern.LIST)
    );

    public static final List<Pattern<? extends FloatProvider>> FLOAT = List.of(
        Pattern.of(RANGE_FLOAT, UniformFloat.class).testing(TestPattern.NUMBER_LIST)
    );

    public static final List<Pattern<? extends IntProvider>> INT = List.of(
        Pattern.of(RANGE_INT, UniformInt.class).testing(TestPattern.NUMBER_LIST)
    );

    public static final List<Pattern<? extends RuleTest>> RULE_TEST = List.of(
        Pattern.of(TAG_TEST, TagMatchTest.class).testing(TestPattern.matching("#.*")),
        Pattern.of(BLOCK_TEST, BlockMatchTest.class).testing(TestPattern.ID),
        Pattern.of(STATE_TEST, BlockStateMatchTest.class).testing(TestPattern.STATE),
        Pattern.of(LIST_TEST, HeterogeneousListRuleTest.class).testing(TestPattern.STRING_LIST)
    );

    public static final List<Pattern<? extends ChunkFilter>> CHUNK_FILTER = List.of(
        Pattern.of(NUMBER_FILTER, ChanceChunkFilter.class).testing(TestPattern.NUMBER),
        Pattern.of(LIST_FILTER, UnionChunkFilter.class).testing(TestPattern.LIST)
    );

    public static final List<Pattern<? extends WeightFunction>> WEIGHT = List.of(
        Pattern.of(NUMBER_WEIGHT, ConstantWeight.class).testing(TestPattern.NUMBER),
        Pattern.of(LIST_WEIGHT, WeightList.class).testing(TestPattern.LIST),
        Pattern.of(RESOURCE_KEY_WEIGHT, weight -> false).testing(TestPattern.ID) // cannot encode
    );

    public static final List<Pattern<? extends VerticalAnchor>> ANCHOR = List.of(
        Pattern.of(NUMBER_ANCHOR, VerticalAnchor.Absolute.class).testing(TestPattern.NUMBER),
        Pattern.of(TYPE_ANCHOR, a -> AnchorType.zeroOffsetType(a).isSuccess()).testing(TestPattern.STRING)
    );

    public static final List<Pattern<? extends HeightProvider>> HEIGHT = List.of(
        HeightPattern.MapWithRange.INFO.heightPattern().testing(MAP_WITH_RANGE_TEST),
        HeightPattern.OffsetParamsList.INFO.heightPattern().testing(TestPattern.MAP_LIST),
        HeightPattern.OffsetParams.INFO.heightPattern().testing(OFFSET_PARAMS_TEST),
        HeightPattern.ParamsOnly.INFO.heightPattern().testing(PARAMS_ONLY_TEST)
    );

    public static final List<Pattern<? extends ColumnProvider>> COLUMN = List.of(
        HeightPattern.MapWithRange.INFO.columnPattern().testing(MAP_WITH_RANGE_TEST),
        HeightPattern.OffsetParamsList.INFO.columnPattern().testing(TestPattern.MAP_LIST),
        HeightPattern.OffsetParams.INFO.columnPattern().testing(OFFSET_PARAMS_TEST),
        HeightPattern.ParamsOnly.INFO.columnPattern().testing(PARAMS_ONLY_TEST)
    );

    public static final List<Pattern<? extends ConditionSource>> CONDITION_SOURCE = List.of(
        Pattern.of(LIST_CONDITION, AllConditionSource.class).testing(TestPattern.LIST),
        Pattern.of(RESOURCE_KEY_CONDITION, condition -> false).testing(TestPattern.ID)
    );

    public static final List<Pattern<? extends RuleSource>> RULE_SOURCE = List.of(
        Pattern.of(LIST_RULE, SequenceRuleSource.class).testing(TestPattern.LIST),
        Pattern.of(STATE_RULE, BlockRuleSource.class).testing(TestPattern.STATE)
    );

    private DefaultCodecPatterns() {}

    private static <T> DataResult<T> parseWithoutArgs(ResourceKey<MapCodec<? extends T>> key) {
        final var registry = RegistryUtils.getHandle(key.registryKey());
        final var codec = registry.lookup(key);
        if (codec == null) {
            return DataResult.error(() -> "No such weight type: " + key.location());
        }
        return codec.compressedDecode(JavaOps.INSTANCE, Map.of())
            .map(t -> (T) t)
            .mapError(e -> "Cannot decode " + key.location() + " without additional properties");
    }

    private static <T> DataResult<ResourceKey<MapCodec<? extends T>>> encodeAsKey(T t) {
        return DataResult.error(() -> "Not supported: encoding value as resource key");
    }

    private static ConditionSource conditionList(List<ConditionSource> list) {
        return list.size() == 1 ? list.getFirst() : new AllConditionSource(list);
    }

    private static RuleSource ruleList(List<RuleSource> list) {
        return list.size() == 1 ? list.getFirst() : new SequenceRuleSource(list);
    }
}
