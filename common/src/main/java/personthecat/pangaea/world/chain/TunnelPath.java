package personthecat.pangaea.world.chain;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import personthecat.pangaea.world.level.PangaeaContext;
import personthecat.pangaea.world.provider.BiasedToBottomFloat;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.pangaea.serialization.codec.PgCodecs.floatRangeFix;

public class TunnelPath extends ChainPath {
    private boolean noiseCorrection;
    private float verticality;
    private float horizontality;
    private float yawChange;
    private float pitchChange;
    private float thickness;

    protected TunnelPath(Config cfg) {
        super(cfg);
    }

    boolean randThickness = true; // DEBUG

    @Override
    public void start(RandomSource rand) {
        super.start(rand);
        final var config = (Config) this.config;
        this.pitch = config.pitch.sample(rand);
        this.verticality = config.verticality.sample(rand);
        this.horizontality = config.horizontality.sample(rand);
        this.thickness = config.thickness.sample(rand);
        if (randThickness && rand.nextInt(10) == 0) {
            this.thickness *= rand.nextFloat() * rand.nextFloat() * 1.5F + 1.0F;
        }
    }

    @Override
    public void next(PangaeaContext ctx, RandomSource rand, int idx, int end) {
        this.rf = 1.0F + Mth.sin(Mth.PI * (float) idx / (float) end) * this.thickness;
        final var cosPitch = Mth.cos(this.pitch);
        this.x += Mth.cos(this.yaw) * cosPitch;
        this.y += Mth.sin(this.pitch);
        this.z += Mth.sin(this.yaw) * cosPitch;
        this.pitch *= this.noiseCorrection ? 0.92F : 0.7F;
        this.pitch += this.pitchChange * this.verticality;
        this.yaw += this.yawChange * this.horizontality;
        this.pitchChange *= 0.9F;
        this.yawChange *= 0.75F;
        this.pitchChange += (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 2.0F;
        this.yawChange += (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 4.0F;
    }

    @Override
    public void reset(PangaeaContext ctx, RandomSource rand, float x, float y, float z) {
        super.reset(ctx, rand, x, y, z);
        this.noiseCorrection = rand.nextInt(6) == 0;
        this.yawChange = 0;
        this.pitchChange = 0;
    }

    @Override
    public ChainPath fork(PangaeaContext ctx, RandomSource rand) {
        final var fork = (TunnelPath) super.fork(ctx, rand);
        fork.verticality = this.verticality;
        fork.horizontality = this.horizontality;
        fork.thickness = rand.nextFloat() * 0.5F + 0.5F;
        return fork;
    }

    @Override
    public void scale(float radiusFactor) {
        this.rf = radiusFactor * this.thickness;
    }

    public record Config(
        FloatProvider verticality,
        FloatProvider horizontality,
        FloatProvider thickness,
        FloatProvider pitch
    ) implements ChainPathConfig<TunnelPath> {
        private static final FloatProvider DEFAULT_TAMENESS = ConstantFloat.of(0.1F);
        private static final FloatProvider DEFAULT_THICKNESS = BiasedToBottomFloat.of(0, 2);
        private static final FloatProvider DEFAULT_PITCH = UniformFloat.of(-0.125F, 0.125F);
        public static final MapCodec<Config> CODEC = codecOf(
            defaulted(FloatProvider.CODEC, "verticality", DEFAULT_TAMENESS, Config::verticality),
            defaulted(FloatProvider.CODEC, "horizontality", DEFAULT_TAMENESS, Config::horizontality),
            defaulted(FloatProvider.CODEC, "thickness", DEFAULT_THICKNESS, Config::thickness),
            defaulted(floatRangeFix(-Mth.TWO_PI, Mth.TWO_PI), "pitch", DEFAULT_PITCH, Config::pitch),
            Config::new
        );

        @Override
        public TunnelPath instance(PangaeaContext ctx, RandomSource rand) {
            return new TunnelPath(this);
        }

        @Override
        public MapCodec<Config> codec() {
            return CODEC;
        }
    }
}
