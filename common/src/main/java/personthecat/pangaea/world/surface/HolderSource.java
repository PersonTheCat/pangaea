package personthecat.pangaea.world.surface;

import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.minecraft.world.level.levelgen.SurfaceRules.SurfaceRule;
import org.jetbrains.annotations.NotNull;

public record HolderSource(Holder<RuleSource> holder) implements RuleSource {
    @Override
    public SurfaceRule apply(Context ctx) {
        return this.holder.value().apply(ctx);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends RuleSource> codec() {
        throw new UnsupportedOperationException("Calling .codec() on a HolderSource");
    }
}
