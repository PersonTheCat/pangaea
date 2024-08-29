package personthecat.pangaea.world.injector;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.simpleEither;

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
        return elementCodec.listOf().xmap(InjectionMap::withRandomIds, map -> List.copyOf(map.values()));
    }

    public static <E> Codec<InjectionMap<E>> codecOfMapOrList(Codec<E> elementCodec) {
        return simpleEither(codecOfMap(elementCodec), codecOfList(elementCodec));
    }

    public static <E> InjectionMap<E> withRandomIds(List<E> list) {
        return new InjectionMap<>();
    }
}
