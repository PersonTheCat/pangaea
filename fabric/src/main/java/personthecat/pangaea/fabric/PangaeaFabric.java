package personthecat.pangaea.fabric;

import net.fabricmc.api.ModInitializer;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.config.Cfg;

public class PangaeaFabric extends Pangaea implements ModInitializer {

    @Override
    public void onInitialize() {
        this.init();
        this.commonSetup();
    }
}
