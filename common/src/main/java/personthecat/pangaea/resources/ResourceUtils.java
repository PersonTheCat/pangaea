package personthecat.pangaea.resources;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import lombok.extern.log4j.Log4j2;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.IoSupplier;
import personthecat.catlib.serialization.codec.CodecSupport;
import personthecat.catlib.serialization.codec.XjsOps;
import personthecat.catlib.util.PathUtils;
import xjs.data.serialization.JsonContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;

@Log4j2
public final class ResourceUtils {
    private ResourceUtils() {}

    public static Dynamic<?> loadDynamic(String path, IoSupplier<InputStream> data) {
        try {
            final var json = JsonContext.getParser(PathUtils.extension(path)).parse(data.get());
            return new Dynamic<>(XjsOps.INSTANCE, json);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static <T> InputStream streamAsJson(ResourceKey<T> key, T value) throws IOException {
        log.warn("Streaming literal resource as JSON: {}", value);
        final var codec = CodecSupport.tryGetCodec(key.registryKey());
        if (codec == null) throw new IOException("No codec available to stream resource: " + value);
        final var data = codec.encodeStart(JsonOps.INSTANCE, value);
        log.error("Error streaming literal resource: {}", data.error().orElseThrow());
        if (data.isError()) throw new IOException("Cannot stream literal resource: " + value);
        return streamJson(data.getOrThrow());
    }

    static InputStream streamAsJson(Dynamic<?> data) throws IOException {
        log.warn("Streaming dynamic resource as JSON: {}", data);
        return streamJson(data.convert(JsonOps.INSTANCE).getValue());
    }

    static InputStream streamJson(JsonElement json) throws IOException {
        final var stream = new ByteArrayStream();
        final var writer = new JsonWriter(new OutputStreamWriter(stream));
        writer.setLenient(true);
        writer.setIndent("  ");
        Streams.write(json, writer);
        writer.close();
        return stream.asInputStream();
    }
}
