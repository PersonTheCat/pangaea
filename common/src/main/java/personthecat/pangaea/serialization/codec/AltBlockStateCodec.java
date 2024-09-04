package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.serialization.codec.EasyStateCodec;
import personthecat.pangaea.config.Cfg;

import static personthecat.catlib.serialization.codec.CodecUtils.simpleEither;

public final class AltBlockStateCodec {
    private static final Codec<BlockState> PROPERTY_LIST_CODEC = new EasyStateCodec();
    private static final Codec<BlockState> DEFAULT_STATE_CODEC = BuiltInRegistries.BLOCK.byNameCodec()
        .xmap(Block::defaultBlockState, BlockBehaviour.BlockStateBase::getBlock);

    private AltBlockStateCodec() {}

    public static Codec<BlockState> wrap(Codec<BlockState> codec) {
        return simpleEither(PROPERTY_LIST_CODEC, codec)
            .withEncoder(s -> {
                if (Cfg.encodeDefaultStateAsBlock() && s == s.getBlock().defaultBlockState()) {
                    return DEFAULT_STATE_CODEC;
                } else if (Cfg.encodeStatePropertiesAsList()) {
                    return PROPERTY_LIST_CODEC;
                }
                return codec;
            });
    }
}
