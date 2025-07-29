package personthecat.pangaea.world.provider;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import personthecat.pangaea.world.level.PangaeaContext;

public record SeaLevelVerticalAnchor(int offset) implements VerticalAnchor {
    public static final Codec<SeaLevelVerticalAnchor> CODEC =
        Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("sea_level")
            .xmap(SeaLevelVerticalAnchor::new, SeaLevelVerticalAnchor::offset)
            .codec();

    @Override
    public int resolveY(WorldGenerationContext gen) {
        return PangaeaContext.get(gen).seaLevel + this.offset;
    }

    @Override
    public String toString() {
        if (this.offset == 0) return "sea level";
        if (this.offset > 0) return this.offset + " above sea level";
        return -this.offset + " below sea level";
    }
}
