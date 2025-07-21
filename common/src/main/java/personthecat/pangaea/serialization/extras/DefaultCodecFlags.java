package personthecat.pangaea.serialization.extras;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.DensityFunctions.Clamp;
import net.minecraft.world.level.levelgen.DensityFunctions.BlendDensity;
import net.minecraft.world.level.levelgen.DensityFunctions.Marker;
import personthecat.catlib.data.FloatRange;
import personthecat.pangaea.serialization.codec.BuilderCodec.BuilderField;

import java.util.ArrayList;
import java.util.List;

import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;

public final class DefaultCodecFlags {
    private static final Codec<List<CacheType>> CACHE_CODEC = easyList(ofEnum(CacheType.class));

    public static final List<BuilderField<DensityFunction, ?>> DENSITY = List.of(
        BuilderField.of(DensityFunction.class, DefaultCodecFlags::isInterpolated)
            .parsing(Codec.BOOL, "interpolate")
            .wrap((interpolate, next) -> interpolate ? DensityFunctions.interpolated(next) : next)
            .unwrap(Marker::wrapped, marker -> marker.type() == Marker.Type.Interpolated),
        BuilderField.of(DensityFunction.class, BlendDensity.class)
            .parsing(Codec.BOOL, "blend")
            .wrap((blend, next) -> blend ? DensityFunctions.blendDensity(next) : next)
            .unwrap(BlendDensity::input, b -> true),
        BuilderField.of(DensityFunction.class, DefaultCodecFlags::isCached)
            .parsing(CACHE_CODEC, "cache")
            .wrap(DefaultCodecFlags::applyCache)
            .unwrap(Marker::wrapped, DefaultCodecFlags::getCache),
        BuilderField.of(DensityFunction.class, Clamp.class)
            .parsing(FloatRange.CODEC, "clamp")
            .wrap((clamp, next) -> next.clamp(clamp.min(), clamp.max()))
            .unwrap(Clamp::input, c -> FloatRange.of((float) c.minValue(), (float) c.maxValue()))
    );

    private DefaultCodecFlags() {}

    private static boolean isInterpolated(DensityFunction f) {
        return f instanceof Marker m && m.type() == Marker.Type.Interpolated;
    }

    private static boolean isCached(DensityFunction f) {
        return f instanceof Marker m && m.type() != Marker.Type.Interpolated;
    }

    private static DensityFunction applyCache(List<CacheType> types, DensityFunction f) {
        for (final var type : types) {
            f = switch (type) {
                case FLAT_CACHE -> DensityFunctions.flatCache(f);
                case CACHE_2D -> DensityFunctions.cache2d(f);
                case CACHE_ONCE -> DensityFunctions.cacheOnce(f);
                case CACHE_ALL_IN_CELL -> DensityFunctions.cacheAllInCell(f);
            };
        }
        return f;
    }

    private static List<CacheType> getCache(DensityFunction f) {
        final var list = new ArrayList<CacheType>();
        while (f instanceof DensityFunctions.MarkerOrMarked m) {
            switch (m.type()) {
                case FlatCache -> list.add(CacheType.FLAT_CACHE);
                case Cache2D -> list.add(CacheType.CACHE_2D);
                case CacheOnce -> list.add(CacheType.CACHE_ONCE);
                case CacheAllInCell -> list.add(CacheType.CACHE_ALL_IN_CELL);
                case Interpolated -> {
                    return list;
                }
            }
            f = m.wrapped();
        }
        return list;
    }

    private enum CacheType {
        FLAT_CACHE,
        CACHE_2D,
        CACHE_ONCE,
        CACHE_ALL_IN_CELL;
    }
}
