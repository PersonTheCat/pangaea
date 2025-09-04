package personthecat.pangaea.resources;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.util.PathUtils;

public record ResourceInfo(ResourceKey<?> key) {

    public ResourceLocation registry() {
        return this.key.registry();
    }

    public ResourceLocation id() {
        return this.key.location();
    }

    public String extension() {
        return PathUtils.extension(this.id().getPath());
    }

    public String namespace() {
        return this.id().getNamespace();
    }

    public boolean isInDirectory(ResourceLocation registry, String dir) {
        return this.registry().equals(registry) && this.id().getPath().startsWith(dir);
    }
}
