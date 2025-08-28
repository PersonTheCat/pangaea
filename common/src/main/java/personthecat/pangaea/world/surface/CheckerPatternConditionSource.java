package personthecat.pangaea.world.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules.Condition;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import org.jetbrains.annotations.NotNull;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record CheckerPatternConditionSource(int width, boolean vertical) implements ConditionSource {
    public static final MapCodec<CheckerPatternConditionSource> CODEC = codecOf(
        field(Codec.intRange(1, Integer.MAX_VALUE) ,"width", CheckerPatternConditionSource::width),
        defaulted(Codec.BOOL, "vertical", false, CheckerPatternConditionSource::vertical),
        CheckerPatternConditionSource::new
    );

    @Override
    public Condition apply(Context ctx) {
        return new CheckerPatternCondition(ctx, this.width, this.vertical);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<CheckerPatternConditionSource> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    record CheckerPatternCondition(Context ctx, int width, boolean vertical) implements Condition {
        @Override
        public boolean test() {
            final int even = sampleChecker(this.ctx.blockX, this.width) % 2;
            return sampleChecker(this.ctx.blockZ, this.width) % 2 == even && (!this.vertical || sampleChecker(this.ctx.blockY, this.width) % 2 == even);
        }

        private static int sampleChecker(int c, int w) {
            return (c < 0 ? ((-c + w - 1) / w) : (c / w)); // offset negative coordinates by space
        }
    }
}
