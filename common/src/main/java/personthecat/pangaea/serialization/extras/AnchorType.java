package personthecat.pangaea.serialization.extras;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import personthecat.catlib.serialization.codec.CodecUtils;
import personthecat.catlib.serialization.codec.SimpleAnyCodec;
import personthecat.pangaea.world.provider.MiddleVerticalAnchor;
import personthecat.pangaea.world.provider.SeaLevelVerticalAnchor;
import personthecat.pangaea.world.provider.SurfaceVerticalAnchor;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum AnchorType {
    ABSOLUTE(-1),
    BOTTOM(0),
    ZERO(1),
    SURFACE(2),
    SEA_LEVEL(2),
    MIDDLE(2),
    TOP(3);

    public static final Codec<AnchorType> CODEC = CodecUtils.ofEnum(AnchorType.class);

    private final int rank;

    AnchorType(int rank) {
        this.rank = rank;
    }

    public String fieldName() {
        return this.name().toLowerCase();
    }

    public boolean isAbove(AnchorType rhs) {
        return this.rank != -1 && rhs.rank != -1 && this.rank > rhs.rank;
    }

    public VerticalAnchor at(int offset) {
        return switch (this) {
            case BOTTOM -> VerticalAnchor.aboveBottom(offset);
            case TOP -> VerticalAnchor.belowTop(-offset);
            case ABSOLUTE, ZERO -> VerticalAnchor.absolute(offset);
            case SURFACE -> new SurfaceVerticalAnchor(offset);
            case SEA_LEVEL -> new SeaLevelVerticalAnchor(offset);
            case MIDDLE -> new MiddleVerticalAnchor(offset);
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> Codec<T> dispatchCodec(Function<AnchorType, Codec<T>> single, Function<T, AnchorType> type) {
        final var map = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(Function.identity(), single));
        final var values = new ArrayList<>(map.values());
        return new SimpleAnyCodec<T>(values.removeFirst(), values.toArray(Decoder[]::new))
            .withEncoder(t -> map.get(type.apply(t)));
    }

    public static DataResult<AnchorType> zeroOffsetType(VerticalAnchor a) {
        return OffsetValue.from(a)
            .flatMap(v -> v.value() == 0 ? DataResult.success(v.type()) : DataResult.error(() -> "Not zero-offset: " + a));
    }
}
