package personthecat.pangaea.data;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import personthecat.pangaea.world.density.DensityCutoff;
import personthecat.pangaea.world.level.GenerationContext;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public record AnchorCutoff(VerticalAnchor min, VerticalAnchor max, double harshness) {
    public static final Codec<AnchorCutoff> CODEC = codecOf(
        field(VerticalAnchor.CODEC, "min", AnchorCutoff::min),
        field(VerticalAnchor.CODEC, "max", AnchorCutoff::max),
        defaulted(Codec.DOUBLE, "harshness", DensityCutoff.DEFAULT_HARSHNESS, AnchorCutoff::harshness),
        AnchorCutoff::new
    ).codec();

    public DensityCutoff getCutoff(GenerationContext ctx) {
        return new DensityCutoff(this.min.resolveY(ctx), this.max.resolveY(ctx), this.harshness);
    }
}
