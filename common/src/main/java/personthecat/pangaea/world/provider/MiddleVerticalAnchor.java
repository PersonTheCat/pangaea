package personthecat.pangaea.world.provider;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public record MiddleVerticalAnchor(int offset) implements VerticalAnchor {
    public static final Codec<MiddleVerticalAnchor> CODEC =
        Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("middle")
            .xmap(MiddleVerticalAnchor::new, MiddleVerticalAnchor::offset)
            .codec();

    @Override
    public int resolveY(WorldGenerationContext ctx) {
        return ctx.getMinGenY() + (ctx.getGenDepth() / 2) + this.offset;
    }

    @Override
    public String toString() {
        if (this.offset == 0) return "middle";
        if (this.offset > 0) return this.offset + " above middle";
        return -this.offset + " below middle";
    }
}
