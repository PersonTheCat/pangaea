package personthecat.pangaea.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import net.minecraft.resources.ResourceLocation;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.world.weight.ConstantWeight;
import personthecat.pangaea.world.weight.WeightFunction;
import personthecat.pangaea.world.weight.WeightList;

import java.util.Map;

import static personthecat.catlib.serialization.codec.CodecUtils.simpleEither;

public final class PatternWeightCodec implements Codec<WeightFunction> {
    public static final Codec<WeightFunction> INSTANCE = new PatternWeightCodec();

    private PatternWeightCodec() {}

    public static Codec<WeightFunction> wrap(Codec<WeightFunction> codec) {
        return simpleEither(codec, INSTANCE)
            .withEncoder(weight -> Pattern.hasMatcherForWeight(weight) ?  INSTANCE : codec);
    }

    @Override
    public <T> DataResult<Pair<WeightFunction, T>> decode(DynamicOps<T> ops, T input) {
        for (final Pattern.Matcher m : Pattern.MATCHERS) {
            final var result = m.codec.decode(ops, input);
            if (result.isSuccess()) {
                return result;
            }
        }
        return DataResult.error(() -> "No matching pattern for input: " + input);
    }

    @Override
    public <T> DataResult<T> encode(WeightFunction input, DynamicOps<T> ops, T prefix) {
        if (Cfg.encodePatternRuleTestCodec()) {
            for (final Pattern.Matcher m : Pattern.MATCHERS) {
                if (m.type.isInstance(input)) {
                    return m.codec.encode(input, ops, prefix);
                }
            }
        }
        return DataResult.error(() -> "No matching pattern for input: " + input);
    }

    private static class Pattern {
        private static final Codec<WeightFunction> NUMBER =
            Codec.DOUBLE.xmap(ConstantWeight::new, weight -> ((ConstantWeight) weight).weight());
        private static final Codec<WeightFunction> LIST =
            WeightFunction.CODEC.listOf().xmap(WeightList::new, weight -> ((WeightList) weight).weights());
        private static final Codec<WeightFunction> RESOURCE_LOCATION =
            ResourceLocation.CODEC.flatXmap(Pattern::parseWithoutArgs, Pattern::encodeAsId);

        private static final Matcher[] MATCHERS = {
            new Matcher(NUMBER, ConstantWeight.class),
            new Matcher(LIST, WeightList.class),
            new Matcher(RESOURCE_LOCATION, Void.class),
        };

        private record Matcher(Codec<WeightFunction> codec, Class<?> type) {}

        private static boolean hasMatcherForWeight(WeightFunction weight) {
            for (final var matcher : MATCHERS) {
                if (matcher.type.isInstance(weight)) {
                    return true;
                }
            }
            return false;
        }

        private static DataResult<WeightFunction> parseWithoutArgs(ResourceLocation id) {
            final var codec = PgRegistries.WEIGHT_TYPE.lookup(id);
            if (codec == null) {
                return DataResult.error(() -> "No such weight type: " + id);
            }
            return codec.compressedDecode(JavaOps.INSTANCE, Map.of())
                .map(weight -> (WeightFunction) weight)
                .mapError(e -> "Cannot decode " + id + " without additional properties");
        }

        private static DataResult<ResourceLocation> encodeAsId(WeightFunction weight) {
            return DataResult.error(() -> "Not supported: encoding weight as resource location");
        }
    }
}
