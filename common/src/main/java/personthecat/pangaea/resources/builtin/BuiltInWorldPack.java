package personthecat.pangaea.resources.builtin;

import com.mojang.serialization.JavaOps;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.resources.EmptyPackResources;
import personthecat.pangaea.resources.ResourceMap;
import personthecat.pangaea.resources.worldpack.WorldPack;
import personthecat.pangaea.resources.worldpack.WorldPackConfig;
import personthecat.pangaea.resources.worldpack.WorldPackExtension;
import personthecat.pangaea.resources.worldpack.WorldPackSelectionConfig;

import java.nio.file.Path;
import java.util.Map;

public final class BuiltInWorldPack {

    private BuiltInWorldPack() {}

    public static Pack buildFromConfig() {
        final var extension = buildExtension();
        final var supplier = WorldPack.createSupplier(EmptyPackResources.SUPPLIER, extension);
        final var location = extension.config().toPackLocationInfo(Path.of("builtin"));
        final var metadata = extension.config().toPackMetadata();
        return new Pack(location, supplier, metadata, WorldPackSelectionConfig.get(false));
    }

    static <T> ResourceKey<T> key(ResourceKey<? extends Registry<T>> registry, String id) {
        return ResourceKey.create(registry, new ResourceLocation(Pangaea.ID, id));
    }

    public static WorldPackExtension buildExtension() {
        return new WorldPackExtension(buildConfig(), buildResources());
    }

    private static ResourceMap buildResources() {
        final var resources = new ResourceMap();
        if (Cfg.enableRoads()) {
            BuiltInRoads.bootstrap(resources);
        }
        if (Cfg.enableChasms()) {
            BuiltInChasms.bootstrap(resources);
        }
        if (Cfg.modifyTerrainShape()) {
            BuiltInTerrain.bootstrap(resources);
        }
        BuiltInGeneral.boostrap(resources);
        return resources;
    }

    private static WorldPackConfig buildConfig() {
        final var config = Map.of(
            "name", "Pangaea Built-In",
            "description", "Generated World Pack",
            "author", "PersonTheCat"
        );
        return WorldPackConfig.CODEC.codec().parse(JavaOps.INSTANCE, config).getOrThrow();
    }
}