package personthecat.pangaea.world.provider;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import personthecat.pangaea.world.level.ScopeExtension;

import java.util.function.Function;

public record SeaLevelVerticalAnchor(int offset) implements VerticalAnchor {
    public static final Codec<SeaLevelVerticalAnchor> CODEC =
        Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("sea_level")
            .xmap(SeaLevelVerticalAnchor::new, SeaLevelVerticalAnchor::offset)
            .codec();

    public static Codec<VerticalAnchor> wrapCodec(Codec<VerticalAnchor> codec) {
        return Codec.xor(codec, CODEC).xmap(
            e -> e.map(Function.identity(), Function.identity()),
            a -> a instanceof SeaLevelVerticalAnchor s ? Either.right(s) : Either.left(a)
        );
    }

    @Override
    public int resolveY(WorldGenerationContext gen) {
        return ScopeExtension.lookupPgContext(gen).seaLevel + this.offset;
    }

    @Override
    public String toString() {
        if (this.offset == 0) return "sea level";
        return this.offset + (this.offset > 0 ? " above sea level" : "below sea level");
    }
}
