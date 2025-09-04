package personthecat.pangaea.resources.worldpack;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.JsonOps;
import lombok.extern.log4j.Log4j2;
import net.minecraft.FileUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.io.FileIO;
import personthecat.pangaea.resources.ResourceMap;
import personthecat.pangaea.resources.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Log4j2
public record WorldPackExtension(WorldPackConfig config, ResourceMap dynamicResources) {
    public static final List<String> ALT_FORMATS = List.of("djs", "xjs", "ubjson", "hjson");
    public static final List<String> MOD_CONFIG_FORMATS = ImmutableList.<String>builder().addAll(ALT_FORMATS).add("json").build();
    private static final PackLocationInfo LOADING_INFO =
        new PackLocationInfo("tmp", Component.literal("tmp"), PackSource.BUILT_IN, Optional.empty());

    public static @Nullable WorldPackExtension load(Path path, Pack.ResourcesSupplier supplier) {
        try (PackResources resources = supplier.openPrimary(LOADING_INFO)) {
            return loadFromResources(resources);
        } catch (final Exception e) {
            log.warn("Failed to load world pack {} extension", path.getFileName(), e);
            return null;
        }
    }

    private static @Nullable WorldPackExtension loadFromResources(PackResources resources) {
        final var configData = discoverModConfig(resources);
        if (configData == null) {
            return null;
        }
        final var dynamic = ResourceUtils.loadDynamic(configData.path, configData.data);
        final var config = WorldPackConfig.CODEC.codec().parse(dynamic).getOrThrow();
        return new WorldPackExtension(config, new ResourceMap());
    }

    private static @Nullable NamedResource discoverModConfig(PackResources resources) {
        for (final var ext : MOD_CONFIG_FORMATS) {
            final var name = WorldPackConfig.FILENAME + "." + ext;
            final var data = resources.getRootResource(name);
            if (data != null) {
                return new NamedResource(name, data);
            }
        }
        return null;
    }

    public void export(Path path, boolean replaceExisting) throws IOException {
        checkDirectory(path, replaceExisting);
        final var config = WorldPackConfig.CODEC.codec().encodeStart(JsonOps.INSTANCE, this.config).getOrThrow();
        writeStream(ResourceUtils.streamJson(config), path.resolve(WorldPackConfig.FILENAME + ".json"));
        for (final var entry : this.dynamicResources.listByAbsolutePath().entrySet()) {
            writeStream(entry.getValue().get(), path.resolve(entry.getKey() + ".json"));
        }
    }

    private static void checkDirectory(Path path, boolean replaceExisting) {
        if (Files.exists(path)) {
            if (replaceExisting) {
                FileIO.delete(path.toFile());
            } else {
                throw new IllegalArgumentException("World pack already exists. Change directory or use replace option");
            }
        }
    }

    private static void writeStream(InputStream is, Path path) throws IOException {
        FileUtil.createDirectoriesSafe(path);
        Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
    }
    
    private record NamedResource(String path, IoSupplier<InputStream> data) {}
}
