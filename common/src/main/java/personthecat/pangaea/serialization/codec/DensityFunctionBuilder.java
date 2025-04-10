package personthecat.pangaea.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import personthecat.catlib.data.FloatRange;
import personthecat.catlib.serialization.codec.UnionCodec;
import personthecat.pangaea.config.Cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.asMapCodec;
import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.nullable;

public final class DensityFunctionBuilder {
    private static final Codec<List<CacheType>> CACHE_CODEC = easyList(ofEnum(CacheType.class));
    private static final MapCodec<DensityFunctionBuilder> CODEC = codecOf(
        defaulted(Codec.BOOL, "interpolate", false, b -> b.interpolate),
        defaulted(Codec.BOOL, "blend", false, b -> b.blend),
        nullable(CACHE_CODEC, "cache", b -> b.cache),
        nullable(FloatRange.CODEC, "clamp", b -> b.clamp),
        DensityFunctionBuilder::new
    );

    private DensityFunction wrapped;
    private boolean interpolate;
    private boolean blend;
    private List<CacheType> cache;
    private FloatRange clamp;

    public DensityFunctionBuilder() {
        this(false, false, null, null);
    }

    private DensityFunctionBuilder(boolean interpolate, boolean blend, List<CacheType> cache, FloatRange clamp) {
        this.interpolate = interpolate;
        this.blend = blend;
        this.cache = cache;
        this.clamp = clamp;
    }

    public static Codec<DensityFunction> wrap(Codec<DensityFunction> codec) {
        return UnionCodec.builder(CODEC, asMapCodec(codec))
            .create(DensityFunctionBuilder::split, DensityFunctionBuilder::combine)
            .reduceError(builder -> DataResult.error(builder.messageSupplier()), Function.identity())
            .codec();
    }

    private static DataResult<Pair<DensityFunctionBuilder, DensityFunction>> split(DensityFunction f) {
        final DensityFunctionBuilder builder = new DensityFunctionBuilder();
        if (!Cfg.encodeDensityBuilders()) {
            return DataResult.success(Pair.of(builder, f));
        }
        final List<CacheType> cacheTypes = new ArrayList<>();
        while (true) {
            if (f instanceof DensityFunctions.MarkerOrMarked m) {
                switch (m.type()) {
                    case Interpolated -> builder.interpolate(true);
                    case FlatCache -> cacheTypes.add(CacheType.FLAT_CACHE);
                    case Cache2D -> cacheTypes.add(CacheType.CACHE_2D);
                    case CacheOnce -> cacheTypes.add(CacheType.CACHE_ONCE);
                    case CacheAllInCell -> cacheTypes.add(CacheType.CACHE_ALL_IN_CELL);
                }
                f = m.wrapped();
            } else if (f instanceof DensityFunctions.BlendDensity b) {
                builder.blend(true);
                f = b.input();
            } else if (f instanceof DensityFunctions.Clamp c) {
                builder.clamp((float) c.minValue(), (float) c.maxValue());
                f = c.input();
            } else if (f instanceof DensityFunctions.HolderHolder h) {
                f = h.function().value();
            } else {
                break;
            }
        }
        if (!cacheTypes.isEmpty()) {
            builder.cache(cacheTypes.reversed());
        }
        if (f == null) {
            return DataResult.error(() -> "Cannot encode null element from wrapper");
        }
        return DataResult.success(Pair.of(builder, f));
    }

    private static DataResult<DensityFunction> combine(Pair<DensityFunctionBuilder, DensityFunction> p) {
        return DataResult.success(p.getFirst().wrap(p.getSecond()).build());
    }

    public void interpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }

    public void blend(boolean blend) {
        this.blend = blend;
    }

    public void cache(List<CacheType> cache) {
        this.cache = cache;
    }

    public void clamp(float min, float max) {
        this.clamp = new FloatRange(min, max);
    }

    public DensityFunctionBuilder wrap(DensityFunction f) {
        this.wrapped = f;
        return this;
    }

    public DensityFunction build() {
        DensityFunction f = Objects.requireNonNull(this.wrapped);
        if (this.clamp != null) f = f.clamp(this.clamp.min(), this.clamp.max());
        if (this.blend) f = DensityFunctions.blendDensity(f);
        if (this.interpolate) f = DensityFunctions.interpolated(f);
        if (this.cache != null) {
            for (final var c : this.cache) {
                switch (c) {
                    case FLAT_CACHE -> f = DensityFunctions.flatCache(f);
                    case CACHE_2D -> f = DensityFunctions.cache2d(f);
                    case CACHE_ONCE -> f = DensityFunctions.cacheOnce(f);
                    case CACHE_ALL_IN_CELL -> f = DensityFunctions.cacheAllInCell(f);
                }
            }
        }
        return f;
    }

    public enum CacheType {
        FLAT_CACHE,
        CACHE_2D,
        CACHE_ONCE,
        CACHE_ALL_IN_CELL
    }
}
