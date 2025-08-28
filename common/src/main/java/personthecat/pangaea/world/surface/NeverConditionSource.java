package personthecat.pangaea.world.surface;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules.Condition;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import org.jetbrains.annotations.NotNull;

public final class NeverConditionSource implements ConditionSource {
    public static final NeverConditionSource INSTANCE = new NeverConditionSource();
    public static final MapCodec<NeverConditionSource> CODEC = MapCodec.unit(INSTANCE);

    private NeverConditionSource() {}

    @Override
    public Condition apply(Context ctx) {
        return NeverCondition.INSTANCE;
    }

    @Override
    public @NotNull KeyDispatchDataCodec<NeverConditionSource> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    public static final class NeverCondition implements Condition {
        public static final NeverCondition INSTANCE = new NeverCondition();

        private NeverCondition() {}

        @Override
        public boolean test() {
            return false;
        }
    }
}
