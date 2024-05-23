package personthecat.pangaea;

import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.event.lifecycle.GameReadyEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.pangaea.config.Cfg;

public abstract class Pangaea {

    protected final void init() {
        Cfg.register();
    }

    protected final void commonSetup() {
        GameReadyEvent.COMMON.register(() -> {
            System.out.println("The game is ready!");
        });
        CommonWorldEvent.LOAD.register(a -> {
            System.out.println("The world has loaded!");
        });
        FeatureModificationEvent.forBiome(new ResourceLocation("forest")).register(ctx -> {
            System.out.println("Loading forest biome!");
        });
    }
}
