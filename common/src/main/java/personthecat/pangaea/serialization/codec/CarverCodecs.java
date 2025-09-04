package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import personthecat.catlib.registry.CommonRegistries;
import personthecat.catlib.serialization.codec.CodecUtils;
import personthecat.pangaea.extras.CarverExtras;

public final class CarverCodecs {
    // Schema: { type: typeof WorldCarver<C> } & C
    public static final MapCodec<ConfiguredWorldCarver<?>> FLAT_CONFIG =
        CommonRegistries.CARVER.codec()
            .dispatchMap(ConfiguredWorldCarver::worldCarver, CarverCodecs::getFlatCodec);

    private CarverCodecs() {}

    private static <C extends CarverConfiguration> MapCodec<ConfiguredWorldCarver<C>> getFlatCodec(
            WorldCarver<C> c) {
        // Whereas Mojang assumes a Codec<C> and reads at field fo "config",
        // we assume and thus only support MapCodecs at the parent level.
        return CodecUtils.asMapCodec(CarverExtras.getCodec(c))
            .xmap(cc -> new ConfiguredWorldCarver<>(c, cc), ConfiguredWorldCarver::config);
    }
}
