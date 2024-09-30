package personthecat.pangaea.registry;

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

public final class PgRegistries {
    public static final RegistryHandle<MapCodec<? extends Injector>> INJECTOR_TYPE =
        RegistryHandle.createAndRegister(Pangaea.MOD, Keys.INJECTOR_TYPE);
    public static final RegistryHandle<Injector> INJECTOR =
        RegistryHandle.createDynamic(Pangaea.MOD, Keys.INJECTOR, Injector.CODEC);
    public static final RegistryHandle<BiomeLayout> BIOME_LAYOUT =
        RegistryHandle.createDynamic(Pangaea.MOD, Keys.BIOME_LAYOUT, BiomeLayout.CODEC.codec());
    public static final RegistryHandle<BiomeSlice> BIOME_SLICE =
        RegistryHandle.createDynamic(Pangaea.MOD, Keys.BIOME_SLICE, BiomeSlice.CODEC.codec());

    private PgRegistries() {}

    public static final class Keys {
        public static final ResourceKey<Registry<MapCodec<? extends Injector>>> INJECTOR_TYPE =
            ResourceKey.createRegistryKey(Pangaea.MOD.id("injector_type"));
        public static final ResourceKey<Registry<Injector>> INJECTOR =
            ResourceKey.createRegistryKey(Pangaea.MOD.id("injector"));
        public static final ResourceKey<Registry<BiomeLayout>> BIOME_LAYOUT =
            ResourceKey.createRegistryKey(Pangaea.MOD.id("layout"));
        public static final ResourceKey<Registry<BiomeSlice>> BIOME_SLICE =
            ResourceKey.createRegistryKey(Pangaea.MOD.id("slice"));
        public static final ResourceKey<Registry<Template<DensityFunction>>> DENSITY_TEMPLATE =
            ResourceKey.createRegistryKey(Pangaea.MOD.id("function"));

        private Keys() {}
    }
}
