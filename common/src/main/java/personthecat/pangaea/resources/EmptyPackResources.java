package personthecat.pangaea.resources;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

public final class EmptyPackResources implements PackResources {
    public static final EmptyPackResources INSTANCE = new EmptyPackResources();
    public static final Pack.ResourcesSupplier SUPPLIER = EmptySupplier.INSTANCE;
    private static final PackLocationInfo EMPTY_INFO =
        new PackLocationInfo("", Component.empty(), PackSource.BUILT_IN, Optional.empty());

    private EmptyPackResources() {}

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... strings) {
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType packType, ResourceLocation resourceLocation) {
        return null;
    }

    @Override
    public void listResources(PackType packType, String string, String string2, ResourceOutput resourceOutput) {}

    @Override
    public @NotNull Set<String> getNamespaces(PackType packType) {
        return Set.of();
    }

    @Override
    public <T> @Nullable T getMetadataSection(MetadataSectionSerializer<T> metadataSectionSerializer) throws IOException {
        return null;
    }

    @Override
    public @NotNull PackLocationInfo location() {
        return EMPTY_INFO;
    }

    @Override
    public void close() {}

    private static class EmptySupplier implements Pack.ResourcesSupplier {
        private static final EmptySupplier INSTANCE = new EmptySupplier();

        @Override
        public @NotNull PackResources openPrimary(PackLocationInfo location) {
            return EmptyPackResources.INSTANCE;
        }

        @Override
        public @NotNull PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
            return EmptyPackResources.INSTANCE;
        }
    }
}
