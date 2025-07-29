package personthecat.pangaea.serialization.codec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.serialization.codec.CodecUtils;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.mapOf;

public class FunctionCodec {
    private static final String ARGUMENT_KEY = "@arg";
    private static final String DEFAULT_KEY = "default";

    private static final Decoder<Map<String, Object>> ARGUMENT_DECODER = mapOf(ExtraCodecs.JAVA);

    public static <A> Codec<A> wrap(Codec<A> codec, ResourceKey<Registry<Template<A>>> key) {
        return CodecUtils.simpleEither(codec, new Dispatcher<>(key));
    }

    public static <A> Codec<Template<A>> create(Codec<A> type) {
        return ExtraCodecs.JAVA.xmap(
            data -> new Template<>(createTemplate(data), type),
            template -> restoreData(template.template));
    }

    private static Object createTemplate(Object data) {
        if (data instanceof Map<?, ?> map) {
            if (map.get(ARGUMENT_KEY) instanceof String key) {
                return new Parameter(key, map.get(DEFAULT_KEY));
            }
        }
        return applyMapOrCollection(data, FunctionCodec::createTemplate);
    }

    private static Object restoreData(Object template) {
        if (template instanceof Parameter parameter) {
            if (parameter.defaultValue != null) {
                return ImmutableMap.of(ARGUMENT_KEY, parameter.name, DEFAULT_KEY, parameter.defaultValue);
            }
            return ImmutableMap.of(ARGUMENT_KEY, parameter.name);
        }
        return applyMapOrCollection(template, FunctionCodec::restoreData);
    }

    private static Object applyMapOrCollection(Object o, Function<Object, Object> f) {
        if (o instanceof Map<?, ?> map) {
            final var builder = ImmutableMap.builder();
            for (final var entry : map.entrySet()) {
                builder.put(entry.getKey(), f.apply(entry.getValue()));
            }
            return builder.build();
        } else if (o instanceof Collection<?> collection) {
            final var builder = ImmutableList.builder();
            for (final var element : collection) {
                builder.add(f.apply(element));
            }
            return builder.build();
        }
        return o;
    }

    public static class Template<A> implements Codec<A> {
        private final Object template;
        private final Codec<A> type;

        private Template(Object template, Codec<A> type) {
            this.template = template;
            this.type = type;
        }

        @Override
        public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
            return ARGUMENT_DECODER.decode(ops, input)
                .flatMap(pair -> applyArguments(this.template, pair.getFirst())
                    .flatMap(data -> this.type.decode(getJavaOps(ops), data)
                        .map(pair2 -> pair2.mapSecond(o -> input))));
        }

        private static DataResult<Object> applyArguments(Object template, Map<String, Object> arguments) {
            if (template instanceof Parameter parameter) {
                final var arg = arguments.getOrDefault(parameter.name, parameter.defaultValue);
                if (arg == null) {
                    return DataResult.error(() -> "Missing required argument: " + parameter.name);
                }
                return DataResult.success(arg);
            } else if (template instanceof Map<?, ?> map) {
                final var builder = ImmutableMap.builder();
                for (final var entry : map.entrySet()) {
                    final var value = applyArguments(entry.getValue(), arguments);
                    if (value.isError()) {
                        return value;
                    }
                    builder.put(entry.getKey(), value.getOrThrow());
                }
                return DataResult.success(builder.build());
            } else if (template instanceof Collection<?> collection) {
                final var builder = ImmutableList.builder();
                for (final var element : collection) {
                    final var value = applyArguments(element, arguments);
                    if (value.isError()) {
                        return value;
                    }
                    builder.add(value.getOrThrow());
                }
                return DataResult.success(builder.build());
            }
            return DataResult.success(template);
        }

        private static DynamicOps<Object> getJavaOps(DynamicOps<?> ops) {
            return ops instanceof RegistryOps<?> regOps ? regOps.withParent(JavaOps.INSTANCE) : JavaOps.INSTANCE;
        }

        @Override
        public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
            return this.type.encode(input, ops, prefix);
        }
    }

    private record Parameter(String name, @Nullable Object defaultValue) {}

    private record Dispatcher<A>(ResourceKey<Registry<Template<A>>> key) implements Decoder<A> {

        @Override
        public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
            final var mapResult = ops.getMap(input);
            if (mapResult.isSuccess()) {
                final var map = mapResult.getOrThrow();
                final var idResult = ResourceLocation.CODEC.decode(ops, map.get("type"));
                if (idResult.isSuccess()) {
                    final var id = idResult.getOrThrow().getFirst();
                    final var registry = DynamicRegistries.get(this.key);
                    if (registry == null) {
                        return DataResult.error(() -> "Function registry not available yet");
                    }
                    final var template = registry.lookup(id);
                    if (template != null) {
                        return template.decode(ops, input);
                    }
                }
            }
            return DataResult.error(() -> "Not a template");
        }
    }
}
