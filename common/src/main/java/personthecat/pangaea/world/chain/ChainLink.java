package personthecat.pangaea.world.chain;

import net.minecraft.util.RandomSource;
import personthecat.pangaea.world.level.PangaeaContext;

public abstract class ChainLink {
    protected final ChainLinkConfig<? extends ChainLink> config;

    protected ChainLink(ChainLinkConfig<? extends ChainLink> config) {
        this.config = config;
    }

    public abstract void place(PangaeaContext ctx, RandomSource rand, ChainPath path, int idx, int end);
    public abstract double radius();

    public final ChainLinkConfig<? extends ChainLink> config() {
        return this.config;
    }
}
