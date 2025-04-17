package personthecat.pangaea.world.injector;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import personthecat.pangaea.serialization.codec.PgCodecs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.easyList;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleEither;
import static personthecat.catlib.serialization.codec.CodecUtils.xmapWithOps;

public class InjectionMap<E> extends HashMap<ResourceLocation, E> {

    public InjectionMap() {}

    public InjectionMap(Map<ResourceLocation, E> source) {
        super(source);
    }

    public static <E> Codec<InjectionMap<E>> codecOfMap(Codec<E> elementCodec) {
        return Codec.unboundedMap(ResourceLocation.CODEC, elementCodec)
            .xmap(InjectionMap::new, Function.identity());
    }

    public static <E> Codec<InjectionMap<E>> codecOfList(Codec<E> elementCodec) {
        return codecOfListInternal(elementCodec.listOf());
    }

    public static <E> Codec<InjectionMap<E>> codecOfEasyList(Codec<E> elementCodec) {
        return codecOfListInternal(easyList(elementCodec));
    }

    private static <E> Codec<InjectionMap<E>> codecOfListInternal(Codec<List<E>> listCodec) {
        return xmapWithOps(listCodec,
            (ops, list) -> withRandomIds(PgCodecs.getActiveNamespace(ops), list),
            (ops, map) -> List.copyOf(map.values()));
    }

    public static <E> Codec<InjectionMap<E>> codecOfMapOrList(Codec<E> elementCodec) {
        return simpleEither(codecOfMap(elementCodec), codecOfList(elementCodec));
    }

    public static <E> InjectionMap<E> withRandomIds(String namespace, List<E> list) {
        final var map = new InjectionMap<E>();
        for (int i = 0; i < list.size(); i++) {
            final var e = list.get(i);
            final var id = String.valueOf(Objects.hash(e, i));
            map.put(new ResourceLocation(namespace, id), e);
        }
        return map;
    }
}
