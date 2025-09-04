package personthecat.pangaea.resources.worldpack;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.FileUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.linkfs.LinkFileSystem;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import org.jetbrains.annotations.Nullable;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.resources.builtin.BuiltInWorldPack;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public class WorldPackRepositorySource implements RepositorySource {
    public static final WorldPackRepositorySource INSTANCE = new WorldPackRepositorySource();
    public static final Path FOLDER = Pangaea.MOD.configFolder().toPath().resolve("packs");

    private WorldPackRepositorySource() {}

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        if (Cfg.enableBuiltInPack()) {
            consumer.accept(BuiltInWorldPack.buildFromConfig());
        }
        try {
            FileUtil.createDirectoriesSafe(FOLDER);
            discoverPacks((path, supplier) -> {
                final var pack = createPack(path, supplier);
                if (pack != null) {
                    consumer.accept(pack);
                }
            });
        } catch (final IOException e) {
            log.warn("Failed to list packs in {}", FOLDER, e);
        }
    }

    private static void discoverPacks(BiConsumer<Path, Pack.ResourcesSupplier> output) throws IOException {
        try (final var stream = Files.newDirectoryStream(FOLDER)) {
            for (final var dir : stream) {
                try {
                    final var supplier = detectPackResources(dir);
                    if (supplier != null) {
                        output.accept(dir, supplier);
                    }
                } catch (final IOException e) {
                    log.warn("Failed to read properties of '{}', ignoring", dir, e);
                }
            }
        }
    }

    private static @Nullable Pack.ResourcesSupplier detectPackResources(Path path) throws IOException {
        final var attributes = getAttributes(path);
        if (attributes == null) {
            return null;
        }
        if (attributes.isSymbolicLink()) {
            log.warn("Symbolic links not yet supported, ignoring: {}", path);
            return null;
        }
        if (attributes.isDirectory()) {
            if (Files.isRegularFile(path.resolve("pack.mcmeta")) || hasModConfig(path)) {
                return createDirectoryPack(path);
            }
        } else if (attributes.isRegularFile()) {
            if (path.getFileName().toString().endsWith(".zip")) {
                return createZipPack(path);
            }
        }
        return null;
    }

    private static boolean hasModConfig(Path path) throws IOException {
        try (final var stream = Files.newDirectoryStream(path)) {
            for (final var dir : stream) {
                if (WorldPackConfig.isMatchingFile(dir)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static @Nullable BasicFileAttributes getAttributes(Path path) throws IOException {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (final NoSuchFileException e) {
            return null;
        }
    }

    private static Pack.ResourcesSupplier createDirectoryPack(Path path) {
        return new PathPackResources.PathResourcesSupplier(path);
    }

    private static @Nullable Pack.ResourcesSupplier createZipPack(Path path) {
        final var fs = path.getFileSystem();
        if (fs != FileSystems.getDefault() && !(fs instanceof LinkFileSystem)) {
            log.warn("Can't open pack archive at {}", path);
            return null;
        } else {
            return new FilePackResources.FileResourcesSupplier(path);
        }
    }

    private static @Nullable Pack createPack(Path path, Pack.ResourcesSupplier supplier) {
        final var extension = WorldPackExtension.load(path, supplier);
        if (extension != null) {
            final var worldPackSupplier = WorldPack.createSupplier(supplier, extension);
            final var location = extension.config().toPackLocationInfo(path);
            final var metadata = extension.config().toPackMetadata();
            final var selectionConfig = WorldPackSelectionConfig.get(extension.config().required());
            return new Pack(location, worldPackSupplier, metadata, selectionConfig);
        }
        final var location = createRegularDataPackInfo(path);
        return Pack.readMetaAndCreate(location, supplier, PackType.SERVER_DATA, WorldPackSelectionConfig.OPTIONAL);
    }

    private static PackLocationInfo createRegularDataPackInfo(Path path) {
        final var filename = path.getFileName().toString();
        return new PackLocationInfo("file/" + filename, Component.literal(filename), PackSource.BUILT_IN, Optional.empty());
    }
}
