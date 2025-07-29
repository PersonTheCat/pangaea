package personthecat.pangaea.serialization.extras;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions.Mapped;
import net.minecraft.world.level.levelgen.DensityFunctions.Noise;
import net.minecraft.world.level.levelgen.DensityFunctions.ShiftedNoise;
import net.minecraft.world.level.levelgen.DensityFunctions.TwoArgumentSimpleFunction;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import personthecat.pangaea.serialization.codec.DensityHelper;
import personthecat.pangaea.serialization.codec.NoiseCodecs;
import personthecat.pangaea.serialization.codec.StructuralCodec.Structure;
import personthecat.pangaea.serialization.codec.TestPattern;
import personthecat.pangaea.world.density.DensityList.Min;
import personthecat.pangaea.world.density.DensityList.Max;
import personthecat.pangaea.world.density.DensityList.Sum;
import personthecat.pangaea.world.density.FastNoiseDensity;
import personthecat.pangaea.world.filter.ChanceChunkFilter;
import personthecat.pangaea.world.filter.ChunkFilter;
import personthecat.pangaea.world.filter.ClusterChunkFilter;
import personthecat.pangaea.world.filter.DensityChunkFilter;
import personthecat.pangaea.world.filter.FastNoiseChunkFilter;
import personthecat.pangaea.world.filter.IntervalChunkFilter;
import personthecat.pangaea.world.filter.SpawnDistanceChunkFilter;
import personthecat.pangaea.world.provider.DensityFloatProvider;
import personthecat.pangaea.world.provider.DensityHeightProvider;
import personthecat.pangaea.world.provider.DensityIntProvider;
import personthecat.pangaea.world.weight.CutoffWeight;
import personthecat.pangaea.world.weight.DensityWeight;
import personthecat.pangaea.world.weight.MultipleWeight;
import personthecat.pangaea.world.weight.SumWeight;
import personthecat.pangaea.world.weight.WeightFunction;

import java.util.List;
import java.util.function.Predicate;

import static personthecat.catlib.serialization.codec.CodecUtils.asParent;
import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public final class DefaultCodecStructures {
    private static final MapCodec<TwoArgumentSimpleFunction> MUL_TIMES_DENSITY =
        MulTimesStructure.DENSITY_CODEC.xmap(MulTimesStructure::toDensity, MulTimesStructure::fromDensity);
    private static final MapCodec<TwoArgumentSimpleFunction> ADD_PLUS_DENSITY =
        AddPlusStructure.DENSITY_CODEC.xmap(AddPlusStructure::toDensity, AddPlusStructure::fromDensity);
    private static final MapCodec<DensityFunction> MIN =
        MapCodec.of(asParent(Min.CODEC), Min.OPTIMIZED_DECODER);
    private static final MapCodec<DensityFunction> MAX =
        MapCodec.of(asParent(Max.CODEC), Max.OPTIMIZED_DECODER);
    private static final MapCodec<DensityFunction> SUM =
        MapCodec.of(asParent(Sum.CODEC), Sum.OPTIMIZED_DECODER);
    private static final MapCodec<DensityFunction> ABS =
        DensityHelper.CODEC.fieldOf("abs").xmap(DensityFunction::abs, Structure.get(Mapped::input));
    private static final MapCodec<DensityFunction> SQUARE =
        DensityHelper.CODEC.fieldOf("square").xmap(DensityFunction::square, Structure.get(Mapped::input));
    private static final MapCodec<DensityFunction> CUBE =
        DensityHelper.CODEC.fieldOf("cube").xmap(DensityFunction::cube, Structure.get(Mapped::input));
    private static final MapCodec<DensityFunction> HALF_NEGATIVE =
        DensityHelper.CODEC.fieldOf("half_negative").xmap(DensityFunction::halfNegative, Structure.get(Mapped::input));
    private static final MapCodec<DensityFunction> QUARTER_NEGATIVE =
        DensityHelper.CODEC.fieldOf("quarter_negative").xmap(DensityFunction::quarterNegative, Structure.get(Mapped::input));
    private static final MapCodec<DensityFunction> SQUEEZE =
        DensityHelper.CODEC.fieldOf("squeeze").xmap(DensityFunction::squeeze, Structure.get(Mapped::input));

    private static final TestPattern.MapPattern FAST_NOISE_TEST_PATTERN =
        m -> m.has("noise", NoiseCodecs.TYPE);

    private static final MapCodec<WeightFunction> MUL_TIMES_WEIGHT =
        MulTimesStructure.WEIGHT_CODEC.xmap(MulTimesStructure::toWeight, MulTimesStructure::fromWeight);
    private static final MapCodec<WeightFunction> ADD_PLUS_WEIGHT =
        AddPlusStructure.WEIGHT_CODEC.xmap(AddPlusStructure::toWeight, AddPlusStructure::fromWeight);

    private static final TestPattern.MapPattern CUTOFF_PATTERN =
        m -> m.has("input") && (m.has("upper") || m.has("lower"));

    private static final MapCodec<VerticalAnchor.AboveBottom> BOTTOM_ANCHOR =
        Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("bottom")
            .xmap(VerticalAnchor.AboveBottom::new, VerticalAnchor.AboveBottom::offset);
    private static final MapCodec<VerticalAnchor.BelowTop> TOP_ANCHOR =
        Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("top")
            .xmap(i -> new VerticalAnchor.BelowTop(-i), a -> -a.offset());

    public static final List<Structure<? extends DensityFunction>> DENSITY = List.of(
        Structure.of(MUL_TIMES_DENSITY, isType(TwoArgumentSimpleFunction.Type.MUL)),
        Structure.of(ADD_PLUS_DENSITY, isType(TwoArgumentSimpleFunction.Type.ADD)),
        Structure.of(MIN, Min.class).normalized(DefaultCodecStructures::normalizeMinFunction).withRequiredFields("min"),
        Structure.of(MAX, Max.class).normalized(DefaultCodecStructures::normalizeMaxFunction).withRequiredFields("max"),
        Structure.of(SUM, Sum.class).withRequiredFields("sum"),
        Structure.of(ABS, isType(Mapped.Type.ABS)),
        Structure.of(SQUARE, isType(Mapped.Type.SQUARE)),
        Structure.of(CUBE, isType(Mapped.Type.CUBE)),
        Structure.of(HALF_NEGATIVE, isType(Mapped.Type.HALF_NEGATIVE)),
        Structure.of(QUARTER_NEGATIVE, isType(Mapped.Type.QUARTER_NEGATIVE)),
        Structure.of(SQUEEZE, isType(Mapped.Type.SQUEEZE)),
        Structure.of(FastNoiseDensity.CODEC, FastNoiseDensity.class).withTestPattern(FAST_NOISE_TEST_PATTERN),
        Structure.of(ShiftedNoise.CODEC.codec(), ShiftedNoise.class).withRequiredFields("noise", "shift_x"),
        Structure.of(Noise.CODEC.codec(), Noise.class).withRequiredFields("noise")
    );

    public static final List<Structure<? extends FloatProvider>> FLOAT = List.of(
        Structure.of(DensityFloatProvider.CODEC, DensityFloatProvider.class)
    );

    public static final List<Structure<? extends IntProvider>> INT = List.of(
        Structure.of(DensityIntProvider.CODEC, DensityIntProvider.class)
    );

    public static final List<Structure<? extends ChunkFilter>> CHUNK_FILTER = List.of(
        Structure.of(DensityChunkFilter.CODEC, DensityChunkFilter.class).withRequiredFields("density"),
        Structure.of(ClusterChunkFilter.CODEC, ClusterChunkFilter.class).withRequiredFields("cluster_chance"),
        Structure.of(FastNoiseChunkFilter.CODEC, FastNoiseChunkFilter.class).withRequiredFields("noise"),
        Structure.of(IntervalChunkFilter.CODEC, IntervalChunkFilter.class).withRequiredFields("interval"),
        Structure.of(SpawnDistanceChunkFilter.CODEC, SpawnDistanceChunkFilter.class).withRequiredFields("distance"),
        Structure.of(ChanceChunkFilter.CODEC, ChanceChunkFilter.class).withRequiredFields("chance")
    );

    public static final List<Structure<? extends WeightFunction>> WEIGHT = List.of(
        Structure.of(MUL_TIMES_WEIGHT, MultipleWeight.class),
        Structure.of(ADD_PLUS_WEIGHT, SumWeight.class),
        Structure.of(DensityWeight.CODEC, DensityWeight.class).withRequiredFields("density"),
        Structure.of(CutoffWeight.CODEC, CutoffWeight.class).withTestPattern(CUTOFF_PATTERN)
    );

    public static final List<Structure<? extends VerticalAnchor>> ANCHOR = List.of(
        Structure.of(BOTTOM_ANCHOR, VerticalAnchor.AboveBottom.class),
        Structure.of(TOP_ANCHOR, VerticalAnchor.BelowTop.class)
    );

    public static final List<Structure<? extends HeightProvider>> HEIGHT = List.of(
        Structure.of(DensityHeightProvider.CODEC, DensityHeightProvider.class).withRequiredFields("density")
    );

    private DefaultCodecStructures() {}

    private static Predicate<DensityFunction> isType(TwoArgumentSimpleFunction.Type type) {
        return f -> f instanceof TwoArgumentSimpleFunction t && t.type() == type;
    }

    private static Predicate<DensityFunction> isType(Mapped.Type type) {
        return f -> f instanceof Mapped m && m.type() == type;
    }

    private static DensityFunction normalizeMinFunction(DensityFunction f) {
        return f instanceof TwoArgumentSimpleFunction t && t.type() == TwoArgumentSimpleFunction.Type.MIN
            ? Min.from(t) : f;
    }

    private static DensityFunction normalizeMaxFunction(DensityFunction f) {
        return f instanceof TwoArgumentSimpleFunction t && t.type() == TwoArgumentSimpleFunction.Type.MAX
            ? Max.from(t) : f;
    }

    private record MulTimesStructure<A>(A mul, A times) {
        static final MapCodec<MulTimesStructure<DensityFunction>> DENSITY_CODEC =
            createCodec(DensityHelper.CODEC);
        static final MapCodec<MulTimesStructure<WeightFunction>> WEIGHT_CODEC =
            createCodec(WeightFunction.CODEC);

        static TwoArgumentSimpleFunction toDensity(MulTimesStructure<DensityFunction> s) {
            return TwoArgumentSimpleFunction.create(TwoArgumentSimpleFunction.Type.MUL, s.mul, s.times);
        }

        static MulTimesStructure<DensityFunction> fromDensity(TwoArgumentSimpleFunction t) {
            return new MulTimesStructure<>(t.argument1(), t.argument2());
        }

        static WeightFunction toWeight(MulTimesStructure<WeightFunction> s) {
            return MultipleWeight.create(s.mul, s.times);
        }

        static MulTimesStructure<WeightFunction> fromWeight(WeightFunction f) {
            final var s = (MultipleWeight) f;
            return new MulTimesStructure<>(s.argument1(), s.argument2());
        }

        private static <A> MapCodec<MulTimesStructure<A>> createCodec(Codec<A> base) {
            return codecOf(
                field(base, "mul", MulTimesStructure::mul),
                field(base, "times", MulTimesStructure::times),
                MulTimesStructure::new
            );
        }
    }

    private record AddPlusStructure<A>(A add, A plus) {
        static final MapCodec<AddPlusStructure<DensityFunction>> DENSITY_CODEC =
            createCodec(DensityHelper.CODEC);
        static final MapCodec<AddPlusStructure<WeightFunction>> WEIGHT_CODEC =
            createCodec(WeightFunction.CODEC);

        static TwoArgumentSimpleFunction toDensity(AddPlusStructure<DensityFunction> s) {
            return TwoArgumentSimpleFunction.create(TwoArgumentSimpleFunction.Type.ADD, s.add, s.plus);
        }

        static AddPlusStructure<DensityFunction> fromDensity(TwoArgumentSimpleFunction t) {
            return new AddPlusStructure<>(t.argument1(), t.argument2());
        }

        static WeightFunction toWeight(AddPlusStructure<WeightFunction> s) {
            return SumWeight.create(s.add, s.plus);
        }

        static AddPlusStructure<WeightFunction> fromWeight(WeightFunction f) {
            final var s = (SumWeight) f;
            return new AddPlusStructure<>(s.argument1(), s.argument2());
        }

        private static <A> MapCodec<AddPlusStructure<A>> createCodec(Codec<A> base) {
            return codecOf(
                field(base, "add", AddPlusStructure::add),
                field(base, "plus", AddPlusStructure::plus),
                AddPlusStructure::new
            );
        }
    }
}
