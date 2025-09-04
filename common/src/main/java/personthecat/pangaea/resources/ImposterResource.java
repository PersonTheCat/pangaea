package personthecat.pangaea.resources;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

public sealed interface ImposterResource<T> extends IoSupplier<InputStream> {
    <O> Either<DataResult<T>, JsonElement> apply(DynamicOps<O> ops, Decoder<T> decoder);
    Object unwrap();

    static <T> ImposterResource<T> fromLiteralValue(ResourceKey<T> key, T value) {
        return new Literal<>(key, value);
    }

    static <T> ImposterResource<T> fromGeneratedData(Dynamic<?> data) {
        return fromGeneratedData(data, false);
    }

    private static <T> ImposterResource<T> fromGeneratedData(Dynamic<?> data, boolean convertToJson) {
        return new Data<>(data, convertToJson);
    }

    static <T> ImposterResource<T> fromLazyValue(ResourceKey<T> key, Supplier<T> value) {
        return new Lazy<>(Suppliers.memoize(() -> fromLiteralValue(key, value.get())));
    }

    static <T> ImposterResource<T> fromLazyData(Supplier<Dynamic<?>> data, boolean convertToJson) {
        return new Lazy<>(Suppliers.memoize(() -> fromGeneratedData(data.get(), convertToJson)));
    }

    record Literal<T>(ResourceKey<T> key, T value) implements ImposterResource<T> {
        @Override
        public <O> Either<DataResult<T>, JsonElement> apply(DynamicOps<O> ops, Decoder<T> decoder) {
            return Either.left(DataResult.success(this.value));
        }

        @Override
        public @NotNull InputStream get() throws IOException {
            return ResourceUtils.streamAsJson(this.key, this.value);
        }

        @Override
        public T unwrap() {
            return this.value;
        }
    }

    record Data<T>(Dynamic<?> data, boolean convertToJson) implements ImposterResource<T> {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <O> Either<DataResult<T>, JsonElement> apply(DynamicOps<O> ops, Decoder<T> decoder) {
            if (this.convertToJson) {
                try {
                    return Either.right(this.data.convert(JsonOps.INSTANCE).getValue());
                } catch (RuntimeException ignored) {
                    // data conversion not possible, this was most likely expected
                }
            }
            if (ops instanceof RegistryOps<O> rOps) {
                final var dOps = (DynamicOps) rOps.withParent(this.data.getOps());
                return Either.left(decoder.parse(dOps, this.data.getValue()));
            }
            return Either.left(decoder.parse(this.data));
        }

        @Override
        public @NotNull InputStream get() throws IOException {
            return ResourceUtils.streamAsJson(this.data);
        }

        @Override
        public Object unwrap() {
            return this.data.getValue();
        }
    }

    record Lazy<T>(Supplier<ImposterResource<T>> supplier) implements ImposterResource<T> {
        @Override
        public <O> Either<DataResult<T>, JsonElement> apply(DynamicOps<O> ops, Decoder<T> decoder) {
            return this.supplier.get().apply(ops, decoder);
        }

        @Override
        public @NotNull InputStream get() throws IOException {
            return this.supplier.get().get();
        }

        @Override
        public Object unwrap() {
            return this.supplier.get().unwrap();
        }
    }
}
