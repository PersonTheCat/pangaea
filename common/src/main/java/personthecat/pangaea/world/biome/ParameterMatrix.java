package personthecat.pangaea.world.biome;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.Parameter;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.Nullable;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.data.Rectangle;
import personthecat.pangaea.util.Utils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.optionalCodec;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public record ParameterMatrix<T>(
        VariantMap<T> variants,
        List<Parameter> x,
        List<Parameter> y,
        List<List<Optional<Choice<T>>>> matrix,
        Supplier<Set<Rectangle>> rectangles) {

    public ParameterMatrix(
            VariantMap<T> variants,
            List<Parameter> x,
            List<Parameter> y,
            List<List<Optional<Choice<T>>>> matrix) {
        this(variants, x, y, matrix, buildRectangleSupplier(matrix));
    }

    public static <T> CodecBuilder<T> codecBuilder(Codec<Holder<T>> holderCodec) {
        return new CodecBuilder<>(holderCodec);
    }

    private static <T> Supplier<Set<Rectangle>> buildRectangleSupplier(List<List<T>> matrix) {
        return Cfg.optimizeBiomeLayouts() ? Suppliers.memoize(() -> Utils.findRectangles(matrix)) : Collections::emptySet;
    }

    public void forEach(TriConsumer<Parameter, Parameter, Choice.Getter<T>> fn) {
        final var rectangles = this.rectangles.get();
        for (final var rectangle : rectangles) {
            final var x = Parameter.span(this.interpolateX(rectangle.minX()), this.interpolateX(rectangle.maxX()));
            final var y = Parameter.span(this.interpolateY(rectangle.minY()), this.interpolateY(rectangle.maxY()));
            final var t = this.matrix.get(rectangle.minY()).get(rectangle.minX());
            t.ifPresent(choice -> fn.accept(x, y, choice.getter(this.variants)));
        }
        for (int y = 0; y < this.matrix.size(); y++) {
            final var row = this.matrix.get(y);
            for (int x = 0; x < row.size(); x++) {
                if (!Utils.rectanglesContainPoint(rectangles, x, y)) {
                    final var t = row.get(x);
                    if (t.isPresent()) {
                        fn.accept(this.interpolateX(x), this.interpolateY(y), t.get().getter(this.variants));
                    }
                }
            }
        }
    }

    private Parameter interpolateX(int x) {
        return interpolate(this.x, x, this.width());
    }

    private Parameter interpolateY(int y) {
        return interpolate(this.y, y, this.height());
    }

    private int width() {
        return this.matrix.isEmpty() ? 0 : this.matrix.getFirst().size();
    }

    private int height() {
        return this.matrix.size();
    }

    private static Parameter interpolate(List<Parameter> values, int idx, int actualSize) {
        if (values.size() == actualSize) {
            return values.get(idx);
        } else if (actualSize < values.size()) {
            actualSize--;
        }
        final float t1 = idx / (actualSize + 1f) * (values.size() + 1f);
        final float t2 = (idx + 1f) / (actualSize + 1f) * (values.size() + 1f);

        return Parameter.span(interpolateValueInCurve(values, t1), interpolateValueInCurve(values, t2));
    }

    private static float interpolateValueInCurve(List<Parameter> values, float t) {
        final int idx = (int) Math.floor(t);

        return Utils.lerp(getValueInCurve(values, idx), getValueInCurve(values, idx + 1), t - idx);
    }

    private static float getValueInCurve(List<Parameter> values, int idx) {
        return Climate.unquantizeCoord(idx >= values.size() ? values.getLast().max() : values.get(idx).min());
    }

    private DataResult<ParameterMatrix<T>> validate() {
        for (final var row : this.matrix) {
            for (final var col : row) {
                if (col.isPresent() && col.get() instanceof Choice.VariantName<?> name && !this.variants.isDefined(name.key())) {
                    return DataResult.error(() -> "No variant for key: " + name.key());
                }
            }
        }
        return DataResult.success(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ParameterMatrix<?> m
            && Objects.equals(this.variants, m.variants)
            && Objects.equals(this.x, m.x)
            && Objects.equals(this.y, m.y)
            && Objects.equals(this.matrix, m.matrix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.variants, this.x, this.y, this.matrix);
    }

    public static class CodecBuilder<T> {
        private static final Codec<List<Parameter>> PARAMETER_LIST_CODEC =
            Parameter.CODEC.listOf(1, Integer.MAX_VALUE);
        private static final List<Parameter> DEFAULT_AXIS = List.of(Parameter.span(0, 1));

        private final Codec<Holder<T>> holderCodec;
        private String xKey;
        private String yKey;
        private String matrixKey;
        private String variantKey;
        private List<Parameter> defaultX = DEFAULT_AXIS;
        private List<Parameter> defaultY = DEFAULT_AXIS;

        private CodecBuilder(Codec<Holder<T>> holderCodec) {
            this.holderCodec = holderCodec;
        }

        public CodecBuilder<T> withKeys(String xKey, String yKey, String matrixKey) {
            this.xKey = xKey;
            this.yKey = yKey;
            this.matrixKey = matrixKey;
            return this;
        }

        public CodecBuilder<T> withDefaultAxes(List<Parameter> x, List<Parameter> y) {
            this.defaultX = x;
            this.defaultY = y;
            return this;
        }

        public CodecBuilder<T> withVariantsNamed(String variantKey) {
            this.variantKey = variantKey;
            return this;
        }

        public MapCodec<ParameterMatrix<T>> build() {
            Objects.requireNonNull(this.xKey, "Missing keys");
            Objects.requireNonNull(this.yKey, "Missing keys");
            Objects.requireNonNull(this.matrixKey, "Missing keys");
            final var variantCodec = variantMapCodec(this.holderCodec, this.variantKey);
            final MapCodec<ParameterMatrix<T>> codec = codecOf(
                defaulted(variantCodec, "variants", VariantMap.empty(), ParameterMatrix::variants),
                defaulted(PARAMETER_LIST_CODEC, this.xKey, this.defaultX, ParameterMatrix::x),
                defaulted(PARAMETER_LIST_CODEC, this.yKey, this.defaultY, ParameterMatrix::y),
                field(matrixCodec(this.holderCodec), this.matrixKey, ParameterMatrix::matrix),
                ParameterMatrix::new
            );
            return codec.validate(ParameterMatrix::validate);
        }

        private static <T> Codec<VariantMap<T>> variantMapCodec(Codec<Holder<T>> holderCodec, @Nullable String variantKey) {
            if (variantKey == null) {
                return Codec.unit(VariantMap.<T>empty())
                    .validate(map -> DataResult.error(() -> "Variant definitions not allowed here"));
            }
            return VariantMap.createCodec(holderCodec, variantKey);
        }

        private static <T> Codec<List<List<Optional<Choice<T>>>>> matrixCodec(Codec<Holder<T>> holderCodec) {
            return optionalCodec(Choice.createCodec(holderCodec)).listOf().listOf().validate(matrix -> {
                if (matrix.isEmpty()) {
                    return DataResult.success(matrix);
                }
                final int expected = matrix.getFirst().size();
                for (int y = 1; y < matrix.size(); y++) {
                    final int row = matrix.get(y).size();
                    if (row != expected) {
                        return DataResult.error(() ->
                            String.format("Matrix rows are not proportionate: %s -> %s", expected, row));
                    }
                }
                return DataResult.success(matrix);
            });
        }
    }
}
