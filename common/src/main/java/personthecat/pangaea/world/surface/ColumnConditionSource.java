package personthecat.pangaea.world.surface;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import net.minecraft.world.level.levelgen.SurfaceRules.Condition;
import org.jetbrains.annotations.NotNull;
import personthecat.pangaea.extras.ContextExtras;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.provider.ColumnProvider;

public record ColumnConditionSource(ColumnProvider column) implements ConditionSource {
    public static final MapCodec<ColumnConditionSource> CODEC =
        ColumnProvider.CODEC.fieldOf("column").xmap(ColumnConditionSource::new, ColumnConditionSource::column);

    @Override
    public Condition apply(Context ctx) {
        return new ColumnCondition(ctx, ContextExtras.getPangaea(ctx), this.column);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends ConditionSource> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    record ColumnCondition(Context ctx, PangaeaContext pg, ColumnProvider column) implements Condition {
        @Override
        public boolean test() {
            return this.column.isInBounds(this.pg, this.ctx.blockX, this.ctx.blockY, this.ctx.blockZ);
        }
    }
}
