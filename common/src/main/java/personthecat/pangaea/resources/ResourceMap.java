package personthecat.pangaea.resources;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JavaOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ResourceMap {
    private final Map<ResourceKey<?>, ImposterResource<?>> byKey = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Map<String, ImposterResource<?>>>> byNsPrefixAndPath = new ConcurrentHashMap<>();
    private final Map<String, Map<String, ImposterResource<?>>> byNsAndFullPath = new ConcurrentHashMap<>();
    private final Map<String, ImposterResource<?>> byAbsolutePath = new ConcurrentHashMap<>();

    public <T> void addLazyValue(ResourceKey<T> key, Supplier<T> value) {
        this.addDynamicResource(key, ImposterResource.fromLazyValue(key, value));
    }

    public <T> void addLazyData(ResourceKey<T> key, Supplier<Dynamic<?>> data) {
        this.addDynamicResource(key, ImposterResource.fromLazyData(data, false));
    }

    public <T> void addLiteralResource(ResourceKey<T> key, T value) {
        this.addDynamicResource(key, ImposterResource.fromLiteralValue(key, value));
    }

    public <T> void addDynamicResource(ResourceKey<T> key, Object java) {
        this.addDynamicResource(key, new Dynamic<>(JavaOps.INSTANCE, java));
    }

    public <T> void addDynamicResource(ResourceKey<T> key, Dynamic<?> data) {
        this.addDynamicResource(key, ImposterResource.fromGeneratedData(data));
    }

    public <T> void addDynamicResource(ResourceKey<T> key, ImposterResource<T> resource) {
        this.add(key, resource);
    }

    public <T> void add(ResourceKey<T> key, ImposterResource<T> resource) {
        final var prefix = getDirectory(key.registry());
        final var ns = key.location().getNamespace();
        final var path = key.location().getPath();

        this.byKey.put(key, resource);
        this.getMapForNamespaceAndPrefix(ns, prefix).put(path, resource);
        this.getMapForNamespace(ns).put(prefix + "/" + path, resource);
        this.byAbsolutePath.put("data/" + ns + "/" + prefix + "/" + path, resource);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable ImposterResource<T> get(ResourceKey<T> key) {
        return (ImposterResource<T>) this.byKey.get(key);
    }

    public @Nullable ImposterResource<?> get(ResourceLocation id) {
        return this.get(id.getNamespace(), id.getPath());
    }

    public @Nullable ImposterResource<?> get(String namespace, String fullPath) {
        return this.getMapForNamespace(namespace).get(fullPath);
    }

    public @Nullable ImposterResource<?> get(String prefix, ResourceLocation id) {
        return this.get(id.getNamespace(), prefix, id.getPath());
    }

    public @Nullable ImposterResource<?> get(String namespace, String prefix, String path) {
        return this.getMapForNamespaceAndPrefix(namespace, prefix).get(path);
    }

    public @Nullable ImposterResource<?> get(String absolutePath) {
        return this.byAbsolutePath.get(absolutePath);
    }

    public Set<String> listNamespaces() {
        return Collections.unmodifiableSet(this.byNsPrefixAndPath.keySet());
    }

    public Map<String, ImposterResource<?>> listByNamespaceAndPrefix(String namespace, String prefix) {
        return Collections.unmodifiableMap(this.getMapForNamespaceAndPrefix(namespace, prefix));
    }

    public Map<String, ImposterResource<?>> listByAbsolutePath() {
        return Collections.unmodifiableMap(this.byAbsolutePath);
    }

    public void clear() {
        this.byKey.clear();
        this.byNsPrefixAndPath.clear();
        this.byNsAndFullPath.clear();
        this.byAbsolutePath.clear();
    }

    private Map<String, ImposterResource<?>> getMapForNamespaceAndPrefix(String namespace, String prefix) {
        return this.byNsPrefixAndPath
            .computeIfAbsent(namespace, ns -> new ConcurrentHashMap<>())
            .computeIfAbsent(prefix, p -> new ConcurrentHashMap<>());
    }

    private Map<String, ImposterResource<?>> getMapForNamespace(String namespace) {
        return this.byNsAndFullPath.computeIfAbsent(namespace, ns -> new ConcurrentHashMap<>());
    }

    private static String getDirectory(ResourceLocation registry) {
        return registry.getNamespace().equals("minecraft") ? registry.getPath() : registry.getNamespace() + "/" + registry.getPath();
    }
}
