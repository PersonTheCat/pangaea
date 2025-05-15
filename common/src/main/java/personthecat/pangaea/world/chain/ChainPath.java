package personthecat.pangaea.world.chain;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import personthecat.pangaea.world.feature.Border;
import personthecat.pangaea.world.level.PangaeaContext;

public abstract class ChainPath {
    protected final ChainPathConfig<? extends ChainPath> config;
    protected float x;
    protected float y;
    protected float z;
    protected float rf;
    protected float yaw;
    protected float pitch;

    protected ChainPath(ChainPathConfig<? extends ChainPath> config) {
        this.config = config;
    }

    public void start(RandomSource rand) {
        this.yaw = rand.nextFloat() * Mth.TWO_PI;
    }

    public abstract void next(PangaeaContext ctx, RandomSource rand, int idx, int end);

    public void reset(PangaeaContext ctx, RandomSource rand, float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public ChainPath fork(PangaeaContext ctx, RandomSource rand) {
        final var fork = this.config.instance(ctx, rand);
        fork.reset(ctx, rand, this.x, this.y, this.z);
        return fork;
    }

    public void redirect(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void scale(float radiusFactor) {
        this.rf = radiusFactor;
    }

    public final float blockX() {
        return this.x;
    }

    public final float blockY() {
        return this.y;
    }

    public final float blockZ() {
        return this.z;
    }

    public final float radiusFactor() {
        return this.rf;
    }

    public final float yaw() {
        return this.yaw;
    }

    public final float pitch() {
        return this.pitch;
    }

    public final ChainPathConfig<? extends ChainPath> config() {
        return this.config;
    }
}
