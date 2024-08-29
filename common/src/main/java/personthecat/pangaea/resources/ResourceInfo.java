package personthecat.pangaea.resources;

import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.util.PathUtils;

public record ResourceInfo(ResourceLocation id) {

    public String extension() {
        return PathUtils.extension(this.id.getPath());
    }

    public String namespace() {
        return this.id.getNamespace();
    }

    public boolean isInDirectory(String dir) {
        return this.id.getPath().startsWith(dir);
    }
}
