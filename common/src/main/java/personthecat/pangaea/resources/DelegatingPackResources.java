package personthecat.pangaea.resources;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

public class DelegatingPackResources implements PackResources {
    protected final PackResources delegate;

    protected DelegatingPackResources(PackResources delegate) {
        this.delegate = delegate;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... path) {
        return this.delegate.getRootResource(path);
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        return this.delegate.getResource(type, id);
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        this.delegate.listResources(type, namespace, prefix, output);
    }

    @Override
    public @NotNull Set<String> getNamespaces(PackType type) {
        return this.delegate.getNamespaces(type);
    }

    @Override
    public <T> @Nullable T getMetadataSection(MetadataSectionSerializer<T> mss) throws IOException {
        return this.delegate.getMetadataSection(mss);
    }

    @Override
    public @NotNull PackLocationInfo location() {
        return this.delegate.location();
    }

    @Override
    public @NotNull String packId() {
        return this.delegate.packId();
    }

    @Override
    public @NotNull Optional<KnownPack> knownPackInfo() {
        return this.delegate.knownPackInfo();
    }

    @Override
    public void close() {
        this.delegate.close();
    }
}
