package personthecat.pangaea.serialization.preset;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformFloat;
import personthecat.catlib.serialization.codec.capture.Captor;
import personthecat.catlib.serialization.codec.capture.CapturingCodec;
import personthecat.pangaea.world.chain.CanyonLink;
import personthecat.pangaea.world.chain.CanyonPath;
import personthecat.pangaea.world.chain.ChainLinkConfig;
import personthecat.pangaea.world.chain.ChainPathConfig;
import personthecat.pangaea.world.chain.SphereLink;
import personthecat.pangaea.world.chain.TunnelPath;
import personthecat.pangaea.world.filter.ChanceChunkFilter;

import java.util.List;

import static personthecat.catlib.serialization.codec.capture.CapturingCodec.suggestType;
import static personthecat.catlib.serialization.codec.capture.CapturingCodec.supply;

public class ChainFeaturePresets {
    public static final List<Captor<?>> RAVINE = List.of(
        supply("chunk_filter", ChanceChunkFilter.of(0.01)),
        supply("system_chance", ConstantFloat.of(0)),
        supply("count", ConstantInt.of(1)),
        supply("enable_branches", false),
        suggestType("path", ChainPathConfig.class, CanyonPath.Config.CODEC),
        suggestType("link", ChainLinkConfig.class, CanyonLink.Config.CODEC)
    );

    private static final List<Captor<?>> TUNNEL_HUB = List.of(
        supply("radius", UniformFloat.of(2.5F, 5.0F)),
        supply("vertical_scale", ConstantFloat.of(0.5F))
    );

    private static final MapCodec<SphereLink.Config> TUNNEL_HUB_CODEC =
        CapturingCodec.builder().capturing(TUNNEL_HUB).build(SphereLink.Config.CODEC);

    public static final List<Captor<?>> TUNNEL = List.of(
        suggestType("path", ChainPathConfig.class, TunnelPath.Config.CODEC),
        suggestType("link", ChainLinkConfig.class, SphereLink.Config.CODEC),
        suggestType("hub", ChainLinkConfig.class, TUNNEL_HUB_CODEC)
    );
}
