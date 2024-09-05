package personthecat.pangaea.world.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.SimpleFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.DensityFunctions.TwoArgumentSimpleFunction;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public abstract class DensityList implements SimpleFunction {
    protected static final double MAX_REASONABLE = 1000000.0;
    protected static final DensityFunction DEFAULT_MIN = DensityFunctions.constant(1);
    protected static final DensityFunction DEFAULT_MAX = DensityFunctions.constant(-1);
    protected static final DensityFunction DEFAULT_SUM = DensityFunctions.zero();
    protected final List<DensityFunction> list;
    protected final double target;
    protected final double minValue;
    protected final double maxValue;
    protected final BoundsPredicate bounds;

    protected DensityList(List<DensityFunction> list, double target) {
        this.list = list;
        this.target = target;
        this.minValue = this.calculateMin(list);
        this.maxValue = this.calculateMax(list);
        this.bounds = isReasonable(MAX_REASONABLE, target)
            ? (idx, curr) -> this.isTarget(curr) : this.calculateBounds(list);
    }

    public static DensityFunction min(List<DensityFunction> list, double target) {
        return min(list, target, DEFAULT_MIN);
    }

    public static DensityFunction min(List<DensityFunction> list, double target, DensityFunction ifEmpty) {
        return apply(list, DensityFunctions::min, ifEmpty).orElseGet(() -> new Min(list, target));
    }

    public static DensityFunction max(List<DensityFunction> list, double target) {
        return max(list, target, DEFAULT_MAX);
    }

    public static DensityFunction max(List<DensityFunction> list, double target, DensityFunction ifEmpty) {
        return apply(list, DensityFunctions::max, ifEmpty).orElseGet(() -> new Max(list, target));
    }

    public static DensityFunction sum(List<DensityFunction> list, double target) {
        return sum(list, target, DEFAULT_SUM);
    }

    public static DensityFunction sum(List<DensityFunction> list, double target, DensityFunction ifEmpty) {
        return apply(list, DensityFunctions::add, ifEmpty).orElseGet(() -> new Sum(list, target));
    }

    protected static Optional<DensityFunction> apply(
            List<DensityFunction> list,
            BiFunction<DensityFunction, DensityFunction, DensityFunction> f,
            DensityFunction ifEmpty) {
        if (list.isEmpty()) return Optional.of(ifEmpty);
        if (list.size() == 1) return Optional.of(unwrapHolder(list.getFirst()));
        if (list.size() == 2) return Optional.of(f.apply(list.getFirst(), list.get(1)));
        return Optional.empty();
    }

    // prevent functions from getting doubly-wrapped
    private static DensityFunction unwrapHolder(DensityFunction f) {
        return f instanceof DensityFunctions.HolderHolder h ? h.function().value() : f;
    }

    protected static boolean isReasonable(double max, double d) {
        return d > -max && d < max;
    }

    protected abstract double calculateMin(List<DensityFunction> list);
    protected abstract double calculateMax(List<DensityFunction> list);
    protected abstract BoundsPredicate calculateBounds(List<DensityFunction> list);
    protected abstract boolean isTarget(double d);

    @Override
    public double minValue() {
        return this.minValue;
    }

    @Override
    public double maxValue() {
        return this.maxValue;
    }

    @Override
    public @NotNull abstract KeyDispatchDataCodec<? extends DensityList> codec();

    public static class Min extends DensityList {
        public static final MapCodec<Min> CODEC = codecOf(
            field(easyList(DensityFunction.HOLDER_HELPER_CODEC), "min", l -> l.list),
            defaulted(Codec.DOUBLE, "target", MAX_REASONABLE, l -> l.target),
            Min::new
        );
        public static final Codec<DensityFunction> LIST_CODEC =
            easyList(DensityFunction.HOLDER_HELPER_CODEC)
                .xmap(l -> min(l, MAX_REASONABLE), Min::unwrapMinList);
        public static final MapDecoder<DensityFunction> OPTIMIZED_DECODER =
            CODEC.map(l -> min(l.list, l.target));

        public Min(List<DensityFunction> list, double target) {
            super(list, target);
        }

        public static Min from(TwoArgumentSimpleFunction twoArg) {
            return new Min(List.of(twoArg.argument1(), twoArg.argument2()), MAX_REASONABLE);
        }

        private static List<DensityFunction> unwrapMinList(DensityFunction f) {
            if (f == DensityFunctions.zero()) {
                return List.of();
            } else if (f instanceof TwoArgumentSimpleFunction twoArg
                    && twoArg.type() == TwoArgumentSimpleFunction.Type.MIN) {
                return List.of(twoArg.argument1(), twoArg.argument2());
            } else if (f instanceof Min min) {
                return min.list;
            }
            return List.of(f);
        }

        @Override
        public double compute(FunctionContext ctx) {
            double min = MAX_REASONABLE;
            for (int i = 0; i < this.list.size(); i++) {
                final DensityFunction f = this.list.get(i);
                final double d = f.compute(ctx);
                if (d < min) min = d;
                if (this.bounds.test(i, min)) break;
            }
            return min;
        }

        @Override
        protected double calculateMin(List<DensityFunction> list) {
            return list.stream().mapToDouble(DensityFunction::minValue).min().orElse(-MAX_REASONABLE);
        }

        @Override
        protected double calculateMax(List<DensityFunction> list) {
            return list.stream().mapToDouble(DensityFunction::maxValue).min().orElse(MAX_REASONABLE);
        }

        @Override
        protected BoundsPredicate calculateBounds(List<DensityFunction> list) {
            final DoubleList bounds = new DoubleArrayList(list.size());
            for (int i = 0; i < list.size(); i++) {
                double min = Double.MAX_VALUE;
                for (int j = i; j < list.size(); j++) {
                    min = Math.min(min, list.get(j).minValue());
                }
                bounds.add(min);
            }
            return (idx, curr) -> curr < bounds.getDouble(idx);
        }

        @Override
        protected boolean isTarget(double d) {
            return d <= this.target;
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return new Min(this.list.stream().map(f -> f.mapAll(visitor)).toList(), this.target);
        }

        @Override
        public @NotNull KeyDispatchDataCodec<Min> codec() {
           return KeyDispatchDataCodec.of(CODEC);
        }
    }

    public static class Max extends DensityList {
        public static final MapCodec<Max> CODEC = codecOf(
            field(easyList(HOLDER_HELPER_CODEC), "max", l -> l.list),
            defaulted(Codec.DOUBLE, "target", -MAX_REASONABLE, l -> l.target),
            Max::new
        );
        public static final Codec<DensityFunction> LIST_CODEC =
            easyList(DensityFunction.HOLDER_HELPER_CODEC)
                .xmap(l -> max(l, -MAX_REASONABLE), Max::unwrapMaxList);
        public static final MapDecoder<DensityFunction> OPTIMIZED_DECODER =
            CODEC.map(l -> max(l.list, l.target));

        public Max(List<DensityFunction> list, double target) {
            super(list, target);
        }

        public static Max from(TwoArgumentSimpleFunction twoArg) {
            return new Max(List.of(twoArg.argument1(), twoArg.argument2()), -MAX_REASONABLE);
        }

        private static List<DensityFunction> unwrapMaxList(DensityFunction f) {
            if (f == DensityFunctions.zero()) {
                return List.of();
            } else if (f instanceof TwoArgumentSimpleFunction twoArg
                    && twoArg.type() == TwoArgumentSimpleFunction.Type.MAX) {
                return List.of(twoArg.argument1(), twoArg.argument2());
            } else if (f instanceof Max max) {
                return max.list;
            }
            return List.of(f);
        }

        @Override
        public double compute(FunctionContext ctx) {
            double max = -MAX_REASONABLE;
            for (int i = 0; i < this.list.size(); i++) {
                final DensityFunction f = this.list.get(i);
                final double d = f.compute(ctx);
                if (d > max) max = d;
                if (this.bounds.test(i, max)) break;
            }
            return max;
        }

        @Override
        protected double calculateMin(List<DensityFunction> list) {
            return list.stream().mapToDouble(DensityFunction::minValue).max().orElse(-MAX_REASONABLE);
        }

        @Override
        protected double calculateMax(List<DensityFunction> list) {
            return list.stream().mapToDouble(DensityFunction::maxValue).max().orElse(MAX_REASONABLE);
        }

        @Override
        protected BoundsPredicate calculateBounds(List<DensityFunction> list) {
            final DoubleList bounds = new DoubleArrayList(list.size());
            for (int i = 0; i < list.size(); i++) {
                double max = -Double.MAX_VALUE;
                for (int j = i; j < list.size(); j++) {
                    max = Math.max(max, list.get(j).maxValue());
                }
                bounds.add(max);
            }
            return (idx, curr) -> curr > bounds.getDouble(idx);
        }

        @Override
        protected boolean isTarget(double d) {
            return d >= this.target;
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return new Max(this.list.stream().map(f -> f.mapAll(visitor)).toList(), this.target);
        }

        @Override
        public @NotNull KeyDispatchDataCodec<Max> codec() {
            return KeyDispatchDataCodec.of(CODEC);
        }
    }

    public static class Sum extends DensityList {
        public static final MapCodec<Sum> CODEC = codecOf(
            field(easyList(DensityFunction.HOLDER_HELPER_CODEC), "sum", l -> l.list),
            defaulted(Codec.DOUBLE, "target", MAX_REASONABLE / 2, l -> l.target),
            Sum::new
        );
        public static final MapDecoder<DensityFunction> OPTIMIZED_DECODER =
            CODEC.map(l -> sum(l.list, l.target));

        public Sum(List<DensityFunction> list, double target) {
            super(list, target);
        }

        @Override
        public double compute(FunctionContext ctx) {
            double sum = 0;
            for (int i = 0; i < this.list.size(); i++) {
                final DensityFunction f = this.list.get(i);
                final double d = f.compute(ctx);
                sum += d;
                if (this.bounds.test(i, sum)) break;
            }
            return sum;
        }

        @Override
        protected double calculateMin(List<DensityFunction> list) {
            return list.stream().mapToDouble(DensityFunction::minValue).sum();
        }

        @Override
        protected double calculateMax(List<DensityFunction> list) {
            return list.stream().mapToDouble(DensityFunction::maxValue).sum();
        }

        @Override
        protected BoundsPredicate calculateBounds(List<DensityFunction> list) {
            return (idx, curr) -> this.isTarget(curr);
        }

        @Override
        protected boolean isTarget(double d) {
            return !isReasonable(this.target, d);
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return new Sum(this.list.stream().map(f -> f.mapAll(visitor)).toList(), this.target);
        }

        @Override
        public @NotNull KeyDispatchDataCodec<Sum> codec() {
            return KeyDispatchDataCodec.of(CODEC);
        }
    }

    @FunctionalInterface
    public interface BoundsPredicate {
        boolean test(int idx, double curr);
    }
}
