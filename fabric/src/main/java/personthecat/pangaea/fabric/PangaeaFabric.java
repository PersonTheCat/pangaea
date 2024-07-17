package personthecat.pangaea.fabric;

import net.fabricmc.api.ModInitializer;
import personthecat.pangaea.Pangaea;

public class PangaeaFabric extends Pangaea implements ModInitializer {

    @Override
    public void onInitialize() {
        this.init();
        this.commonSetup();
    }
}
