package personthecat.pangaea.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.serialization.codec.FunctionCodec.Template;
import personthecat.pangaea.world.biome.BiomeLayout;
import personthecat.pangaea.world.biome.BiomeSlice;
import personthecat.pangaea.world.injector.Injector;
import personthecat.pangaea.world.placer.BlockPlacer;

public final class PgRegistries {
    public static final RegistryHandle<MapCodec<? extends Injector>> INJECTOR_TYPE = type(Keys.INJECTOR_TYPE);
    public static final RegistryHandle<MapCodec<? extends BlockPlacer>> PLACER_TYPE = type(Keys.PLACER_TYPE);

    public static final RegistryHandle<Injector> INJECTOR = dynamic(Keys.INJECTOR, Injector.CODEC);
    public static final RegistryHandle<BiomeLayout> BIOME_LAYOUT = dynamic(Keys.BIOME_LAYOUT, BiomeLayout.CODEC.codec());
    public static final RegistryHandle<BiomeSlice> BIOME_SLICE = dynamic(Keys.BIOME_SLICE, BiomeSlice.CODEC.codec());
    public static final RegistryHandle<BlockPlacer> PLACER = dynamic(Keys.PLACER, BlockPlacer.CODEC);

    private PgRegistries() {}

    private static <T> RegistryHandle<MapCodec<? extends T>> type(ResourceKey<Registry<MapCodec<? extends T>>> key) {
        return RegistryHandle.createAndRegister(Pangaea.MOD, key);
    }

    private static <T> RegistryHandle<T> dynamic(ResourceKey<Registry<T>> key, Codec<T> codec) {
        return RegistryHandle.createDynamic(Pangaea.MOD, key, codec);
    }

    public static final class Keys {
        public static final ResourceKey<Registry<MapCodec<? extends Injector>>> INJECTOR_TYPE = key("injector_type");
        public static final ResourceKey<Registry<MapCodec<? extends BlockPlacer>>> PLACER_TYPE = key("placer_type");

        public static final ResourceKey<Registry<Injector>> INJECTOR = key("injector");
        public static final ResourceKey<Registry<BiomeLayout>> BIOME_LAYOUT = key("layout");
        public static final ResourceKey<Registry<BiomeSlice>> BIOME_SLICE = key("slice");
        public static final ResourceKey<Registry<BlockPlacer>> PLACER = key("placer");
        public static final ResourceKey<Registry<Template<DensityFunction>>> DENSITY_TEMPLATE = key("function");

        private Keys() {}

        private static <T> ResourceKey<Registry<T>> key(String path) {
            return ResourceKey.createRegistryKey(Pangaea.MOD.id(path));
        }
    }
}
