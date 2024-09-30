package personthecat.pangaea.world.biome;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.Parameter;
import org.apache.commons.lang3.function.TriConsumer;
import personthecat.pangaea.data.Rectangle;
import personthecat.pangaea.util.Utils;

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
        List<Parameter> x, List<Parameter> y, List<List<Optional<T>>> matrix, Supplier<Set<Rectangle>> rectangles) {

    public ParameterMatrix(List<Parameter> x, List<Parameter> y, List<List<Optional<T>>> matrix) {
        this(x, y, matrix, Suppliers.memoize(() -> Utils.findRectangles(matrix)));
    }

    public static <T> CodecBuilder<T> codecBuilder(Codec<T> elementCodec) {
        return new CodecBuilder<>(elementCodec);
    }

    public void forEach(TriConsumer<Parameter, Parameter, T> fn) {
        final var rectangles = this.rectangles.get();
        for (final var rectangle : rectangles) {
            final var x = Parameter.span(this.interpolateX(rectangle.minX()), this.interpolateX(rectangle.maxX()));
            final var y = Parameter.span(this.interpolateY(rectangle.minY()), this.interpolateY(rectangle.maxY()));
            final var t = this.matrix.get(rectangle.minY()).get(rectangle.minX());
            t.ifPresent(value -> fn.accept(x, y, value));
        }
        for (int y = 0; y < this.matrix.size(); y++) {
            final var row = this.matrix.get(y);
            for (int x = 0; x < row.size(); x++) {
                if (!Utils.rectanglesContainPoint(rectangles, x, y)) {
                    final var t = row.get(x);
                    if (t.isPresent()) {
                        fn.accept(this.interpolateX(x), this.interpolateY(y), t.get());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ParameterMatrix<?> m
            && Objects.equals(this.x, m.x)
            && Objects.equals(this.y, m.y)
            && Objects.equals(this.matrix, m.matrix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y, this.matrix);
    }

    public static class CodecBuilder<T> {
        private static final Codec<List<Parameter>> PARAMETER_LIST_CODEC =
            Parameter.CODEC.listOf(1, Integer.MAX_VALUE);
        private static final List<Parameter> DEFAULT_AXIS = List.of(Parameter.span(0, 1));

        private final Codec<T> elementCodec;
        private String xKey;
        private String yKey;
        private String matrixKey;
        private List<Parameter> defaultX = DEFAULT_AXIS;
        private List<Parameter> defaultY = DEFAULT_AXIS;

        private CodecBuilder(Codec<T> elementCodec) {
            this.elementCodec = elementCodec;
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

        public MapCodec<ParameterMatrix<T>> build() {
            Objects.requireNonNull(this.xKey, "Missing keys");
            Objects.requireNonNull(this.yKey, "Missing keys");
            Objects.requireNonNull(this.matrixKey, "Missing keys");
            return codecOf(
                defaulted(PARAMETER_LIST_CODEC, this.xKey, this.defaultX, ParameterMatrix::x),
                defaulted(PARAMETER_LIST_CODEC, this.yKey, this.defaultY, ParameterMatrix::y),
                field(matrixCodec(this.elementCodec), this.matrixKey, ParameterMatrix::matrix),
                ParameterMatrix::new
            );
        }

        private static <T> Codec<List<List<Optional<T>>>> matrixCodec(Codec<T> elementCodec) {
            return optionalCodec(elementCodec).listOf().listOf().validate(matrix -> {
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
