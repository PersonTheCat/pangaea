package personthecat.pangaea.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockStateMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.mixin.BlockMatchTestAccessor;
import personthecat.pangaea.mixin.BlockStateMatchTestAccessor;
import personthecat.pangaea.mixin.TagMatchTestAccessor;

public class PatternRuleTestCodec implements Codec<RuleTest> {
    private final Codec<RuleTest> wrapped;

    private PatternRuleTestCodec(Codec<RuleTest> wrapped) {
        this.wrapped = wrapped;
    }

    public static Codec<RuleTest> wrap(Codec<RuleTest> codec) {
        return new PatternRuleTestCodec(codec);
    }

    @Override
    public <T> DataResult<Pair<RuleTest, T>> decode(DynamicOps<T> ops, T input) {
        for (final Pattern.Matcher m : Pattern.MATCHERS) {
            final var result = m.codec.decode(ops, input);
            if (result.isSuccess()) {
                return result;
            }
        }
        return this.wrapped.decode(ops, input);
    }

    @Override
    public <T> DataResult<T> encode(RuleTest input, DynamicOps<T> ops, T prefix) {
        if (Cfg.encodePatternRuleTestCodec()) {
            for (final Pattern.Matcher m : Pattern.MATCHERS) {
                if (m.type.isInstance(input)) {
                    return m.codec.encode(input, ops, prefix);
                }
            }
        }
        return this.wrapped.encode(input, ops, prefix);
    }

    private static class Pattern {
        private static final Codec<RuleTest> TAG =
            TagKey.hashedCodec(Registries.BLOCK).xmap(TagMatchTest::new, test -> ((TagMatchTestAccessor) test).getTag());
        private static final Codec<RuleTest> BLOCK =
            BuiltInRegistries.BLOCK.byNameCodec().xmap(BlockMatchTest::new, test -> ((BlockMatchTestAccessor) test).getBlock());
        private static final Codec<RuleTest> STATE =
            BlockState.CODEC.xmap(BlockStateMatchTest::new, test -> ((BlockStateMatchTestAccessor) test).getBlockState());

        private static final Matcher[] MATCHERS = {
            new Matcher(TAG, TagMatchTest.class),
            new Matcher(BLOCK, BlockMatchTest.class),
            new Matcher(STATE, BlockStateMatchTest.class)
        };

        private record Matcher(Codec<RuleTest> codec, Class<? extends RuleTest> type) {}
    }
}
