package personthecat.pangaea.fabric;

import net.fabricmc.api.ModInitializer;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.pangaea.config.Cfg;

public class Pangaea implements ModInitializer {

    @Override
    public void onInitialize() {
        Cfg.register();

        CommonWorldEvent.LOAD.register(a -> {
            System.out.println("The world has loaded!");
        });
    }
}
