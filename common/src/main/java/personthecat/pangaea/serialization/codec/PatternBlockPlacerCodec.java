package personthecat.pangaea.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.world.placer.BlockPlacer;
import personthecat.pangaea.world.placer.BlockPlacerList;
import personthecat.pangaea.world.placer.UnconditionalBlockPlacer;

import static personthecat.catlib.serialization.codec.CodecUtils.simpleEither;

public final class PatternBlockPlacerCodec implements Codec<BlockPlacer> {
    public static final Codec<BlockPlacer> INSTANCE = new PatternBlockPlacerCodec();

    private PatternBlockPlacerCodec() {}

    public static Codec<BlockPlacer> wrap(Codec<BlockPlacer> codec) {
        return simpleEither(codec, INSTANCE)
            .withEncoder((placer) ->
                Cfg.encodePatternBlockPlacers() && Pattern.hasMatcherForPlacer(placer)
                    ? INSTANCE : codec);
    }

    @Override
    public <T> DataResult<Pair<BlockPlacer, T>> decode(DynamicOps<T> ops, T input) {
        for (final var matcher : Pattern.MATCHERS) {
            final var result = matcher.codec.decode(ops, input);
            if (result.isSuccess()) {
                return result;
            }
        }
        return DataResult.error(() -> "No matching pattern for input: " + input);
    }

    @Override
    public <T> DataResult<T> encode(BlockPlacer input, DynamicOps<T> ops, T prefix) {
        for (final var matcher : Pattern.MATCHERS) {
            if (matcher.type.isInstance(input)) {
                return matcher.codec.encode(input, ops, prefix);
            }
        }
        return DataResult.error(() -> "No matching pattern for input: " + input);
    }

    private static class Pattern {
        private static final Codec<BlockPlacer> STATE =
            BlockState.CODEC.xmap(UnconditionalBlockPlacer::new, p -> ((UnconditionalBlockPlacer) p).place());
        private static final Codec<BlockPlacer> LIST =
            BlockPlacer.CODEC.listOf().xmap(BlockPlacerList::new, p -> ((BlockPlacerList) p).place());

        private static final Matcher[] MATCHERS = {
            new Matcher(STATE, UnconditionalBlockPlacer.class),
            new Matcher(LIST, BlockPlacerList.class),
        };

        private record Matcher(Codec<BlockPlacer> codec, Class<? extends BlockPlacer> type) {}

        private static boolean hasMatcherForPlacer(BlockPlacer placer) {
            for (final var matcher : MATCHERS) {
                if (matcher.type.isInstance(placer)) {
                    return true;
                }
            }
            return false;
        }
    }
}
