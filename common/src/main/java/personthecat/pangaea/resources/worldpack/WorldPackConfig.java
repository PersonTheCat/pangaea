package personthecat.pangaea.resources.worldpack;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.util.PathUtils;
import personthecat.catlib.versioning.Version;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.mapOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.nullable;

public record WorldPackConfig(
        Component name,
        @Nullable String id,
        String modId,
        Component author,
        Version version,
        Component description,
        @Nullable Component details,
        int packFormat,
        boolean required,
        boolean convertToJson,
        Map<String, JsonElement> metadata) {

    public static final String FILENAME = "mod";
    private static final int CURRENT_PACK_FORMAT =
        SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA);

    public static final MapCodec<WorldPackConfig> CODEC = codecOf(
        field(ComponentSerialization.CODEC, "name", WorldPackConfig::name),
        nullable(Codec.STRING, "id", WorldPackConfig::id),
        defaulted(Codec.STRING, "mod_id", "pangaea", WorldPackConfig::modId),
        field(ComponentSerialization.CODEC, "author", WorldPackConfig::author),
        defaulted(Version.CODEC, "version", Version.create(0, 0, 1), WorldPackConfig::version),
        field(ComponentSerialization.CODEC, "description", WorldPackConfig::description),
        nullable(ComponentSerialization.CODEC, "details", WorldPackConfig::details),
        defaulted(Codec.INT, "pack_format", CURRENT_PACK_FORMAT, WorldPackConfig::packFormat),
        defaulted(Codec.BOOL, "required", true, WorldPackConfig::required),
        defaulted(Codec.BOOL, "convert_to_json", true, WorldPackConfig::convertToJson),
        defaulted(mapOf(ExtraCodecs.JSON), "metadata", Map.of(), WorldPackConfig::metadata),
        WorldPackConfig::new
    );

    public static boolean isMatchingFile(Path file) {
        final var filename = file.getFileName().toString();
        return Files.isRegularFile(file)
            && PathUtils.noExtension(filename).equals(FILENAME)
            && WorldPackExtension.MOD_CONFIG_FORMATS.contains(PathUtils.extension(filename));
    }

    public String getId(Path path) {
        return "pangaea/" + (this.id != null ? this.id : PathUtils.noExtension(path.getFileName().toString()));
    }

    public PackMetadataSection toPackMcmeta() {
        return new PackMetadataSection(this.description, this.packFormat, Optional.empty());
    }

    public KnownPack toKnownPack(Path path) {
        return new KnownPack(this.modId(), this.getId(path), this.version.toString());
    }

    public PackLocationInfo toPackLocationInfo(Path path) {
        return new PackLocationInfo(this.getId(path), this.name, WorldPackSource.get(this.required), Optional.of(this.toKnownPack(path)));
    }

    public Pack.Metadata toPackMetadata() {
        return new Pack.Metadata(this.description, PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), this.getOverlays());
    }

    public List<String> getOverlays() {
        final var overlays = this.getMetadata(OverlayMetadataSection.TYPE);
        return overlays != null ? overlays.overlaysForVersion(this.packFormat) : List.of();
    }

    public <T> @Nullable T getMetadata(MetadataSectionSerializer<T> mss) {
        final var json = this.metadata.get(mss.getMetadataSectionName());
        if (json != null && json.isJsonObject()) {
            return mss.fromJson(json.getAsJsonObject());
        }
        return null;
    }
}
