package personthecat.pangaea.world.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.Parameter;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.nullable;

public record ParameterMap(
        @Nullable Parameter temperature,
        @Nullable Parameter humidity,
        @Nullable Parameter continentalness,
        @Nullable Parameter erosion,
        @Nullable Parameter depth,
        @Nullable Parameter weirdness,
        @Nullable Long offset) {

    public static final ParameterMap EMPTY =
        new ParameterMap(null, null, null, null, null, null, null);
    public static final ParameterMap WHEN_WEIRD =
        new ParameterMap(null, null, null, null, null, Parameter.span(0, 1), null);
    public static final Parameter FULL_RANGE = Parameter.span(-2, 2);

    private static final Codec<Long> OFFSET_CODEC =
        Codec.floatRange(0.0F, 1.0F).xmap(Climate::quantizeCoord, Climate::unquantizeCoord);

    public static final MapCodec<ParameterMap> CODEC = codecOf(
        nullable(Parameter.CODEC, "temperature", ParameterMap::temperature),
        nullable(Parameter.CODEC, "humidity", ParameterMap::humidity),
        nullable(Parameter.CODEC, "continentalness", ParameterMap::continentalness),
        nullable(Parameter.CODEC, "erosion", ParameterMap::erosion),
        nullable(Parameter.CODEC, "depth", ParameterMap::depth),
        nullable(Parameter.CODEC, "weirdness", ParameterMap::weirdness),
        nullable(OFFSET_CODEC, "offset", ParameterMap::offset),
        ParameterMap::new
    ).validate(ParameterMap::validate);

    public static ParameterMap flatten(List<ParameterMap> maps) {
        var out = EMPTY;
        for (final var map : maps) {
            out = out.withValuesFrom(map);
        }
        return out;
    }

    public ParameterMap withValuesFrom(ParameterMap other) {
        return new ParameterMap(
            getOrElse(other.temperature, this.temperature),
            getOrElse(other.humidity, this.humidity),
            getOrElse(other.continentalness, this.continentalness),
            getOrElse(other.erosion, this.erosion),
            getOrElse(other.depth, this.depth),
            getOrElse(other.weirdness, this.weirdness),
            getOrElse(other.offset, this.offset)
        );
    }

    public ParameterPoint toPoint() {
        return new ParameterPoint(
            getOrElse(this.temperature, FULL_RANGE),
            getOrElse(this.humidity, FULL_RANGE),
            getOrElse(this.continentalness, FULL_RANGE),
            getOrElse(this.erosion, FULL_RANGE),
            getOrElse(this.depth, FULL_RANGE),
            getOrElse(this.weirdness, FULL_RANGE),
            getOrElse(this.offset, 0L)
        );
    }

    public boolean matchesPoint(ParameterPoint point) {
        return check(this.temperature(), point.temperature())
            && check(this.humidity(), point.humidity())
            && check(this.continentalness(), point.continentalness())
            && check(this.erosion(), point.erosion())
            && check(this.depth(), point.depth())
            && check(this.weirdness(), point.weirdness());
    }

    private DataResult<ParameterMap> validate() {
        if (isAnyMeaningful(
                this.temperature, this.humidity, this.continentalness, this.erosion, this.depth, this.weirdness)) {
            return DataResult.success(this);
        }
        return DataResult.error(() -> "Must define at least one parameter");
    }

    private static <T> T getOrElse(@Nullable T t, T def) {
        return t != null ? t : def;
    }

    private static boolean isAnyMeaningful(Parameter... params) {
        for (final var param : params) {
            if (isMeaningful(param)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMeaningful(Parameter param) {
        return param != null && !param.equals(FULL_RANGE);
    }

    private static boolean check(@Nullable Parameter condition, Parameter actual) {
        return condition == null || actual.min() >= condition.min() && actual.max() <= condition.max();
    }
}
