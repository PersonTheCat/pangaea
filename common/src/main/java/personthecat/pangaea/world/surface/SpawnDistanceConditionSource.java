package personthecat.pangaea.world.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Condition;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import net.minecraft.world.level.levelgen.SurfaceRules.LazyXZCondition;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.Range;
import personthecat.pangaea.extras.ContextExtras;
import personthecat.pangaea.util.Utils;
import personthecat.pangaea.world.surface.NeverConditionSource.NeverCondition;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record SpawnDistanceConditionSource(Range distance, double chance, int fade) implements ConditionSource {
    public static final MapCodec<SpawnDistanceConditionSource> CODEC = codecOf(
        field(Range.RANGE_UP_CODEC, "distance", SpawnDistanceConditionSource::distance),
        defaulted(Codec.doubleRange(0, 1), "chance", 1.0, SpawnDistanceConditionSource::chance),
        defaulted(Codec.intRange(0, Integer.MAX_VALUE), "fade", 0, SpawnDistanceConditionSource::fade),
        SpawnDistanceConditionSource::new
    );

    @Override
    public Condition apply(Context ctx) {
        final int minChunk = (this.distance.min() >> 4) - this.fade - 1;
        final int maxChunk = (this.distance.max() >> 4) + this.fade + 1;
        final var pos = ctx.chunk.getPos();
        final int chunkDistance = (int) Math.sqrt(pos.x * pos.x + pos.z * pos.z);
        if (chunkDistance >= minChunk && chunkDistance < maxChunk) {
            return new SpawnDistanceCondition(ctx, this.distance, this.chance, this.fade);
        }
        return NeverCondition.INSTANCE;
    }

    @Override
    public @NotNull KeyDispatchDataCodec<SpawnDistanceConditionSource> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }

    static class SpawnDistanceCondition extends LazyXZCondition {
        private final RandomSource rand;
        private final Range distance;
        private final double chance;
        private final int fade;

        protected SpawnDistanceCondition(Context ctx, Range distance, double chance, int fade) {
            super(ctx);
            this.rand = ContextExtras.getRandomSource(ctx);
            this.distance = distance;
            this.chance = chance;
            this.fade = fade;
        }

        @Override
        public boolean compute() {
            final int d = (int) Math.sqrt((this.context.blockX * this.context.blockX) + (this.context.blockZ * this.context.blockZ));
            return Utils.checkDistanceWithFade(this.rand, this.distance, d, this.chance, this.fade);
        }
    }
}
