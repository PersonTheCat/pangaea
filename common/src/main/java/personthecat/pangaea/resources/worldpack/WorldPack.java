package personthecat.pangaea.resources.worldpack;

import com.google.common.collect.ImmutableSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.util.PathUtils;
import personthecat.pangaea.resources.DelegatingPackResources;
import personthecat.pangaea.resources.ImposterResource;
import personthecat.pangaea.resources.ResourceMap;
import personthecat.pangaea.resources.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;

public class WorldPack extends DelegatingPackResources {
    private static final String DEFAULT_PACK_ICON = "assets/pangaea/icon.png";
    private final ResourceMap dynamicResources;
    private final PackLocationInfo location;
    private final WorldPackConfig config;

    private WorldPack(PackResources parent, WorldPackExtension extension, PackLocationInfo location) {
        super(parent);
        this.dynamicResources = extension.dynamicResources();
        this.location = location;
        this.config = extension.config();
    }

    public static Pack.ResourcesSupplier createSupplier(Pack.ResourcesSupplier delegate, WorldPackExtension extension) {
        return new WorldPackResourcesSupplier(delegate, extension);
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... path) {
        IoSupplier<InputStream> resource = this.dynamicResources.get(StringUtils.join(path, "/"));
        if (resource != null) {
            return resource;
        }
        resource = super.getRootResource(path);
        if (resource != null) {
            return resource;
        }
        if (path.length == 1 && path[0].equals("pack.png")) {
            return () -> Objects.requireNonNull(WorldPack.class.getClassLoader().getResourceAsStream(DEFAULT_PACK_ICON));
        }
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        if (type == PackType.SERVER_DATA) {
            final var dynamic = this.dynamicResources.get(id);
            if (dynamic != null) {
                return dynamic;
            }
        }
        return super.getResource(type, id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getMetadataSection(MetadataSectionSerializer<T> mss) throws IOException {
        if (mss == PackMetadataSection.TYPE) {
            return (T) this.config.toPackMcmeta();
        }
        final var meta = this.config.getMetadata(mss);
        return meta != null ? meta : super.getMetadataSection(mss);
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        if (type == PackType.SERVER_DATA) {
            this.dynamicResources.listByNamespaceAndPrefix(namespace, prefix)
                .forEach((path, resource) ->
                    output.accept(new ResourceLocation(namespace, prefix + "/" + path + ".json"), resource));
        }
        super.listResources(type, namespace, prefix, (id, data) -> {
            // dynamically insert non-json formats as imposter json files
            final var ext = PathUtils.extension(id.getPath());
            if (WorldPackExtension.ALT_FORMATS.contains(ext)) {
                final var jsonId = new ResourceLocation(id.getNamespace(), PathUtils.noExtension(id.getPath()) + ".json");
                final boolean convertToJson = this.config.convertToJson();
                output.accept(jsonId, ImposterResource.fromLazyData(() -> ResourceUtils.loadDynamic(id.getPath(), data), convertToJson));
            } else {
                output.accept(id, data);
            }
        });
    }

    @Override
    public @NotNull Set<String> getNamespaces(PackType type) {
        if (type != PackType.SERVER_DATA) {
            return super.getNamespaces(type);
        }
        return ImmutableSet.<String>builder()
            .addAll(super.getNamespaces(type))
            .addAll(this.dynamicResources.listNamespaces())
            .build();
    }

    @Override
    public @NotNull PackLocationInfo location() {
        return this.location;
    }

    public WorldPackConfig getConfig() {
        return this.config;
    }

    private record WorldPackResourcesSupplier(
            Pack.ResourcesSupplier delegate, WorldPackExtension extension) implements Pack.ResourcesSupplier {
        @Override
        public @NotNull PackResources openPrimary(PackLocationInfo location) {
            return new WorldPack(this.delegate.openPrimary(location), this.extension, location);
        }

        @Override
        public @NotNull PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
            return new WorldPack(this.delegate.openFull(location, metadata), this.extension, location);
        }
    }
}
