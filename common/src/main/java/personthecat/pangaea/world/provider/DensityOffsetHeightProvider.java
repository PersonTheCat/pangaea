package personthecat.pangaea.world.provider;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import org.jetbrains.annotations.NotNull;
import personthecat.pangaea.world.density.AutoWrapDensity;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;

public class DensityOffsetHeightProvider extends ScalableHeightProvider {
    public static final MapCodec<DensityOffsetHeightProvider> CODEC = codecOf(
        field(HeightProvider.CODEC, "reference", p -> p.reference),
        field(AutoWrapDensity.HELPER_CODEC, "offset", p -> p.offset),
        DensityOffsetHeightProvider::new
    );
    public static final HeightProviderType<DensityOffsetHeightProvider> TYPE = () -> CODEC;

    private final HeightProvider reference;
    private final DensityFunction offset;

    public DensityOffsetHeightProvider(HeightProvider reference, DensityFunction offset) {
        this.reference = reference;
        this.offset = offset;
    }

    @Override
    protected int sample(RandomSource rand, WorldGenerationContext gen, FunctionContext fn) {
        return this.reference.sample(rand, gen) + (int) this.offset.compute(fn);
    }

    @Override
    public @NotNull HeightProviderType<DensityOffsetHeightProvider> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return this.reference + " + " + this.offset;
    }
}
