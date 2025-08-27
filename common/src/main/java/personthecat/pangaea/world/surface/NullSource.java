package personthecat.pangaea.world.surface;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.minecraft.world.level.levelgen.SurfaceRules.SurfaceRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NullSource implements RuleSource {
    public static final NullSource INSTANCE = new NullSource();
    public static final MapCodec<NullSource> CODEC = MapCodec.unit(INSTANCE);

    private NullSource() {}

    @Override
    public @NotNull KeyDispatchDataCodec<NullSource> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    @Override
    public SurfaceRule apply(Context ctx) {
        return NullRule.INSTANCE;
    }

    public static class NullRule implements SurfaceRule {
        public static final NullRule INSTANCE = new NullRule();

        @Override
        public @Nullable BlockState tryApply(int x, int y, int z) {
            return null;
        }
    }
}
